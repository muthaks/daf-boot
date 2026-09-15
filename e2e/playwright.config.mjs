import { defineConfig } from "@playwright/test";

/**
 * Runs the remote standalone (http://localhost:4408) and hosted inside the
 * platform shell (http://localhost:4400). Both dev servers are started on
 * demand; set DAF_E2E_BASE / DAF_E2E_SHELL to point at deployed
 * environments instead (then no server is started).
 */
const APP = process.env.DAF_E2E_BASE ?? "http://localhost:4408";
const SHELL = process.env.DAF_E2E_SHELL ?? "http://localhost:4400";
const local = !process.env.DAF_E2E_BASE;

export default defineConfig({
  testDir: "./tests",
  timeout: 45000,
  expect: { timeout: 15000 },
  workers: 1,
  outputDir: "./test-results",
  reporter: [["list"], ["html", { open: "never", outputFolder: "./playwright-report" }]],
  use: {
    browserName: "chromium",
    ...(process.platform === "win32" ? { channel: "msedge" } : {}),
    headless: true,
    viewport: { width: 1440, height: 1000 },
    screenshot: "only-on-failure",
    trace: "retain-on-failure",
  },
  webServer: local
    ? [
        {
          command: "npm --prefix ../web start",
          url: APP,
          reuseExistingServer: true,
          timeout: 180000,
        },
        {
          command: "npm --prefix ../../cat-utils/co-platform/shell start",
          url: SHELL,
          reuseExistingServer: true,
          timeout: 180000,
        },
      ]
    : undefined,
});
