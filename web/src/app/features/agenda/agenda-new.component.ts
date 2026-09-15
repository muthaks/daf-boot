import { Component, computed, DestroyRef, inject, OnInit, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { catchError, debounceTime, distinctUntilChanged, of, Subject, switchMap, tap } from "rxjs";
import { DatePipe, NgTemplateOutlet } from "@angular/common";
import { FormsModule } from "@angular/forms";
import {
  CoActionGroupComponent,
  CoAlertComponent,
  CoBreadcrumb,
  CoButtonComponent,
  CoCheckboxComponent,
  CoContentStackComponent,
  CoDetailCardComponent,
  CoDetailFieldComponent,
  CoEmptyStateComponent,
  CoErrorStateComponent,
  CoFieldComponent,
  CoFieldGridComponent,
  CoIdentityCardComponent,
  CoInputDirective,
  CoPageHeaderComponent,
  CoSearchableSelectComponent,
  CoSectionHeadingComponent,
  CoSelectOption,
  CoSpinnerComponent,
  CoStatCardComponent,
  CoStatusComponent,
  CoTabItem,
  CoTabsComponent,
} from "co-ds";
import {
  AgendaCasdmApplication,
  AgendaCommittee,
  AgendaCounterparty,
  AgendaItem,
  AgendaParticipant,
  AgendaUser,
  BookAgendaItemRequest,
  HELD_VIA_OPTIONS,
} from "../../core/models/agenda.models";
import { AgendaService } from "../../core/services/agenda.service";
import { DafNavigationService } from "../../core/services/daf-navigation.service";
import { PermissionsService } from "../../core/services/permissions.service";
import { SessionService } from "../../core/services/session.service";
import { getRemoteHost } from "../../remote/remote-host";
import { DisableUnlessDirective } from "../../shared/directives/disable-unless.directive";

/** A person in a picker: from the committee's List Management list, the DAF directory or CASDM. */
export interface Person {
  displayName: string;
  userPrincipalName?: string;
  userId?: string;
  /** AD mail, when the person came from the directory search. */
  mail?: string;
  fromCommitteeList?: boolean;
}

/** The in-flight credit application returned by /external/casdm/application. */
interface CreditApplicationSummary {
  applicationId?: number;
  proposedDcp?: number | null;
  proposedFrRating?: string;
  creditAnalyst?: string;
  creditAnalystId?: string;
  creditAnalystEmail?: string;
  reviewAnalyst?: string;
  reviewAnalystId?: string;
  reviewAnalystEmail?: string;
}

type RosterKey =
  | "chair"
  | "independentMember"
  | "portfolioCce1"
  | "portfolioCce2"
  | "businessRep1"
  | "seniorBusinessRep"
  | "lecRep"
  | "eqMemberFrmExec"
  | "scribe";

type TeamKey = "creditTeam" | "businessTeam" | "mezz" | "esg" | "additional";
type PeopleListKey = RosterKey | TeamKey;

interface RosterRole {
  key: RosterKey;
  label: string;
}
interface RosterGroup {
  title: string;
  hint: string;
  roles: RosterRole[];
}

interface ParticipantChip {
  name: string;
  role: string;
  initials: string;
}

type RequiredField = "committee" | "meetingDate" | "counterpartyName" | "purposeOfSubmission";

const DAY_NAMES = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];

/**
 * Book an agenda item (DAF `agenda-new.component`): a six-step wizard capturing the committee
 * and date, the deal, the team, classification, participants, then a review before booking.
 */
@Component({
  selector: "app-agenda-new",
  standalone: true,
  imports: [
    DatePipe,
    NgTemplateOutlet,
    FormsModule,
    CoActionGroupComponent,
    CoAlertComponent,
    CoButtonComponent,
    CoCheckboxComponent,
    CoContentStackComponent,
    CoDetailCardComponent,
    CoDetailFieldComponent,
    CoEmptyStateComponent,
    CoErrorStateComponent,
    CoFieldComponent,
    CoFieldGridComponent,
    CoIdentityCardComponent,
    CoInputDirective,
    CoPageHeaderComponent,
    CoSearchableSelectComponent,
    CoSectionHeadingComponent,
    CoSpinnerComponent,
    CoStatCardComponent,
    CoStatusComponent,
    CoTabsComponent,
    DisableUnlessDirective,
  ],
  templateUrl: "./agenda-new.component.html",
  host: { "data-co-daf": "", "data-co-pattern": "daf-agenda-new" },
})
export class AgendaNewComponent implements OnInit {
  private readonly agenda = inject(AgendaService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly permissions = inject(PermissionsService);
  readonly navigation = inject(DafNavigationService);
  readonly session = inject(SessionService);
  readonly hosted = !!getRemoteHost();
  readonly breadcrumbs: CoBreadcrumb[] = [
    { label: "Committee Agendas", route: this.navigation.href("/home") },
    { label: "Agenda items", route: this.navigation.href("/agenda") },
    { label: "Book an agenda item" },
  ];
  readonly canBook = computed(() => this.permissions.can("agenda.book"));

  // ---- Reference data ----
  readonly committees = signal<AgendaCommittee[]>([]);
  readonly loading = signal(false);
  readonly error = signal("");
  readonly committeeOptions = computed<CoSelectOption<number>[]>(() =>
    this.committees().map((c) => ({
      value: c.agcId,
      label: `${c.committeeName} - ${c.mandateLevel}`,
      subtitle: c.levelDescription ?? "",
    })),
  );

  // Counterparty typeahead against the live CASDM search (every counterparty, with or without a
  // credit application); debounced, min 2 chars. Ones not on CASDM are captured manually.
  readonly cpTypeahead$ = new Subject<string>();
  readonly counterparties = signal<AgendaCounterparty[]>([]);
  readonly cpLoading = signal(false);
  /** The picked counterparty, kept in the options after the search results move on. */
  readonly selectedCounterparty = signal<AgendaCounterparty | null>(null);
  readonly counterpartyOptions = computed<CoSelectOption[]>(() => {
    const picked = this.selectedCounterparty();
    const all = picked ? [picked, ...this.counterparties()] : this.counterparties();
    const seen = new Set<string>();
    return all
      .filter((c) => {
        const id = this.inkombaOf(c);
        if (!id || !c.counterpartyName || seen.has(id)) return false;
        seen.add(id);
        return true;
      })
      .map((c) => ({ value: this.inkombaOf(c), label: c.counterpartyName ?? "" }));
  });

  // People pickers: the committee's List Management members (predefined pool) with no search
  // term; typing >= 2 chars searches the wider RMB AD instead (debounced).
  readonly userTypeahead$ = new Subject<string>();
  readonly committeeMembers = signal<Person[]>([]);
  readonly committeeListName = signal("");
  readonly adUsers = signal<Person[]>([]);
  readonly userLoading = signal(false);
  /** People picked or prefilled from CASDM, kept in the options so the pickers can display them. */
  readonly extraPeople = signal<Person[]>([]);
  readonly people = computed<Person[]>(() => {
    const seen = new Set<string>();
    const out: Person[] = [];
    for (const p of [...this.committeeMembers(), ...this.extraPeople(), ...this.adUsers()]) {
      const key = this.personKey(p);
      if (!key || seen.has(key)) continue;
      seen.add(key);
      out.push(p);
    }
    return out;
  });
  readonly peopleOptions = computed<CoSelectOption[]>(() =>
    this.people().map((p) => ({
      value: this.personKey(p),
      label: p.displayName,
      subtitle: p.userPrincipalName ?? p.userId ?? "",
      badge: p.fromCommitteeList ? "Committee" : undefined,
    })),
  );

  // ---- Static option lists (as DAF captures them) ----
  readonly roleGroups: RosterGroup[] = [
    {
      title: "Standard Quorum",
      hint: "",
      roles: [
        { key: "chair", label: "Chair" },
        { key: "independentMember", label: "Independent" },
        { key: "portfolioCce1", label: "Portfolio CCE / CCE 1" },
        { key: "portfolioCce2", label: "Portfolio CCE / CCE 2" },
        { key: "businessRep1", label: "Business Rep" },
        { key: "seniorBusinessRep", label: "Senior Business Rep" },
      ],
    },
    {
      title: "Enhanced Quorum",
      hint: "Added for Enhanced Quorum levels (L1/L2).",
      roles: [
        { key: "lecRep", label: "LEC Rep" },
        { key: "eqMemberFrmExec", label: "EQ Member / FRM Exec" },
      ],
    },
    { title: "Supporting", hint: "", roles: [{ key: "scribe", label: "Scribe" }] },
  ];
  readonly purposeOptions: CoSelectOption[] = [
    "Annual review",
    "New facility (existing client)",
    "New facility (new client)",
    "Increase in existing facilities",
  ].map((v) => ({ value: v, label: v }));
  readonly ratingBandOptions: CoSelectOption[] = [
    "B1 and better",
    "B2 - B3",
    "B4 - B5",
    "CB and worse",
    "Not rated",
  ].map((v) => ({ value: v, label: v }));
  readonly heldViaOptions: CoSelectOption[] = HELD_VIA_OPTIONS.map((v) => ({ value: v, label: v }));
  readonly discussionTimeOptions: CoSelectOption<number>[] = [15, 30, 45, 60, 75, 90, 105, 120].map(
    (t) => ({ value: t, label: `${t} minutes` }),
  );
  readonly applicantRelationshipOptions: CoSelectOption[] = ["New Client", "Existing Client"].map(
    (v) => ({ value: v, label: v }),
  );
  readonly applicationTypeOptions: CoSelectOption[] = [
    "Complex",
    "Vanilla / well rated",
    "Annual review",
    "Other",
  ].map((v) => ({ value: v, label: v }));

  // ---- Booking state ----
  selectedAgcId: number | "" = "";
  readonly selectedCommittee = signal<AgendaCommittee | null>(null);
  /** yyyy-MM-dd from the date input; `meetingDate` is the equivalent JS Date. */
  meetingDateIso = "";
  readonly meetingDate = signal<Date | null>(null);
  readonly minBookingDate = this.toIsoDate(new Date());
  heldVia: string = HELD_VIA_OPTIONS[0];

  item: Partial<AgendaItem> = { withinPageGuidance: true, lateSubmission: false };
  manualCounterparty = false;
  selectedInkombaId: string | "" = "";
  /** DCP as typed; digits only are kept on save. */
  dcpInput = "";

  // Single-person pickers hold the selected person's key; item.* keeps the saved snapshot.
  primaryAnalystKey: string | "" = "";
  reviewAnalystKey: string | "" = "";
  creditSponsorKey: string | "" = "";
  buHeadKey: string | "" = "";

  // Multi-person lists: roster roles, teams, overlays and additional attendees.
  readonly peopleLists = signal<Record<PeopleListKey, Person[]>>({
    chair: [],
    independentMember: [],
    portfolioCce1: [],
    portfolioCce2: [],
    businessRep1: [],
    seniorBusinessRep: [],
    lecRep: [],
    eqMemberFrmExec: [],
    scribe: [],
    creditTeam: [],
    businessTeam: [],
    mezz: [],
    esg: [],
    additional: [],
  });
  readonly rosterPrefilled = signal(false);

  // Items already booked on the selected committee + date (session-load indicator).
  // Standard session assumed 08:00-12:00 (240 min) until business confirms per-committee lengths.
  readonly sessionItems = signal<AgendaItem[]>([]);
  readonly sessionMinutes = 240;

  // CASDM checks on counterparty selection.
  readonly creditApplication = signal<CreditApplicationSummary | null>(null);
  readonly applicationChecked = signal(false);
  readonly checkingApplication = signal(false);
  readonly casdmApp = signal<AgendaCasdmApplication | null>(null);
  readonly casdmChecked = signal(false);
  readonly checkingCasdm = signal(false);

  // Save + validation.
  readonly saving = signal(false);
  readonly saveError = signal("");
  readonly submitted = signal(false);
  fieldErrors: Partial<Record<RequiredField, string>> = {};

  // ---- Wizard ----
  readonly steps = [
    "Committee & Meeting",
    "Deal Details",
    "Team",
    "Classification",
    "Participants",
    "Review & Book",
  ];
  readonly step = signal(0);
  private readonly gatedSteps = [0, 1];

  ngOnInit(): void {
    this.session.ensure();
    this.load();

    this.cpTypeahead$
      .pipe(
        distinctUntilChanged(),
        debounceTime(300),
        tap(() => this.cpLoading.set(true)),
        switchMap((term) => {
          if (!term || term.trim().length < 2) return of(null);
          return this.agenda
            .searchCasdmCounterparties(term.trim(), false)
            .pipe(catchError(() => of(null)));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((res) => {
        this.counterparties.set(res?.counterpartyDetail ?? []);
        this.cpLoading.set(false);
      });

    this.userTypeahead$
      .pipe(
        distinctUntilChanged(),
        debounceTime(300),
        tap(() => this.userLoading.set(true)),
        switchMap((term) => {
          if (!term || term.trim().length < 2) return of<AgendaUser[]>([]);
          return this.agenda
            .searchAdUsers("RMB", term.trim())
            .pipe(catchError(() => of<AgendaUser[]>([])));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((users) => {
        this.adUsers.set((users ?? []).map((u) => this.toPerson(u)));
        this.userLoading.set(false);
      });
  }

  // ---- Loading ----

  load(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set("");
    this.agenda
      .getCommittees()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (committees) => {
          this.committees.set(committees ?? []);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(
            err?.status === 401 || err?.status === 403
              ? "DAF did not accept your session for the agenda module. Sign in again or ask for the credit analyst role."
              : "The DAF service did not answer. It may be down or unreachable from this network.",
          );
          this.loading.set(false);
        },
      });
  }

  // ---- Committee & date ----

  onCommitteeChange(agcId: number | "" | null): void {
    this.selectedCommittee.set(null);
    this.meetingDate.set(null);
    this.meetingDateIso = "";
    this.sessionItems.set([]);
    this.committeeMembers.set([]);
    this.committeeListName.set("");
    this.adUsers.set([]);
    const committee = this.committees().find((c) => c.agcId === agcId) ?? null;
    if (committee) {
      this.selectedCommittee.set(committee);
      this.prefillFromCommittee(committee);
      this.loadCommitteeMembers(committee.agcId);
    }
    if (this.submitted()) this.validate();
  }

  /** Predefined member pool: the List Management list linked to this committee. */
  private loadCommitteeMembers(agcId: number): void {
    this.agenda
      .getCommitteeMembers(agcId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (list) => {
          if (!list?.users) return;
          this.committeeListName.set(list.listManagementName ?? "");
          this.committeeMembers.set(
            list.users.map((u) => ({
              displayName: `${this.str(u["name"])} ${this.str(u["surname"])}`.trim(),
              userPrincipalName: this.str(u["email"]) || this.str(u["userId"]) || undefined,
              userId: this.str(u["userId"]) || undefined,
              fromCommitteeList: true,
            })),
          );
        },
        error: () => {
          // No list linked (or LMS unavailable): pickers fall back to plain AD search.
        },
      });
  }

  /** Roster defaults come from the committee record; only fills roles not yet set. */
  private prefillFromCommittee(committee: AgendaCommittee): void {
    this.rosterPrefilled.set(false);
    const setIfEmpty = (key: RosterKey, name?: string, id?: string) => {
      if (this.item[key] || !name) return;
      const people = this.splitNames(name);
      if (id && people.length === 1) people[0].userPrincipalName = id;
      this.peopleLists.update((lists) => ({ ...lists, [key]: people }));
      this.item[key] = name;
      this.rosterPrefilled.set(true);
    };
    setIfEmpty("chair", committee.chair, committee.chairId);
    setIfEmpty("scribe", committee.minuteTaker, committee.minuteTakerId);
  }

  onMeetingDateChange(): void {
    const date = this.parseIsoDate(this.meetingDateIso);
    this.meetingDate.set(date);
    this.loadSessionLoad();
    if (this.submitted()) this.validate();
  }

  /** Weekends are never bookable; any weekday can host an ad-hoc sitting. */
  isWeekend(date: Date | null): boolean {
    return !!date && (date.getDay() === 0 || date.getDay() === 6);
  }

  isBeforeToday(date: Date | null): boolean {
    if (!date) return false;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return date.getTime() < today.getTime();
  }

  isCommitteeDay(date: Date | null): boolean {
    const committee = this.selectedCommittee();
    if (!date || !committee?.meetingDay) return false;
    return DAY_NAMES[date.getDay()].toLowerCase() === committee.meetingDay.trim().toLowerCase();
  }

  dateHint(): string {
    const committee = this.selectedCommittee();
    const day = committee?.meetingDay ? `${committee.meetingDay}s` : "its usual day";
    return `The committee usually sits on ${day}, but any weekday can be selected for an ad-hoc sitting. Weekends are not selectable.`;
  }

  // ---- Session load ----

  private loadSessionLoad(): void {
    this.sessionItems.set([]);
    const committee = this.selectedCommittee();
    const date = this.meetingDate();
    if (!committee || !date) return;
    this.agenda
      .getAllItems()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (items) => {
          this.sessionItems.set(
            (items ?? []).filter(
              (i) =>
                i.itemStatus !== "Removed" &&
                i.committeeName === committee.committeeName &&
                i.mandateLevel === committee.mandateLevel &&
                this.sameDay(i.meetingDate, date),
            ),
          );
        },
        error: () => this.sessionItems.set([]),
      });
  }

  private sameDay(a: string | Date | null | undefined, b: Date | null): boolean {
    if (!a || !b) return false;
    const da = new Date(a);
    return (
      da.getFullYear() === b.getFullYear() &&
      da.getMonth() === b.getMonth() &&
      da.getDate() === b.getDate()
    );
  }

  bookedMinutes(): number {
    return this.sessionItems().reduce((total, i) => total + (i.estimatedMinutes || 0), 0);
  }

  remainingMinutes(): number {
    return this.sessionMinutes - this.bookedMinutes() - (this.item.estimatedMinutes || 0);
  }

  /** Hours once a duration reaches one ("2 hours 30 min"); minutes only below that. */
  formatDuration(minutes: number): string {
    const mins = Math.max(0, minutes || 0);
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    if (h === 0) return `${m} min`;
    const hours = `${h} ${h === 1 ? "hour" : "hours"}`;
    return m === 0 ? hours : `${hours} ${m} min`;
  }

  reservedSlots(): AgendaItem[] {
    return this.sessionItems()
      .filter((i) => i.startTime && i.endTime)
      .sort((a, b) => `${a.startTime}`.localeCompare(`${b.startTime}`));
  }

  // ---- Counterparty + CASDM checks ----

  onCounterpartyChange(inkombaId: string | "" | null): void {
    this.creditApplication.set(null);
    this.applicationChecked.set(false);
    this.casdmApp.set(null);
    this.casdmChecked.set(false);
    if (!inkombaId) {
      if (this.submitted()) this.validate();
      return;
    }
    this.item.inkombaId = inkombaId;
    const match = this.counterparties().find((c) => this.inkombaOf(c) === inkombaId);
    if (match) {
      this.selectedCounterparty.set(match);
      if (match.counterpartyName) this.item.counterpartyName = match.counterpartyName;
    }
    this.checkCreditApplication(inkombaId);
    this.checkCasdmApplication(inkombaId);
    if (this.submitted()) this.validate();
  }

  onManualCounterpartyChange(manual: boolean): void {
    this.manualCounterparty = manual;
    if (manual) {
      this.selectedInkombaId = "";
      this.selectedCounterparty.set(null);
      this.item.inkombaId = undefined;
    }
  }

  /** Direct CASDM REST check: the latest application's current status on CASDM itself. */
  private checkCasdmApplication(inkombaId: string): void {
    this.checkingCasdm.set(true);
    this.agenda
      .getCasdmApplication(inkombaId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (app) => {
          this.checkingCasdm.set(false);
          this.casdmChecked.set(true);
          this.casdmApp.set(app ?? null);
          this.prefillFromCasdm(app ?? null);
        },
        error: () => {
          this.checkingCasdm.set(false);
          this.casdmChecked.set(true);
          this.casdmApp.set(null);
        },
      });
  }

  /** Prefill from the live CASDM application; never overwrites what the analyst typed. */
  private prefillFromCasdm(app: AgendaCasdmApplication | null): void {
    if (!app?.found) return;
    if (!this.item.primaryAnalyst && app.creditAnalyst) {
      this.setAnalyst("primary", {
        displayName: app.creditAnalyst,
        userPrincipalName: app.creditAnalystEmail || undefined,
      });
    }
    if (!this.item.reviewAnalyst && app.reviewAnalyst) {
      this.setAnalyst("review", {
        displayName: app.reviewAnalyst,
        userPrincipalName: app.reviewAnalystEmail || undefined,
      });
    }
    if (!this.item.countryOfRisk && app.countryOfRisk) this.item.countryOfRisk = app.countryOfRisk;
    if (!this.item.purposeDetails && app.purposeOfSubmission) {
      this.item.purposeDetails = app.purposeOfSubmission;
    }
    const band = this.bandForRating(app.proposedRating);
    if (!this.item.ratingBand && band) this.item.ratingBand = band;
  }

  /** CASDM carries symbol ratings (B1, B2...); the agenda captures bands. */
  private bandForRating(symbol?: string): string | null {
    if (!symbol) return null;
    const s = symbol.trim().toUpperCase();
    if (s.startsWith("CB") || s.startsWith("C") || s.startsWith("D")) return "CB and worse";
    if (s === "B1" || s.startsWith("A") || s.endsWith("1")) return "B1 and better";
    if (s === "B2" || s === "B3") return "B2 - B3";
    if (s === "B4" || s === "B5") return "B4 - B5";
    return null;
  }

  /** WIP / application check: the in-flight CASDM credit application, pre-filling what we can. */
  private checkCreditApplication(inkombaId: string): void {
    this.checkingApplication.set(true);
    this.agenda
      .getCreditApplication(inkombaId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.checkingApplication.set(false);
          this.applicationChecked.set(true);
          const app = (res as { creditApplication?: CreditApplicationSummary } | null)
            ?.creditApplication;
          if (!app?.applicationId) return;
          this.creditApplication.set(app);
          if (this.item.dcpZar === undefined && app.proposedDcp) {
            this.item.dcpZar = app.proposedDcp;
            this.dcpInput = String(app.proposedDcp);
          }
          if (this.item.ratingBand === undefined && app.proposedFrRating) {
            this.item.ratingBand = app.proposedFrRating;
          }
          if (this.item.primaryAnalyst === undefined && app.creditAnalyst) {
            this.setAnalyst("primary", {
              displayName: app.creditAnalyst,
              userPrincipalName: app.creditAnalystId || app.creditAnalystEmail || undefined,
            });
          }
          if (this.item.reviewAnalyst === undefined && app.reviewAnalyst) {
            this.setAnalyst("review", {
              displayName: app.reviewAnalyst,
              userPrincipalName: app.reviewAnalystId || app.reviewAnalystEmail || undefined,
            });
          }
        },
        error: () => {
          this.checkingApplication.set(false);
          this.applicationChecked.set(true);
        },
      });
  }

  // ---- People ----

  userLabel(user: Person | null | undefined): string {
    return user?.displayName ?? "";
  }

  personKey(p: Person): string {
    return p.userPrincipalName || p.userId || p.displayName;
  }

  /** An AD user as /auth/users returns it: displayName, else name + surname, as DAF's page did. */
  private toPerson(u: AgendaUser): Person {
    const userId = this.str(u["userId"]) || u.samAccountName || undefined;
    return {
      displayName: u.displayName || `${this.str(u["name"])} ${this.str(u["surname"])}`.trim(),
      userPrincipalName: u.userPrincipalName || userId,
      userId,
      mail: u.mail || undefined,
    };
  }

  private str(value: unknown): string {
    return typeof value === "string" ? value : "";
  }

  /** Resolves a picker key and remembers the person so the option survives the next AD search. */
  private findPerson(key: string | "" | null): Person | null {
    if (!key) return null;
    const p = this.people().find((x) => this.personKey(x) === key) ?? null;
    if (p && !p.fromCommitteeList && !this.extraPeople().some((x) => this.personKey(x) === key)) {
      this.extraPeople.update((list) => [...list, p]);
    }
    return p;
  }

  /** `;`-joined captured names back to people (for prefilled rosters). */
  private splitNames(value: string): Person[] {
    return value
      .split(";")
      .map((s) => s.trim())
      .filter((s) => s !== "")
      .map((displayName) => ({ displayName }));
  }

  private setAnalyst(which: "primary" | "review", person: Person): void {
    if (!this.people().some((x) => this.personKey(x) === this.personKey(person))) {
      this.extraPeople.update((list) => [...list, person]);
    }
    if (which === "primary") {
      this.item.primaryAnalyst = person.displayName;
      this.item.primaryAnalystId = person.userPrincipalName || person.userId;
      this.primaryAnalystKey = this.personKey(person);
    } else {
      this.item.reviewAnalyst = person.displayName;
      this.item.reviewAnalystId = person.userPrincipalName || person.userId;
      this.reviewAnalystKey = this.personKey(person);
    }
  }

  onPrimaryAnalystChange(key: string | "" | null): void {
    const p = this.findPerson(key);
    this.item.primaryAnalyst = p ? p.displayName : undefined;
    this.item.primaryAnalystId = p ? p.userPrincipalName || p.userId : undefined;
  }

  onReviewAnalystChange(key: string | "" | null): void {
    const p = this.findPerson(key);
    this.item.reviewAnalyst = p ? p.displayName : undefined;
    this.item.reviewAnalystId = p ? p.userPrincipalName || p.userId : undefined;
  }

  onCreditSponsorChange(key: string | "" | null): void {
    const p = this.findPerson(key);
    if (!p) return;
    this.item.creditSponsor = p.displayName;
    this.item.creditSponsorId = p.userPrincipalName || p.userId;
  }

  onBuHeadChange(key: string | "" | null): void {
    const p = this.findPerson(key);
    if (!p) return;
    this.item.buHead = p.displayName;
    this.item.buHeadId = p.userPrincipalName || p.userId;
  }

  listOf(key: PeopleListKey): Person[] {
    return this.peopleLists()[key];
  }

  /** Adds the picked person to a multi-person list and clears the picker for the next pick. */
  addPerson(key: PeopleListKey, value: string | "" | null, picker: CoSearchableSelectComponent): void {
    const p = this.findPerson(value);
    if (!p) return;
    picker.select(null);
    const list = this.listOf(key);
    if (list.some((x) => this.personKey(x) === this.personKey(p))) return;
    this.setList(key, [...list, p]);
  }

  removePerson(key: PeopleListKey, person: Person): void {
    this.setList(
      key,
      this.listOf(key).filter((x) => this.personKey(x) !== this.personKey(person)),
    );
  }

  private setList(key: PeopleListKey, list: Person[]): void {
    this.peopleLists.update((lists) => ({ ...lists, [key]: list }));
    if (this.isRosterKey(key)) this.item[key] = this.joinNames(list, "; ") || undefined;
  }

  private isRosterKey(key: PeopleListKey): key is RosterKey {
    return this.roleGroups.some((g) => g.roles.some((r) => r.key === key));
  }

  joinNames(list: Person[], separator: string): string {
    return list.map((u) => this.userLabel(u)).join(separator);
  }

  namesOf(list: Person[]): string {
    return list.length === 0 ? "-" : this.joinNames(list, ", ");
  }

  initials(name: string): string {
    return name
      .split(" ")
      .filter((s) => s !== "")
      .slice(0, 2)
      .map((s) => s.charAt(0).toUpperCase())
      .join("");
  }

  // ---- Committee summary / quorum ----

  filledRoleCount(): number {
    return this.roleGroups.reduce(
      (n, g) => n + g.roles.filter((r) => !!this.item[r.key]).length,
      0,
    );
  }

  attendeeCount(): number {
    return this.listOf("mezz").length + this.listOf("esg").length + this.listOf("additional").length;
  }

  memberCount(): number {
    return this.filledRoleCount() + this.attendeeCount();
  }

  quorumTarget(): number {
    return this.selectedCommittee()?.quorumSize ?? 0;
  }

  quorumMet(): boolean {
    return this.quorumTarget() > 0 && this.memberCount() >= this.quorumTarget();
  }

  /** Every distinct person on the booking with the first role they hold. */
  participantChips(): ParticipantChip[] {
    const chips: ParticipantChip[] = [];
    const seen = new Set<string>();
    const push = (name: string | undefined, role: string) => {
      const n = (name ?? "").trim();
      if (n === "" || seen.has(n.toLowerCase())) return;
      seen.add(n.toLowerCase());
      chips.push({ name: n, role, initials: this.initials(n) });
    };
    this.roleGroups.forEach((g) =>
      g.roles.forEach((r) => (this.item[r.key] ?? "").split(";").forEach((n) => push(n, r.label))),
    );
    this.listOf("mezz").forEach((u) => push(this.userLabel(u), "Mezz / Overlay"));
    this.listOf("esg").forEach((u) => push(this.userLabel(u), "ESG Overlay"));
    this.listOf("additional").forEach((u) => push(this.userLabel(u), "Member"));
    return chips;
  }

  // ---- Wizard navigation ----

  stepComplete(i: number): boolean {
    switch (i) {
      case 0:
        return this.selectedCommittee() !== null && this.meetingDate() !== null;
      case 1:
        return !!this.item.counterpartyName && this.item.purposeOfSubmission !== undefined;
      case 2:
        return !!this.item.primaryAnalyst;
      case 3:
        return true;
      case 4:
        return this.memberCount() > 0;
      case 5:
        return this.canSave();
      default:
        return false;
    }
  }

  canNext(): boolean {
    return !this.gatedSteps.includes(this.step()) || this.stepComplete(this.step());
  }

  canGoTo(i: number): boolean {
    if (i <= this.step()) return true;
    return this.gatedSteps.filter((g) => g < i).every((g) => this.stepComplete(g));
  }

  stepTabs(): CoTabItem[] {
    return this.steps.map((label, i) => ({
      id: String(i),
      label: `${this.step() !== i && this.stepComplete(i) ? "✓" : i + 1}. ${label}`,
      disabled: !this.canGoTo(i),
    }));
  }

  goToStep(i: number): void {
    if (this.canGoTo(i)) {
      this.step.set(i);
      window.scrollTo(0, 0);
    }
  }

  onTabChange(id: string): void {
    this.goToStep(Number(id));
  }

  next(): void {
    if (this.step() < this.steps.length - 1 && this.canNext()) this.goToStep(this.step() + 1);
  }

  back(): void {
    if (this.step() > 0) this.goToStep(this.step() - 1);
  }

  /** "N% complete" reflects steps actually completed, not how far the user clicked. */
  completionPct(): number {
    return Math.round(
      (this.steps.filter((_, i) => this.stepComplete(i)).length * 100) / this.steps.length,
    );
  }

  discussionTimeSet(): boolean {
    return this.item.estimatedMinutes !== undefined && this.item.estimatedMinutes !== null;
  }

  readyToBook(): boolean {
    return this.canSave() && this.memberCount() > 0 && this.discussionTimeSet();
  }

  // ---- Validation + save ----

  canSave(): boolean {
    return (
      this.selectedCommittee() !== null &&
      this.meetingDate() !== null &&
      !this.isWeekend(this.meetingDate()) &&
      !this.isBeforeToday(this.meetingDate()) &&
      !!this.item.counterpartyName &&
      this.item.purposeOfSubmission !== undefined
    );
  }

  private validate(): RequiredField | undefined {
    const date = this.meetingDate();
    this.fieldErrors = {
      committee: this.selectedCommittee() ? "" : "Select the committee and mandate level.",
      meetingDate: !date
        ? "Select the proposed meeting date."
        : this.isWeekend(date)
          ? "Weekends are not selectable. Choose a weekday."
          : this.isBeforeToday(date)
            ? "The meeting date cannot be in the past."
            : "",
      counterpartyName: this.item.counterpartyName
        ? ""
        : this.manualCounterparty
          ? "Enter the counterparty or deal name."
          : "Select a counterparty, or tick Capture manually and type the name.",
      purposeOfSubmission: this.item.purposeOfSubmission ? "" : "Select the purpose of submission.",
    };
    return (["committee", "meetingDate", "counterpartyName", "purposeOfSubmission"] as const).find(
      (k) => this.fieldErrors[k],
    );
  }

  fieldError(field: RequiredField): string {
    return this.submitted() ? (this.fieldErrors[field] ?? "") : "";
  }

  private controlId(field: RequiredField): string {
    switch (field) {
      case "committee":
        return "daf-agenda-committee";
      case "meetingDate":
        return "daf-agenda-meeting-date";
      case "counterpartyName":
        return this.manualCounterparty ? "daf-agenda-counterparty-name" : "daf-agenda-counterparty";
      case "purposeOfSubmission":
        return "daf-agenda-purpose";
    }
  }

  save(): void {
    if (this.saving() || !this.canBook()) return;
    this.submitted.set(true);
    const firstInvalid = this.validate();
    if (firstInvalid) {
      this.goToStep(firstInvalid === "committee" || firstInvalid === "meetingDate" ? 0 : 1);
      const id = this.controlId(firstInvalid);
      setTimeout(() => document.getElementById(id)?.focus());
      return;
    }
    const committee = this.selectedCommittee();
    const meetingDate = this.meetingDate();
    if (!committee || !meetingDate) return;

    this.saving.set(true);
    this.saveError.set("");

    const digits = this.dcpInput.replace(/[^0-9.]/g, "");
    this.item.dcpZar = digits === "" ? null : Number(digits);

    this.item.creditTeam = this.joinNames(this.listOf("creditTeam"), "; ");
    this.item.businessTeam = this.joinNames(this.listOf("businessTeam"), "; ");
    this.item.mezzOverlayAttendees = this.joinNames(this.listOf("mezz"), "; ");
    this.item.esgOverlayAttendees = this.joinNames(this.listOf("esg"), "; ");
    this.item.additionalTeamAttendees = this.joinNames(this.listOf("additional"), "; ");

    // Resolved recipient ids/UPNs for the attendee invite (roster roles + analysts + teams).
    const emails: string[] = [];
    const add = (e?: string) => {
      if (e && !emails.includes(e)) emails.push(e);
    };
    const lists = this.peopleLists();
    (Object.keys(lists) as PeopleListKey[])
      .filter((k) => this.isRosterKey(k))
      .forEach((k) => lists[k].forEach((u) => add(u.userPrincipalName || u.userId)));
    add(this.item.primaryAnalystId);
    add(this.item.reviewAnalystId);
    add(this.item.creditSponsorId);
    add(this.item.buHeadId);
    (["creditTeam", "businessTeam", "mezz", "esg", "additional"] as const).forEach((k) =>
      lists[k].forEach((u) => add(u.userPrincipalName || u.userId)),
    );
    this.item.attendeeEmails = emails.join(";");

    // Participant records (BRS 6.4) from the roster pickers: one row per person per committee role,
    // quorum-eligible for the quorum roles, so the server evaluates quorum by role (RULE-005).
    const participants: AgendaParticipant[] = [];
    this.roleGroups.forEach((g) =>
      g.roles.forEach((r) =>
        lists[r.key].forEach((u) =>
          participants.push({
            personId: u.userPrincipalName || u.userId || null,
            personName: this.userLabel(u),
            email: u.mail || u.userPrincipalName || null,
            committeeRole: r.key.charAt(0).toUpperCase() + r.key.slice(1),
            attendanceRequired: true,
            quorumEligible: r.key !== "scribe",
            votingEligible: r.key !== "scribe",
            independentFlag: r.key === "independentMember",
            plannedAttendance: "Planned",
          }),
        ),
      ),
    );
    this.item.participants = participants;

    const request: BookAgendaItemRequest = {
      agcId: committee.agcId,
      meetingDate: meetingDate.toISOString(),
      heldVia: this.heldVia,
      item: this.item,
    };

    this.agenda
      .bookItem(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (created) => {
          this.saving.set(false);
          if (created?.agiId) this.navigation.navigate(`/agenda/item/${created.agiId}`);
          else this.navigation.navigate("/agenda/my");
        },
        error: (err) => {
          this.saving.set(false);
          this.saveError.set(
            err?.status === 403
              ? "Your account may not book agenda items; the slot was not booked."
              : err?.error?.message ||
                  "Failed to book the agenda item. Please try again or contact support.",
          );
        },
      });
  }

  cancel(): void {
    this.navigation.navigate("/agenda");
  }

  // ---- Helpers ----

  private inkombaOf(c: AgendaCounterparty): string {
    return c.creditInkombaId ?? c.inkombaId ?? "";
  }

  private toIsoDate(d: Date): string {
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");
    return `${d.getFullYear()}-${mm}-${dd}`;
  }

  private parseIsoDate(value: string): Date | null {
    const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value ?? "");
    if (!m) return null;
    return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
  }
}
