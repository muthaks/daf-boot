import type { CoNavigationItem } from "co-ds";
import { getRemoteHost, getRemotePermissions } from "./remote-host";

interface RemoteNavigationContext {
  basePath: string;
  settingsBasePath: string;
  role: string;
  groups: readonly string[];
  permissions?: Readonly<Record<string, boolean>>;
}

/**
 * The app's thin menu, rendered by the shell under the platform chrome (and by
 * the standalone chrome). Items are filtered by the UI permissions derived from
 * the DAF roles in the user's DAF token (`core/models/permissions.ts`): hosted, from
 * the signal published in `remote-host.ts`; standalone, from the context the app shell
 * passes. Until the permissions are known only Home is offered, and DAF still enforces
 * access on every call regardless of what the menu shows.
 */
export function getRemoteNavigation(
  context: RemoteNavigationContext,
): readonly CoNavigationItem[] {
  const base = context.basePath;
  const permissions = getRemoteHost() ? getRemotePermissions() : context.permissions;
  const home: CoNavigationItem = { id: base + "/home", label: "Home" };
  if (!permissions) return [home];
  const items: CoNavigationItem[] = [
    home,
    {
      id: "daf-agendas",
      label: "Committee agendas",
      children: [
        { id: base + "/agenda", label: "All agenda items" },
        { id: base + "/agenda/my", label: "My agenda items" },
        { id: base + "/agenda/dashboard", label: "Committee dashboard" },
      ],
    },
    { id: base + "/agenda/new", label: "Book agenda item" },
    { id: base + "/agenda/admin", label: "Committee administration" },
  ];
  const required: Record<string, string> = {
    [base + "/agenda"]: "agenda.view",
    [base + "/agenda/my"]: "agenda.view",
    [base + "/agenda/dashboard"]: "agenda.view",
    [base + "/agenda/new"]: "agenda.book",
    [base + "/agenda/admin"]: "agenda.admin",
  };
  const permitted = (entries: readonly CoNavigationItem[]): CoNavigationItem[] =>
    entries.flatMap((item) => {
      if (item.children) {
        const children = permitted(item.children);
        return children.length ? [{ ...item, children }] : [];
      }
      return !required[item.id] || permissions[required[item.id]] ? [item] : [];
    });
  return permitted(items);
}
