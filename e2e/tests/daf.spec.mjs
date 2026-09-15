import { test, expect } from "@playwright/test";

const APP = process.env.DAF_E2E_BASE ?? "http://localhost:4408";
const API = "http://localhost:19080/daf";

/** DAF's role token: an unsigned JWT with `sub` and `roles`, enough for the UI to decode. */
function dafToken(roles) {
  const payload = Buffer.from(
    JSON.stringify({
      sub: "preview@rmb.co.za",
      roles,
      exp: Math.floor(Date.now() / 1000) + 3600,
    }),
  ).toString("base64url");
  return `eyJhbGciOiJub25lIn0.${payload}.preview`;
}

const items = [
  {
    agiId: 1,
    agmId: 10,
    committeeName: "Large Exposures Committee",
    mandateLevel: "L1",
    meetingDate: new Date(Date.now() + 2 * 86400000).toISOString(),
    counterpartyName: "Acme Mining Ltd",
    purposeOfSubmission: "Annual review",
    numberOfPages: 18,
    estimatedMinutes: 20,
    primaryAnalyst: "Preview User",
    itemStatus: "Captured",
    agendaNumber: "1.1",
  },
  {
    agiId: 2,
    agmId: 10,
    committeeName: "Large Exposures Committee",
    mandateLevel: "L1",
    meetingDate: new Date(Date.now() + 2 * 86400000).toISOString(),
    counterpartyName: "Beta Retail Holdings",
    purposeOfSubmission: "New facility",
    numberOfPages: 9,
    estimatedMinutes: 15,
    primaryAnalyst: "Another Analyst",
    itemStatus: "Confirmed",
    agendaNumber: "1.2",
  },
];

/**
 * All DAF traffic is fulfilled with local fixtures; no real DAF, SSO server or gateway is used.
 * `signedIn` seeds a DAF session for the tab.
 */
async function fixture(page, { signedIn = true, roles = ["creditanalyst"], unavailable = false } = {}) {
  const errors = [];
  page.on("pageerror", (e) => errors.push(e.message));
  page.on("console", (m) => {
    if (m.type() === "error" && /NG\d{4}|NullInjectorError|federation init failed/.test(m.text()))
      errors.push(m.text());
  });
  const token = dafToken(roles);
  await page.addInitScript(
    ({ token, roles, signedIn }) => {
      sessionStorage.setItem("daf.sso_failed", "1");
      if (signedIn) {
        sessionStorage.setItem("daf.session.token", token);
        sessionStorage.setItem(
          "daf.session.user",
          JSON.stringify({
            username: "preview@rmb.co.za",
            displayName: "Preview User",
            domain: "rmb.co.za",
            roles,
            tenantId: 1,
          }),
        );
      }
    },
    { token, roles, signedIn },
  );
  await page.route(`${API}/**`, async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace(/^\/daf/, "");
    if (unavailable) return route.fulfill({ status: 503, body: "" });
    if (path === "/auth/ssoServers") return route.fulfill({ json: [] });
    if (path === "/approvalForum/agenda/items" || path === "/approvalForum/agenda/items/my")
      return route.fulfill({ json: items });
    return route.fulfill({ status: 404, body: "" });
  });
  return errors;
}

test("standalone: signed-in analyst sees the agenda list", async ({ page }) => {
  const errors = await fixture(page);
  await page.goto(`${APP}/agenda`);
  await expect(page.getByRole("heading", { name: "Agenda items" }).first()).toBeVisible();
  await expect(page.getByText("Acme Mining Ltd")).toBeVisible();
  await expect(page.getByRole("group", { name: "Agenda actions" }).getByRole("button", { name: /Book agenda item/ })).toBeVisible();
  expect(errors).toEqual([]);
});

test("standalone: a chairperson without the analyst role cannot book", async ({ page }) => {
  await fixture(page, { roles: ["chairperson"] });
  await page.goto(`${APP}/agenda`);
  await expect(page.getByText("Beta Retail Holdings")).toBeVisible();
  await expect(page.getByRole("group", { name: "Agenda actions" }).getByRole("button", { name: /Book agenda item/ })).toHaveCount(0);
});

test("standalone: without a session the login page is shown", async ({ page }) => {
  await fixture(page, { signedIn: false });
  await page.goto(`${APP}/agenda`);
  await expect(page).toHaveURL(/\/login/);
  await expect(page.getByRole("button", { name: "Sign In", exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Sign in with Windows SSO" })).toBeVisible();
});

test("standalone: DAF unavailable shows a retryable error, not a blank page", async ({ page }) => {
  await fixture(page, { unavailable: true });
  await page.goto(`${APP}/agenda`);
  await expect(page.getByText("Agenda items could not load")).toBeVisible();
  await expect(page.getByRole("button", { name: /Retry|Try again/i })).toBeVisible();
});

test("standalone: a review analyst can open the agenda list but cannot book", async ({ page }) => {
  await fixture(page, { roles: ["ReviewAnalyst"] });
  await page.goto(`${APP}/agenda`);
  await expect(page.getByText("Acme Mining Ltd")).toBeVisible();
  await expect(page.getByRole("group", { name: "Agenda actions" }).getByRole("button", { name: /Book agenda item/ })).toHaveCount(0);
});

test("standalone: the secretariat sees Committee administration, an analyst does not", async ({ page, browser }) => {
  await fixture(page, { roles: ["Secretary"] });
  await page.goto(`${APP}/agenda`);
  const nav = page.getByRole("navigation", { name: "Primary navigation" });
  await expect(nav.getByRole("button", { name: "Committee administration" })).toBeVisible();

  const analystContext = await browser.newContext();
  const analyst = await analystContext.newPage();
  await fixture(analyst, { roles: ["CreditAnalyst"] });
  await analyst.goto(`${APP}/agenda`);
  await expect(analyst.getByText("Acme Mining Ltd")).toBeVisible();
  await expect(
    analyst.getByRole("navigation", { name: "Primary navigation" }).getByRole("button", { name: "Committee administration" }),
  ).toHaveCount(0);
  await analystContext.close();
});
