import { Routes } from "@angular/router";
import { dafSessionGuard } from "../core/guards/daf-session.guard";
import { AGENDA_ROUTES } from "../features/agenda/agenda.routes";
import { withHeading } from "./remote-heading";

export { getRemoteNavigation } from "./remote.navigation";
export { configureRemoteHost } from "./remote-host";

/**
 * The routes the Credit shell mounts when it embeds the DAF agenda remote (`./RemoteRoutes`).
 * Flat routes, no wrapper component; each page publishes its heading band via `withHeading`.
 * Standalone chrome and login are deliberately excluded (the shell signs in; the DAF session
 * is established silently through Windows SSO by `dafSessionGuard`).
 */
export const DAF_REMOTE_ROUTES: Routes = [
  {
    path: "",
    data: { dafRoot: true },
    canActivateChild: [dafSessionGuard],
    children: [
      {
        path: "home",
        data: { dafHomePage: true },
        loadComponent: () =>
          import("../features/home/daf-home.component").then((m) => m.DafHomeComponent),
      },
      ...AGENDA_ROUTES(withHeading),
      { path: "", redirectTo: "home", pathMatch: "full" },
    ],
  },
];

/** The literal export name the shell reads (remote contract). */
export const remoteRoutes = DAF_REMOTE_ROUTES;

export const remoteDisplayName = "Committee Agendas";

/** Pages are designed full-bleed (standalone shell runs uncontained). */
export const remoteLayout = "full" as const;
