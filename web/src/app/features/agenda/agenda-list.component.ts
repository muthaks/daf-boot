import { Component, computed, DestroyRef, inject, OnInit, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { DatePipe } from "@angular/common";
import { ActivatedRoute } from "@angular/router";
import { TableModule } from "primeng/table";
import {
  CoActionGroupComponent,
  CoBreadcrumb,
  CoButtonComponent,
  CoContentStackComponent,
  CoDetailCardComponent,
  CoEmptyStateComponent,
  CoErrorStateComponent,
  CoFieldGridComponent,
  CoFilterChip,
  CoFilterChipsComponent,
  CoPageHeaderComponent,
  CoSearchFieldComponent,
  CoSpinnerComponent,
  CoStatCardComponent,
  CoStatusComponent,
  CoStatusKind,
} from "co-ds";
import { AgendaItem } from "../../core/models/agenda.models";
import { AgendaService } from "../../core/services/agenda.service";
import { DafNavigationService } from "../../core/services/daf-navigation.service";
import { PermissionsService } from "../../core/services/permissions.service";
import { SessionService } from "../../core/services/session.service";
import { getRemoteHost } from "../../remote/remote-host";
import { CanDirective } from "../../shared/directives/can.directive";

type StatusFilter = "all" | "captured" | "submitted" | "approved" | "confirmed" | "returned" | "rejected" | "removed";

/** Item statuses DAF uses (BRS v0.1 workflow): filter id to DAF status. */
const STATUS_FILTERS: Record<Exclude<StatusFilter, "all">, { status: string; label: string }> = {
  captured: { status: "Captured", label: "Awaiting paper" },
  submitted: { status: "Submitted", label: "Submitted" },
  returned: { status: "Returned", label: "Returned" },
  approved: { status: "Approved", label: "Approved" },
  rejected: { status: "Rejected", label: "Rejected" },
  confirmed: { status: "Confirmed", label: "Confirmed" },
  removed: { status: "Removed", label: "Removed" },
};

/** All / my agenda items (DAF `agenda-list.component`), one page with a route flag. */
@Component({
  selector: "app-agenda-list",
  standalone: true,
  imports: [
    DatePipe,
    TableModule,
    CoActionGroupComponent,
    CoButtonComponent,
    CoContentStackComponent,
    CoDetailCardComponent,
    CoEmptyStateComponent,
    CoErrorStateComponent,
    CoFieldGridComponent,
    CoFilterChipsComponent,
    CoPageHeaderComponent,
    CoSearchFieldComponent,
    CoSpinnerComponent,
    CoStatCardComponent,
    CoStatusComponent,
    CanDirective,
  ],
  templateUrl: "./agenda-list.component.html",
  host: { "data-co-daf": "", "data-co-pattern": "daf-agenda-list" },
})
export class AgendaListComponent implements OnInit {
  private readonly agenda = inject(AgendaService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly permissions = inject(PermissionsService);
  readonly navigation = inject(DafNavigationService);
  readonly session = inject(SessionService);
  readonly hosted = !!getRemoteHost();
  readonly mine = !!this.route.snapshot.data["mine"];
  readonly title = this.mine ? "My agenda items" : "Agenda items";
  readonly breadcrumbs: CoBreadcrumb[] = [
    { label: "Committee Agendas", route: this.navigation.href("/home") },
    { label: this.title },
  ];

  readonly items = signal<AgendaItem[]>([]);
  readonly loading = signal(false);
  readonly error = signal("");
  readonly query = signal("");
  readonly status = signal<StatusFilter>("all");
  readonly first = signal(0);

  readonly statusChips = computed<CoFilterChip[]>(() => {
    const count = (s: string) => this.items().filter((i) => i.itemStatus === s).length;
    return [
      { id: "all", label: "All", count: this.items().length },
      ...(Object.entries(STATUS_FILTERS) as [Exclude<StatusFilter, "all">, { status: string; label: string }][])
        .map(([id, f]) => ({ id, label: f.label, count: count(f.status) }))
        // Keep the original three chips even when empty; show workflow statuses only when present.
        .filter((chip) => ["captured", "confirmed", "removed"].includes(chip.id) || chip.count > 0),
    ];
  });

  readonly filtered = computed(() => {
    const q = this.query().trim().toLowerCase();
    const status = this.status();
    return this.items().filter((i) => {
      if (status !== "all" && i.itemStatus !== STATUS_FILTERS[status].status) return false;
      if (!q) return true;
      return [
        i.counterpartyName,
        i.committeeName,
        i.mandateLevel,
        i.purposeOfSubmission,
        i.itemStatus,
        i.primaryAnalyst,
        i.agendaNumber,
      ]
        .join(" ")
        .toLowerCase()
        .includes(q);
    });
  });

  readonly upcoming = computed(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return this.items().filter(
      (i) => i.itemStatus !== "Removed" && new Date(i.meetingDate).getTime() >= today.getTime(),
    ).length;
  });
  readonly papersOutstanding = computed(
    () => this.items().filter((i) => i.itemStatus === "Captured" || i.itemStatus === "Returned").length,
  );
  readonly canBook = computed(() => this.permissions.can("agenda.book"));

  ngOnInit(): void {
    this.session.ensure();
    this.load();
  }

  load(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set("");
    (this.mine ? this.agenda.getMyItems() : this.agenda.getAllItems())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (items) => {
          this.items.set(items ?? []);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(
            err?.status === 401 || err?.status === 403
              ? "DAF did not accept your session for the agenda module. Sign in again, or ask the DAF administrators for an agenda role."
              : "The DAF service did not answer. It may be down or unreachable from this network.",
          );
          this.loading.set(false);
        },
      });
  }

  applyFilter(value: string): void {
    this.query.set(value);
    this.first.set(0);
  }

  setStatus(id: string): void {
    this.status.set((id as StatusFilter) || "all");
    this.first.set(0);
  }

  statusKind(status: string): CoStatusKind {
    switch (status) {
      case "Confirmed":
      case "Approved":
        return "active";
      case "Submitted":
        return "info";
      case "Removed":
      case "Rejected":
        return "blocked";
      default:
        // Captured, Returned: waiting on the analyst.
        return "risk";
    }
  }

  openItem(item: AgendaItem): void {
    this.navigation.navigate(`/agenda/item/${item.agiId}`);
  }

  openMeeting(item: AgendaItem, event: Event): void {
    event.stopPropagation();
    this.navigation.navigate(`/agenda/meeting/${item.agmId}`);
  }
}
