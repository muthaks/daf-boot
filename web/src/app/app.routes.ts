import { Routes } from "@angular/router";
import { authGuard } from "./core/guards/auth.guard";
import { AGENDA_ROUTES } from "./features/agenda/agenda.routes";

/** Standalone routes: login + the same pages as the federation surface, under the app's own chrome. */
export const routes: Routes = [
  {
    path: "login",
    loadComponent: () =>
      import("./features/auth/login.component").then((m) => m.LoginComponent),
  },
  {
    path: "unauthorized",
    loadComponent: () =>
      import("./features/auth/unauthorized.component").then(
        (m) => m.UnauthorizedComponent,
      ),
  },
  {
    path: "",
    loadComponent: () =>
      import("./layout/app-shell.component").then((m) => m.AppShellComponent),
    canActivateChild: [authGuard],
    children: [
      {
        path: "home",
        data: { dafHomePage: true },
        loadComponent: () =>
          import("./features/home/daf-home.component").then((m) => m.DafHomeComponent),
      },
      ...AGENDA_ROUTES(),
      { path: "", redirectTo: "home", pathMatch: "full" },
    ],
  },
  { path: "**", redirectTo: "home" },
];
