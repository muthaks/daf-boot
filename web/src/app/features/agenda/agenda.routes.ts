import { CanActivateFn, Routes } from "@angular/router";

/**
 * The agenda pages, shared by the standalone routes and the federation surface. `heading` is
 * the shell's `withHeading` guard factory (remote); standalone passes nothing and each page
 * renders its own `co-page-header`.
 */
export function AGENDA_ROUTES(
  heading?: (title: string, description: string) => CanActivateFn,
): Routes {
  const guard = (title: string, description: string) =>
    heading ? { canActivate: [heading(title, description)] } : {};
  return [
    {
      path: "agenda",
      loadComponent: () =>
        import("./agenda-list.component").then((m) => m.AgendaListComponent),
      ...guard("Agenda items", "Every item booked on a credit committee agenda."),
    },
    {
      path: "agenda/my",
      data: { mine: true },
      loadComponent: () =>
        import("./agenda-list.component").then((m) => m.AgendaListComponent),
      ...guard("My agenda items", "Items you booked or analyse."),
    },
    {
      path: "agenda/dashboard",
      loadComponent: () =>
        import("./agenda-dashboard.component").then((m) => m.AgendaDashboardComponent),
      ...guard("Committee dashboard", "Meeting schedule and running order for the next two weeks."),
    },
    {
      path: "agenda/admin",
      loadComponent: () =>
        import("./agenda-admin.component").then((m) => m.AgendaAdminComponent),
      ...guard("Committee administration", "Committees, cut-off rules, quorum roles and the meeting calendar."),
    },
    {
      path: "agenda/new",
      loadComponent: () =>
        import("./agenda-new.component").then((m) => m.AgendaNewComponent),
      ...guard("Book an agenda item", "Reserve a slot on a committee meeting for a credit paper."),
    },
    {
      path: "agenda/item/:agiId",
      loadComponent: () =>
        import("./agenda-item.component").then((m) => m.AgendaItemComponent),
      ...guard("Agenda item", "Details, credit paper and post-meeting outcome."),
    },
    {
      path: "agenda/meeting/:agmId",
      loadComponent: () =>
        import("./agenda-meeting.component").then((m) => m.AgendaMeetingComponent),
      ...guard("Committee meeting", "Compile, order and lock the agenda for a meeting."),
    },
  ];
}
