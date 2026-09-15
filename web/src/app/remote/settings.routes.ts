import { Routes } from "@angular/router";

export { getRemoteNavigation } from "./remote.navigation";
export { configureRemoteHost } from "./remote-host";

/**
 * The settings entries the Credit shell mounts (`./SettingsRoutes`): flat leaves
 * with `data.settingsCard`, linked from the shell's settings page.
 */
export const DAF_SETTINGS_ROUTES: Routes = [
  {
    path: "dashboard",
    redirectTo: "/agenda/dashboard",
    data: {
      settingsCard: {
        title: "Committee dashboard",
        description: "Meeting schedule, running order and papers outstanding.",
        icon: "pi-calendar",
      },
    },
  },
];

/** The literal export name the shell reads (remote contract). */
export const remoteRoutes = DAF_SETTINGS_ROUTES;

export const remoteDisplayName = "Committee Agendas";
