import { Component, computed, DestroyRef, inject, OnInit, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { DatePipe } from "@angular/common";
import { TableModule } from "primeng/table";
import * as XLSX from "xlsx";
import {
  CoActionGroupComponent,
  CoBreadcrumb,
  CoButtonComponent,
  CoContentStackComponent,
  CoDetailCardComponent,
  CoEmptyStateComponent,
  CoErrorStateComponent,
  CoFieldComponent,
  CoFieldGridComponent,
  CoInputDirective,
  CoPageHeaderComponent,
  CoSectionHeadingComponent,
  CoSpinnerComponent,
  CoStatCardComponent,
  CoStatusComponent,
  CoStatusKind,
  CoTabItem,
  CoTabsComponent,
} from "co-ds";
import { AgendaItem, AgendaReportSummary } from "../../core/models/agenda.models";
import { AgendaService } from "../../core/services/agenda.service";
import { DafNavigationService } from "../../core/services/daf-navigation.service";
import { SessionService } from "../../core/services/session.service";
import { getRemoteHost } from "../../remote/remote-host";

/** One committee meeting derived from its agenda items (DAF has no meeting call for this page). */
export interface MeetingGroup {
  agmId: number;
  committeeName: string;
  mandateLevel: string;
  meetingDate: string;
  items: AgendaItem[];
  totalMinutes: number;
  /** Earliest item start time (HH:mm), "" when no item has one. */
  startTime: string;
  /** Latest item end time (HH:mm), "" when no item has one. */
  endTime: string;
}

type Week = "this" | "next";
type View = "week" | "all";

/** The meetings of one calendar day in the two-week window, or an empty week marker. */
interface WeekGroup {
  week: Week;
  /** `Date#toDateString()` of the day; "" for an empty-week marker. */
  day: string;
  date: string;
  meetings: MeetingGroup[];
  empty: boolean;
}

/** A week group on the current page, with the labels the page shows before it. */
interface PagedWeekGroup extends WeekGroup {
  showWeekLabel: boolean;
  showExpandAll: boolean;
}

/** One key/count row of a report breakdown map (DAF `reportRows`). */
export interface ReportRow {
  key: string;
  value: number;
}

/** A titled breakdown of the operational report. */
interface ReportBreakdown {
  id: string;
  title: string;
  rows: ReportRow[];
}

const DAY_MS = 86400000;
const WEEK_PAGE_SIZE = 3;
const ALL_PAGE_SIZE = 4;

/** Committee dashboard (DAF `agenda-dashboard.component`): schedule and running order for two weeks. */
@Component({
  selector: "app-agenda-dashboard",
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
    CoFieldComponent,
    CoFieldGridComponent,
    CoInputDirective,
    CoPageHeaderComponent,
    CoSectionHeadingComponent,
    CoSpinnerComponent,
    CoStatCardComponent,
    CoStatusComponent,
    CoTabsComponent,
  ],
  templateUrl: "./agenda-dashboard.component.html",
  host: { "data-co-daf": "", "data-co-pattern": "daf-agenda-dashboard" },
})
export class AgendaDashboardComponent implements OnInit {
  private readonly agenda = inject(AgendaService);
  private readonly destroyRef = inject(DestroyRef);
  readonly navigation = inject(DafNavigationService);
  readonly session = inject(SessionService);
  readonly hosted = !!getRemoteHost();
  readonly breadcrumbs: CoBreadcrumb[] = [
    { label: "Committee Agendas", route: this.navigation.href("/home") },
    { label: "Agenda items", route: this.navigation.href("/agenda") },
    { label: "Committee dashboard" },
  ];
  readonly tabs: CoTabItem[] = [
    { id: "week", label: "This & next week" },
    { id: "all", label: "All meetings" },
  ];

  readonly items = signal<AgendaItem[]>([]);
  readonly loading = signal(false);
  readonly error = signal("");
  readonly view = signal<View>("week");
  readonly weekPage = signal(1);
  readonly page = signal(1);
  /** Meetings the user collapsed; every meeting starts expanded, as in DAF. */
  readonly collapsed = signal<ReadonlySet<number>>(new Set<number>());
  /** Midnight of the day the data was loaded, so a refresh re-anchors the two-week window. */
  private readonly today = signal(startOfDay(new Date()));

  /** Removed items never count towards the schedule or the efficiency figures. */
  readonly activeItems = computed(() => this.items().filter((i) => i.itemStatus !== "Removed"));

  // --- computeSchedule ---------------------------------------------------------------------

  readonly allMeetings = computed<MeetingGroup[]>(() => {
    const byMeeting = new Map<number, MeetingGroup>();
    for (const i of this.activeItems()) {
      let m = byMeeting.get(i.agmId);
      if (m === undefined) {
        m = {
          agmId: i.agmId,
          committeeName: i.committeeName,
          mandateLevel: i.mandateLevel,
          meetingDate: i.meetingDate,
          items: [],
          totalMinutes: 0,
          startTime: "",
          endTime: "",
        };
        byMeeting.set(i.agmId, m);
      }
      m.items.push(i);
      m.totalMinutes += i.estimatedMinutes || 0;
      if (i.startTime && (m.startTime === "" || toMinutes(i.startTime) < toMinutes(m.startTime))) {
        m.startTime = i.startTime;
      }
      if (i.endTime && (m.endTime === "" || toMinutes(i.endTime) > toMinutes(m.endTime))) {
        m.endTime = i.endTime;
      }
    }
    const meetings = [...byMeeting.values()];
    for (const m of meetings) {
      m.items.sort(
        (a, b) =>
          toMinutes(a.startTime) - toMinutes(b.startTime) ||
          `${a.agendaNumber ?? ""}`.localeCompare(`${b.agendaNumber ?? ""}`),
      );
    }
    meetings.sort(
      (a, b) =>
        new Date(a.meetingDate).getTime() - new Date(b.meetingDate).getTime() ||
        toMinutes(a.startTime) - toMinutes(b.startTime),
    );
    return meetings;
  });

  /** Monday 00:00 of the current week, then +7 and +14 days. */
  private readonly window = computed(() => {
    const weekStart = startOfWeek(this.today());
    const nextWeekStart = new Date(weekStart.getTime());
    nextWeekStart.setDate(nextWeekStart.getDate() + 7);
    const windowEnd = new Date(weekStart.getTime());
    windowEnd.setDate(windowEnd.getDate() + 14);
    return { weekStart, nextWeekStart, windowEnd };
  });

  readonly meetingsThisWeek = computed(() => {
    const { weekStart, nextWeekStart } = this.window();
    return this.allMeetings().filter((m) => inRange(m, weekStart, nextWeekStart));
  });
  readonly meetingsNextWeek = computed(() => {
    const { nextWeekStart, windowEnd } = this.window();
    return this.allMeetings().filter((m) => inRange(m, nextWeekStart, windowEnd));
  });
  readonly itemsThisWeek = computed(() => countItems(this.meetingsThisWeek()));
  readonly itemsNextWeek = computed(() => countItems(this.meetingsNextWeek()));
  readonly papersOutstanding = computed(() =>
    [...this.meetingsThisWeek(), ...this.meetingsNextWeek()].reduce(
      (n, m) => n + m.items.filter((i) => i.itemStatus !== "Confirmed").length,
      0,
    ),
  );

  // --- computeEfficiency -------------------------------------------------------------------

  readonly onTimePct = computed(() => {
    const total = this.activeItems().length;
    const onTime = this.activeItems().filter((i) => !i.lateSubmission).length;
    return total === 0 ? 0 : Math.round((onTime * 100) / total);
  });
  readonly withinGuidancePct = computed(() => {
    const total = this.activeItems().length;
    const within = this.activeItems().filter((i) => i.withinPageGuidance).length;
    return total === 0 ? 0 : Math.round((within * 100) / total);
  });
  private readonly pastItems = computed(() => {
    const today = this.today().getTime();
    return this.activeItems().filter((i) => new Date(i.meetingDate).getTime() < today);
  });
  readonly decisionsDue = computed(() => this.pastItems().length);
  readonly decisionsCaptured = computed(
    () => this.pastItems().filter((i) => !!i.postMeeting?.decision).length,
  );

  // --- Week view: meetings grouped per day, paged by day --------------------------------

  readonly weekGroups = computed<WeekGroup[]>(() => {
    const groups: WeekGroup[] = [];
    const addWeek = (week: Week, meetings: MeetingGroup[]) => {
      if (meetings.length === 0) {
        groups.push({ week, day: "", date: "", meetings: [], empty: true });
        return;
      }
      let group: WeekGroup | null = null;
      for (const m of meetings) {
        const day = new Date(m.meetingDate).toDateString();
        if (group === null || group.day !== day) {
          group = { week, day, date: m.meetingDate, meetings: [], empty: false };
          groups.push(group);
        }
        group.meetings.push(m);
      }
    };
    addWeek("this", this.meetingsThisWeek());
    addWeek("next", this.meetingsNextWeek());
    return groups;
  });
  readonly weekTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.weekGroups().length / WEEK_PAGE_SIZE)),
  );
  /** Current week page, clamped so a refresh with fewer days never shows an empty page. */
  readonly currentWeekPage = computed(() => Math.min(this.weekPage(), this.weekTotalPages()));
  readonly pagedWeekGroups = computed<PagedWeekGroup[]>(() => {
    const start = (this.currentWeekPage() - 1) * WEEK_PAGE_SIZE;
    const slice = this.weekGroups().slice(start, start + WEEK_PAGE_SIZE);
    let labelShown = false;
    return slice.map((g, ix) => {
      const showWeekLabel = ix === 0 || slice[ix - 1].week !== g.week;
      const showExpandAll = showWeekLabel && !labelShown;
      labelShown = labelShown || showWeekLabel;
      return { ...g, showWeekLabel, showExpandAll };
    });
  });

  private readonly windowMeetingIds = computed(() =>
    [...this.meetingsThisWeek(), ...this.meetingsNextWeek()].map((m) => m.agmId),
  );
  readonly allExpanded = computed(() => {
    const collapsed = this.collapsed();
    return this.windowMeetingIds().every((id) => !collapsed.has(id));
  });

  // --- All meetings view, paged ----------------------------------------------------------

  readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.allMeetings().length / ALL_PAGE_SIZE)),
  );
  readonly currentPage = computed(() => Math.min(this.page(), this.totalPages()));
  readonly pagedAll = computed(() => {
    const start = (this.currentPage() - 1) * ALL_PAGE_SIZE;
    return this.allMeetings().slice(start, start + ALL_PAGE_SIZE);
  });
  readonly pageStart = computed(() =>
    this.allMeetings().length === 0 ? 0 : (this.currentPage() - 1) * ALL_PAGE_SIZE + 1,
  );
  readonly pageEnd = computed(() =>
    Math.min(this.currentPage() * ALL_PAGE_SIZE, this.allMeetings().length),
  );

  // --- Operational report (BRS section 10, server aggregates) -----------------------------

  readonly report = signal<AgendaReportSummary | null>(null);
  /** The range the shown report was run for; the export names and labels it. */
  readonly reportRange = signal<{ from: string; to: string } | null>(null);
  /** DAF defaults: 90 days back to 30 days ahead, as yyyy-MM-dd (UTC, as DAF computes them). */
  readonly reportFrom = signal(new Date(Date.now() - 90 * DAY_MS).toISOString().substring(0, 10));
  readonly reportTo = signal(new Date(Date.now() + 30 * DAY_MS).toISOString().substring(0, 10));
  readonly showReport = signal(false);
  readonly reportLoading = signal(false);
  readonly reportError = signal("");

  /** Persistent range validation: both dates required and From on or before To. */
  readonly reportRangeError = computed(() => {
    const from = this.reportFrom();
    const to = this.reportTo();
    if (!from || !to) return "Enter both a From and a To date.";
    return from > to ? "The To date must be on or after the From date." : "";
  });

  readonly reportBreakdowns = computed<ReportBreakdown[]>(() => {
    const r = this.report();
    if (r === null) return [];
    return [
      { id: "status", title: "Items by status", rows: this.reportRows(r.itemsByStatus) },
      { id: "committee", title: "Items by committee", rows: this.reportRows(r.itemsByCommittee) },
      {
        id: "businessUnit",
        title: "Items by business unit",
        rows: this.reportRows(r.itemsByBusinessUnit),
      },
      { id: "purpose", title: "Items by purpose", rows: this.reportRows(r.itemsByPurpose) },
      { id: "decisions", title: "Decisions", rows: this.reportRows(r.decisions) },
      { id: "meetings", title: "Meetings by status", rows: this.reportRows(r.meetingsByStatus) },
    ];
  });

  readonly lateItemDetails = computed<AgendaItem[]>(() => this.report()?.lateItemDetails ?? []);

  ngOnInit(): void {
    this.session.ensure();
    this.load();
  }

  load(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set("");
    this.agenda
      .getAllItems()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (items) => {
          this.today.set(startOfDay(new Date()));
          this.items.set(items ?? []);
          this.loading.set(false);
        },
        error: (err: { status?: number } | null) => {
          this.error.set(
            err?.status === 401 || err?.status === 403
              ? "DAF did not accept your session for the agenda module. Sign in again or ask for the credit analyst or chairperson role."
              : "The DAF service did not answer. It may be down or unreachable from this network.",
          );
          this.loading.set(false);
        },
      });
  }

  setView(id: string): void {
    this.view.set(id === "all" ? "all" : "week");
    this.page.set(1);
    this.weekPage.set(1);
  }

  setWeekPage(p: number): void {
    if (p < 1 || p > this.weekTotalPages()) return;
    this.weekPage.set(p);
  }

  setPage(p: number): void {
    if (p < 1 || p > this.totalPages()) return;
    this.page.set(p);
  }

  isExpanded(m: MeetingGroup): boolean {
    return !this.collapsed().has(m.agmId);
  }

  toggleMeeting(m: MeetingGroup): void {
    const next = new Set(this.collapsed());
    if (next.has(m.agmId)) {
      next.delete(m.agmId);
    } else {
      next.add(m.agmId);
    }
    this.collapsed.set(next);
  }

  toggleAll(): void {
    if (this.allExpanded()) {
      this.collapsed.set(new Set(this.windowMeetingIds()));
    } else {
      const next = new Set(this.collapsed());
      for (const id of this.windowMeetingIds()) next.delete(id);
      this.collapsed.set(next);
    }
  }

  toggleReport(): void {
    this.showReport.update((v) => !v);
    if (this.showReport() && this.report() === null) {
      this.loadReport();
    }
  }

  loadReport(): void {
    if (this.reportLoading() || this.reportRangeError()) return;
    const from = this.reportFrom();
    const to = this.reportTo();
    this.reportLoading.set(true);
    this.reportError.set("");
    this.agenda
      .getReportSummary(from, to)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (summary) => {
          this.report.set(summary);
          this.reportRange.set({ from, to });
          this.reportLoading.set(false);
        },
        error: (err: unknown) => {
          const e = err as { error?: { message?: string } } | null;
          this.reportError.set(e?.error?.message || "Failed to load the operational report.");
          this.reportLoading.set(false);
        },
      });
  }

  reportRows(map: Record<string, number> | null | undefined): ReportRow[] {
    return map ? Object.keys(map).map((k) => ({ key: k, value: map[k] })) : [];
  }

  /** DAF `exportReport`: one "Report" sheet, same rows, sections and file name. */
  exportReport(): void {
    const r = this.report();
    const range = this.reportRange();
    if (r === null || range === null) return;
    const rows: (string | number)[][] = [
      ["AGENDA BUILDER OPERATIONAL REPORT"],
      ["From", range.from],
      ["To", range.to],
      [],
      ["Meetings", r.meetings],
      ["Agenda items", r.items],
      ["Late submissions", r.lateItems],
      ["Outstanding mandatory papers", r.outstandingMandatoryPapers],
      ["Planned discussion minutes", r.plannedMinutes],
      ["Actual discussion minutes", r.actualMinutes],
      ["Invited", r.invited],
      ["Planned attending", r.plannedAttending],
      ["Actually attended", r.actuallyAttended],
      ["Quorum met (items)", r.quorumMet],
      ["Quorum not met (items)", r.quorumNotMet],
      ["Open conditions", r.openConditions],
      ["Overdue conditions", r.overdueConditions],
      [],
    ];
    const section = (title: string, map: Record<string, number> | null | undefined) => {
      rows.push([title]);
      for (const x of this.reportRows(map)) rows.push([x.key, x.value]);
      rows.push([]);
    };
    section("ITEMS BY STATUS", r.itemsByStatus);
    section("ITEMS BY COMMITTEE", r.itemsByCommittee);
    section("ITEMS BY BUSINESS UNIT", r.itemsByBusinessUnit);
    section("ITEMS BY PURPOSE", r.itemsByPurpose);
    section("DECISIONS", r.decisions);
    section("MEETINGS BY STATUS", r.meetingsByStatus);
    rows.push(["LATE SUBMISSIONS"]);
    rows.push(["Committee", "Meeting date", "Counterparty", "Reason"]);
    for (const i of r.lateItemDetails ?? []) {
      rows.push([
        i.committeeName + " " + (i.mandateLevel || ""),
        i.meetingDate ? String(i.meetingDate).substring(0, 10) : "",
        i.counterpartyName,
        i.lateSubmissionReason || "",
      ]);
    }
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet(rows), "Report");
    XLSX.writeFile(workbook, `Agenda_Report_${range.from}_${range.to}.xlsx`);
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
      case "Captured":
      case "Returned":
        return "risk";
      default:
        return "risk";
    }
  }

  meetingTitle(m: MeetingGroup): string {
    return m.mandateLevel ? `${m.committeeName} (${m.mandateLevel})` : m.committeeName;
  }

  /** "09:00 – 11:30 · 3 items · 90 min", without the time when no item has one. */
  meetingSummary(m: MeetingGroup): string {
    const parts: string[] = [];
    if (m.startTime) parts.push(m.endTime ? `${m.startTime} – ${m.endTime}` : m.startTime);
    parts.push(`${m.items.length} ${m.items.length === 1 ? "item" : "items"}`);
    parts.push(`${m.totalMinutes} min`);
    return parts.join(" · ");
  }

  itemTime(item: AgendaItem): string {
    if (!item.startTime) return "—";
    return item.endTime ? `${item.startTime} – ${item.endTime}` : item.startTime;
  }

  openItem(item: AgendaItem): void {
    this.navigation.navigate(`/agenda/item/${item.agiId}`);
  }

  openMeeting(m: MeetingGroup): void {
    this.navigation.navigate(`/agenda/meeting/${m.agmId}`);
  }
}

function startOfDay(d: Date): Date {
  const x = new Date(d.getTime());
  x.setHours(0, 0, 0, 0);
  return x;
}

/** Monday 00:00 of the week containing d. */
function startOfWeek(d: Date): Date {
  const x = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  x.setDate(x.getDate() - ((x.getDay() + 6) % 7));
  return x;
}

/** Minutes since midnight for "HH:mm"; missing times sort last. */
function toMinutes(time: string | undefined): number {
  if (!time || time.indexOf(":") < 0) return Number.MAX_SAFE_INTEGER;
  const parts = time.split(":");
  return Number(parts[0]) * 60 + Number(parts[1]);
}

function inRange(m: MeetingGroup, from: Date, to: Date): boolean {
  const d = new Date(m.meetingDate).getTime();
  return d >= from.getTime() && d < to.getTime();
}

function countItems(meetings: MeetingGroup[]): number {
  return meetings.reduce((n, m) => n + m.items.length, 0);
}
