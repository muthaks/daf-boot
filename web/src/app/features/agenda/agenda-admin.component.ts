import {
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  OnInit,
  signal,
  TemplateRef,
  untracked,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { DatePipe, formatDate } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Dialog, DialogRef } from "@angular/cdk/dialog";
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  firstValueFrom,
  Observable,
  of,
  Subject,
  switchMap,
  tap,
} from "rxjs";
import { TableModule } from "primeng/table";
import {
  CO_DIALOG_CONFIG,
  CO_DIALOG_WIDE_CONFIG,
  CoActionGroupComponent,
  CoAlertComponent,
  CoBreadcrumb,
  CoButtonComponent,
  CoCheckboxComponent,
  CoCheckboxGroupComponent,
  CoConfirmService,
  CoContentStackComponent,
  CoDetailCardComponent,
  CoDialogLayoutComponent,
  CoEmptyStateComponent,
  CoErrorStateComponent,
  CoFieldComponent,
  CoFieldGridComponent,
  CoInputDirective,
  CoPageHeaderComponent,
  CoSearchableSelectComponent,
  CoSectionHeadingComponent,
  CoSelectOption,
  CoSpinnerComponent,
  CoStatusComponent,
  CoStatusKind,
  CoTabItem,
  CoTabsComponent,
} from "co-ds";
import {
  AgendaCommittee,
  AgendaMeeting,
  AgendaUser,
  DafTimestamp,
} from "../../core/models/agenda.models";
import { AgendaService } from "../../core/services/agenda.service";
import { DafNavigationService } from "../../core/services/daf-navigation.service";
import { PermissionsService } from "../../core/services/permissions.service";
import { SessionService } from "../../core/services/session.service";
import { getRemoteHost } from "../../remote/remote-host";
import { DisableUnlessDirective } from "../../shared/directives/disable-unless.directive";

type Tab = "committees" | "calendar";

// Option lists exactly as DAF's agenda-admin page defines them (DAF commit caf8b602).
const DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"];
/** This page's own mode spellings (DAF agenda-admin `modes`), not the booking page's HELD_VIA_OPTIONS. */
const MODES = ["Teams", "Hybrid", "Physical"];
const LEVELS = ["L1", "L2", "L3", "L4"];
/** Committee roles a participant can hold (BRS 6.4); the quorum roles are picked from these. */
const COMMITTEE_ROLES = [
  "Chair",
  "IndependentMember",
  "PortfolioCce1",
  "PortfolioCce2",
  "BusinessRep1",
  "SeniorBusinessRep",
  "LecRep",
  "EqMemberFrmExec",
  "Scribe",
  "Secretary",
  "Member",
];
const ADMIN_DOCUMENT_TYPES = ["Credit Paper", "Annexure", "Supporting Document", "Other"];
/** DAF stores the multi-value committee settings `;`-joined. */
const SEPARATOR = ";";

type StandingRoleKey = "chair" | "altChair" | "meetingSecretary" | "minuteTaker" | "administrator";
type StandingRoleIdKey = `${StandingRoleKey}Id`;
const STANDING_ROLES: readonly { key: StandingRoleKey; label: string }[] = [
  { key: "chair", label: "Chair" },
  { key: "altChair", label: "Alternate chair" },
  { key: "meetingSecretary", label: "Meeting secretary" },
  { key: "minuteTaker", label: "Minute taker" },
  { key: "administrator", label: "Administrator" },
];

/** Meeting statuses DAF no longer lets the secretariat edit or cancel. */
const FINISHED_STATUSES = ["Cancelled", "Held", "Closed"];

interface CommitteeForm {
  committeeName: string;
  mandateLevel: string;
  levelDescription: string;
  quorumSize: number | null;
  pageGuidance: number | null;
  meetingDay: string;
  defaultHeldVia: string;
  defaultDurationMinutes: number | null;
  cutOffDayOfWeek: string;
  cutOffTime: string;
  effectiveFrom: string;
  effectiveTo: string;
  listName: string;
  approverComposition: string;
}
type CommitteeErrorKey = "committeeName" | "mandateLevel" | "quorumSize";

interface MeetingForm {
  agcId: string;
  meetingDate: string;
  startTime: string;
  endTime: string;
  heldVia: string;
  venue: string;
  teamsJoinUrl: string;
}
type MeetingErrorKey = "agcId" | "meetingDate";

/** A person picked for a standing role: DAF stores the display name and the UPN. */
interface Person {
  name: string;
  upn: string | null;
}

/**
 * DAF accepts null to clear a value; the shared model types only declare the present case,
 * so request bodies are built with this shape and passed through `unknown`.
 */
type CommitteePayload = { [K in keyof AgendaCommittee]?: AgendaCommittee[K] | null };
type MeetingPayload = { [K in keyof AgendaMeeting]?: AgendaMeeting[K] | null };

/**
 * Committee & calendar administration for the secretariat (DAF `agenda-admin.component`, BRS
 * BR-001 / BR-002, RULE-004, RULE-007): maintain committees (levels, quorum, meeting day, cut-off,
 * mandatory papers, quorum roles, effective dates) and schedule, move or cancel meetings.
 */
@Component({
  selector: "app-agenda-admin",
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    TableModule,
    CoActionGroupComponent,
    CoAlertComponent,
    CoButtonComponent,
    CoCheckboxComponent,
    CoCheckboxGroupComponent,
    CoContentStackComponent,
    CoDetailCardComponent,
    CoDialogLayoutComponent,
    CoEmptyStateComponent,
    CoErrorStateComponent,
    CoFieldComponent,
    CoFieldGridComponent,
    CoInputDirective,
    CoPageHeaderComponent,
    CoSearchableSelectComponent,
    CoSectionHeadingComponent,
    CoSpinnerComponent,
    CoStatusComponent,
    CoTabsComponent,
    DisableUnlessDirective,
  ],
  templateUrl: "./agenda-admin.component.html",
  host: { "data-co-daf": "", "data-co-pattern": "daf-agenda-admin" },
})
export class AgendaAdminComponent implements OnInit {
  private readonly agenda = inject(AgendaService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly confirms = inject(CoConfirmService);
  private readonly dialog = inject(Dialog);
  private readonly permissions = inject(PermissionsService);
  private dialogRef?: DialogRef<void>;
  readonly navigation = inject(DafNavigationService);
  readonly session = inject(SessionService);
  readonly hosted = !!getRemoteHost();
  readonly breadcrumbs: CoBreadcrumb[] = [
    { label: "Committee Agendas", route: this.navigation.href("/home") },
    { label: "Committee administration" },
  ];

  readonly tabs: CoTabItem[] = [
    { id: "committees", label: "Committees" },
    { id: "calendar", label: "Meeting calendar" },
  ];
  readonly committeeRoles = COMMITTEE_ROLES;
  readonly documentTypes = ADMIN_DOCUMENT_TYPES;
  readonly standingRoles = STANDING_ROLES;
  readonly levelOptions: CoSelectOption[] = LEVELS.map((l) => ({ value: l, label: l }));

  readonly canAdmin = computed(() => this.permissions.can("agenda.admin"));
  private started = false;

  // ---- Page state
  readonly tab = signal<Tab>("committees");
  readonly message = signal("");

  // ---- Committees
  readonly committees = signal<AgendaCommittee[]>([]);
  readonly committeesLoading = signal(false);
  readonly committeesError = signal("");
  readonly availableLists = signal<string[]>([]);
  readonly committeeFirst = signal(0);

  readonly listOptions = computed<CoSelectOption[]>(() => [
    { value: "", label: "None - capture people one by one" },
    ...this.availableLists().map((name) => ({ value: name, label: name })),
  ]);
  readonly meetingCommitteeOptions = computed<CoSelectOption[]>(() =>
    this.committees().map((c) => ({
      value: String(c.agcId),
      label: `${c.committeeName} ${c.mandateLevel}`,
      disabled: !c.active,
      subtitle: c.active ? undefined : "Inactive",
    })),
  );

  // ---- Committee dialog
  /** The committee being edited; null while creating a new one. */
  readonly editingCommittee = signal<AgendaCommittee | null>(null);
  readonly savingCommittee = signal(false);
  readonly committeeSaveError = signal("");
  committeeForm: CommitteeForm = this.newCommitteeForm();
  committeeErrors: Partial<Record<CommitteeErrorKey, string>> = {};
  quorumRoleSelection: Record<string, boolean> = {};
  docTypeSelection: Record<string, boolean> = {};
  dayOptions: CoSelectOption[] = [];
  cutOffDayOptions: CoSelectOption[] = [];
  defaultModeOptions: CoSelectOption[] = [];

  // ---- Standing-role people pickers: live RMB AD search, as DAF's page (debounced, min 2 chars)
  readonly userSearch$ = new Subject<string>();
  readonly usersLoading = signal(false);
  private readonly userResults = signal<CoSelectOption[]>([]);
  private readonly personByValue = new Map<string, Person>();
  readonly personOptions = signal<Record<StandingRoleKey, CoSelectOption[]>>(emptyRoleRecord(() => []));
  people: Record<StandingRoleKey, string> = emptyRoleRecord(() => "");
  /** The picker values the dialog opened with, so an untouched role keeps its stored name and id. */
  private peopleSeed: Record<StandingRoleKey, string> = emptyRoleRecord(() => "");

  // ---- Meeting calendar
  readonly meetings = signal<AgendaMeeting[]>([]);
  readonly meetingsLoading = signal(false);
  readonly meetingsError = signal("");
  readonly meetingFirst = signal(0);
  from = isoDate(new Date(Date.now() - 14 * 86400000));
  to = isoDate(new Date(Date.now() + 90 * 86400000));

  // ---- Meeting dialogs
  /** The meeting being edited; null while scheduling a new one. */
  readonly editingMeeting = signal<AgendaMeeting | null>(null);
  readonly savingMeeting = signal(false);
  readonly meetingSaveError = signal("");
  meetingForm: MeetingForm = this.newMeetingForm();
  meetingErrors: Partial<Record<MeetingErrorKey, string>> = {};
  meetingModeOptions: CoSelectOption[] = [];

  readonly cancellingMeeting = signal<AgendaMeeting | null>(null);
  readonly cancelling = signal(false);
  readonly cancelError = signal("");
  cancelReason = "";
  cancelReasonError = "";

  constructor() {
    this.destroyRef.onDestroy(() => this.dialogRef?.close());

    // The data is secretariat-only: load it once the session shows the role.
    effect(() => {
      if (this.canAdmin() && !this.started) {
        untracked(() => this.start());
      }
    });

    this.userSearch$
      .pipe(
        distinctUntilChanged(),
        debounceTime(300),
        tap(() => this.usersLoading.set(true)),
        switchMap((term) => {
          const q = (term ?? "").trim();
          if (q.length < 2) return of<AgendaUser[]>([]);
          return this.agenda.searchAdUsers("RMB", q).pipe(catchError(() => of<AgendaUser[]>([])));
        }),
        takeUntilDestroyed(),
      )
      .subscribe((found) => {
        const options: CoSelectOption[] = [];
        for (const user of found ?? []) {
          const name = userLabel(user);
          if (!name) continue;
          const upn = userId(user);
          const value = upn ?? name;
          if (options.some((o) => o.value === value)) continue;
          this.personByValue.set(value, { name, upn });
          options.push(personOption(value, { name, upn }));
        }
        this.userResults.set(options);
        this.usersLoading.set(false);
        this.refreshPersonOptions();
      });
  }

  ngOnInit(): void {
    this.session.ensure();
  }

  private start(): void {
    this.started = true;
    this.loadCommittees();
    this.agenda
      .getAvailableLists()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (names) => this.availableLists.set(names ?? []),
        error: () => this.availableLists.set([]),
      });
  }

  setTab(id: string): void {
    const next: Tab = id === "calendar" ? "calendar" : "committees";
    this.tab.set(next);
    this.message.set("");
    if (next === "calendar") this.loadMeetings();
  }

  // ---- Committees ----------------------------------------------------------------------------

  loadCommittees(): void {
    if (this.committeesLoading()) return;
    this.committeesLoading.set(true);
    this.committeesError.set("");
    this.agenda
      .getAllCommittees()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (committees) => {
          this.committees.set(committees ?? []);
          this.committeesLoading.set(false);
        },
        error: (err: unknown) => {
          this.committeesError.set(apiMessage(err, "Failed to load committees."));
          this.committeesLoading.set(false);
        },
      });
  }

  openNewCommittee(template: TemplateRef<unknown>): void {
    if (this.dialogRef || !this.canAdmin()) return;
    this.editingCommittee.set(null);
    this.committeeForm = this.newCommitteeForm();
    this.seedCommitteeSelections(
      "Chair;IndependentMember;PortfolioCce1;PortfolioCce2;BusinessRep1;SeniorBusinessRep",
      "Credit Paper",
      null,
    );
    this.openCommitteeDialog(template);
  }

  openEditCommittee(template: TemplateRef<unknown>, committee: AgendaCommittee): void {
    if (this.dialogRef || !this.canAdmin()) return;
    this.editingCommittee.set(committee);
    this.committeeForm = {
      committeeName: committee.committeeName ?? "",
      mandateLevel: committee.mandateLevel ?? "",
      levelDescription: committee.levelDescription ?? "",
      quorumSize: committee.quorumSize ?? null,
      pageGuidance: committee.pageGuidance ?? null,
      meetingDay: committee.meetingDay ?? "",
      defaultHeldVia: committee.defaultHeldVia ?? "",
      defaultDurationMinutes: committee.defaultDurationMinutes ?? null,
      cutOffDayOfWeek: committee.cutOffDayOfWeek ?? "",
      cutOffTime: committee.cutOffTime ?? "",
      effectiveFrom: dateInputValue(committee.effectiveFrom),
      effectiveTo: dateInputValue(committee.effectiveTo),
      listName: committee.listName ?? "",
      approverComposition: committee.approverComposition ?? "",
    };
    this.seedCommitteeSelections(committee.quorumRoles, committee.requiredDocTypes, committee);
    this.openCommitteeDialog(template);
  }

  private openCommitteeDialog(template: TemplateRef<unknown>): void {
    const f = this.committeeForm;
    this.dayOptions = withCurrent(DAYS, f.meetingDay);
    this.cutOffDayOptions = withCurrent(DAYS, f.cutOffDayOfWeek);
    this.defaultModeOptions = withCurrent(MODES, f.defaultHeldVia);
    this.committeeErrors = {};
    this.committeeSaveError.set("");
    this.message.set("");
    this.dialogRef = this.dialog.open<void>(template, {
      ...CO_DIALOG_WIDE_CONFIG,
      ariaLabelledBy: "daf-agenda-admin-committee-title",
    });
    this.dialogRef.closed.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.dialogRef = undefined;
    });
  }

  private seedCommitteeSelections(
    quorumRoles: string | undefined | null,
    requiredDocTypes: string | undefined | null,
    committee: AgendaCommittee | null,
  ): void {
    this.quorumRoleSelection = {};
    splitList(quorumRoles).forEach((r) => (this.quorumRoleSelection[r] = true));
    this.docTypeSelection = {};
    splitList(requiredDocTypes).forEach((t) => (this.docTypeSelection[t] = true));
    const people = emptyRoleRecord(() => "");
    for (const { key } of STANDING_ROLES) {
      const name = committee?.[key];
      if (!name) continue;
      const upn = committee?.[`${key}Id` as StandingRoleIdKey] || null;
      const value = upn ?? name;
      this.personByValue.set(value, { name, upn });
      people[key] = value;
    }
    this.people = people;
    this.peopleSeed = { ...people };
    this.refreshPersonOptions();
  }

  onPersonChange(): void {
    this.refreshPersonOptions();
  }

  /** Search results plus each role's current pick, so a pick stays visible after a new search. */
  private refreshPersonOptions(): void {
    const results = this.userResults();
    const next = emptyRoleRecord<CoSelectOption[]>(() => []);
    for (const { key } of STANDING_ROLES) {
      const value = this.people[key];
      const options = [...results];
      const person = value ? this.personByValue.get(value) : undefined;
      if (value && person && !options.some((o) => o.value === value)) {
        options.unshift(personOption(value, person));
      }
      next[key] = options;
    }
    this.personOptions.set(next);
  }

  toggleQuorumRole(role: string, checked: boolean): void {
    this.quorumRoleSelection = { ...this.quorumRoleSelection, [role]: checked };
  }

  toggleDocType(type: string, checked: boolean): void {
    this.docTypeSelection = { ...this.docTypeSelection, [type]: checked };
  }

  closeCommitteeDialog(): void {
    if (!this.savingCommittee()) this.dialogRef?.close();
  }

  private validateCommittee(): CommitteeErrorKey | undefined {
    const f = this.committeeForm;
    this.committeeErrors = {
      committeeName: f.committeeName.trim() ? "" : "Enter the committee name.",
      mandateLevel: f.mandateLevel ? "" : "Select the mandate level.",
      quorumSize:
        f.quorumSize !== null && f.quorumSize >= 1 ? "" : "Enter a quorum size of at least 1.",
    };
    const order: CommitteeErrorKey[] = ["committeeName", "mandateLevel", "quorumSize"];
    return order.find((k) => this.committeeErrors[k]);
  }

  saveCommittee(): void {
    if (this.savingCommittee() || !this.canAdmin()) return;
    const firstInvalid = this.validateCommittee();
    if (firstInvalid) {
      setTimeout(() => document.getElementById(`daf-agc-${firstInvalid}`)?.focus());
      return;
    }
    const original = this.editingCommittee();
    const payload = this.committeePayload(original);
    this.savingCommittee.set(true);
    this.committeeSaveError.set("");
    if (this.dialogRef) this.dialogRef.disableClose = true;
    const request =
      original === null
        ? this.agenda.createCommittee(payload as unknown as Partial<AgendaCommittee>)
        : this.agenda.updateCommittee(payload as unknown as AgendaCommittee);
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.savingCommittee.set(false);
        if (this.dialogRef) this.dialogRef.disableClose = false;
        this.dialogRef?.close();
        this.message.set("Committee saved.");
        this.loadCommittees();
      },
      error: (err: unknown) => {
        this.savingCommittee.set(false);
        if (this.dialogRef) this.dialogRef.disableClose = false;
        this.committeeSaveError.set(apiMessage(err, "Failed to save the committee."));
      },
    });
  }

  /** The body DAF's page sends: the whole committee with the edited fields applied. */
  private committeePayload(original: AgendaCommittee | null): CommitteePayload {
    const f = this.committeeForm;
    const payload: CommitteePayload = {
      ...(original ?? { active: true }),
      committeeName: f.committeeName,
      mandateLevel: f.mandateLevel,
      levelDescription: f.levelDescription,
      quorumSize: f.quorumSize ?? undefined,
      pageGuidance: f.pageGuidance,
      meetingDay: f.meetingDay,
      defaultHeldVia: f.defaultHeldVia || null,
      defaultDurationMinutes: f.defaultDurationMinutes,
      cutOffDayOfWeek: f.cutOffDayOfWeek,
      cutOffTime: f.cutOffTime,
      requiredDocTypes: ADMIN_DOCUMENT_TYPES.filter((t) => this.docTypeSelection[t]).join(SEPARATOR),
      quorumRoles: COMMITTEE_ROLES.filter((r) => this.quorumRoleSelection[r]).join(SEPARATOR),
      effectiveFrom: f.effectiveFrom || null,
      effectiveTo: f.effectiveTo || null,
      listName: f.listName || null,
      approverComposition: f.approverComposition,
    };
    for (const { key } of STANDING_ROLES) {
      const value = this.people[key];
      if (value === this.peopleSeed[key]) continue;
      const idKey = `${key}Id` as StandingRoleIdKey;
      const person = value ? this.personByValue.get(value) : undefined;
      payload[key] = person ? person.name : null;
      payload[idKey] = person ? person.upn : null;
    }
    return payload;
  }

  deactivateCommittee(committee: AgendaCommittee): void {
    if (!committee.active || !this.canAdmin()) return;
    this.message.set("");
    this.confirms
      .confirm({
        heading: "Deactivate committee",
        message: `Deactivate ${committee.committeeName} ${committee.mandateLevel}? No new meetings can be scheduled for it.`,
        confirmLabel: "Deactivate",
        pendingLabel: "Deactivating…",
        variant: "danger",
        errorMessage: "Failed to deactivate the committee.",
        action: async () => {
          try {
            await firstValueFrom(this.agenda.deactivateCommittee(committee.agcId));
          } catch (err: unknown) {
            throw new Error(apiMessage(err, "Failed to deactivate the committee."));
          }
        },
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((done) => {
        if (!done) return;
        this.message.set("Committee deactivated.");
        this.loadCommittees();
      });
  }

  // ---- Meeting calendar ----------------------------------------------------------------------

  loadMeetings(): void {
    if (this.meetingsLoading() || !this.from || !this.to) return;
    this.meetingsLoading.set(true);
    this.meetingsError.set("");
    this.agenda
      .getMeetingsBetween(this.from, this.to)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (meetings) => {
          this.meetings.set(meetings ?? []);
          this.meetingFirst.set(0);
          this.meetingsLoading.set(false);
        },
        error: (err: unknown) => {
          this.meetingsError.set(apiMessage(err, "Failed to load meetings."));
          this.meetingsLoading.set(false);
        },
      });
  }

  canChangeMeeting(meeting: AgendaMeeting): boolean {
    return !FINISHED_STATUSES.includes(meeting.status);
  }

  openNewMeeting(template: TemplateRef<unknown>): void {
    if (this.dialogRef || !this.canAdmin()) return;
    this.editingMeeting.set(null);
    this.meetingForm = this.newMeetingForm();
    this.openMeetingDialog(template);
  }

  openEditMeeting(template: TemplateRef<unknown>, meeting: AgendaMeeting): void {
    if (this.dialogRef || !this.canAdmin() || !this.canChangeMeeting(meeting)) return;
    this.editingMeeting.set(meeting);
    this.meetingForm = {
      agcId: String(meeting.agcId),
      meetingDate: dateInputValue(meeting.meetingDate),
      startTime: meeting.startTime ?? "",
      endTime: meeting.endTime ?? "",
      heldVia: meeting.heldVia ?? "",
      venue: meeting.venue ?? "",
      teamsJoinUrl: meeting.teamsJoinUrl ?? "",
    };
    this.openMeetingDialog(template);
  }

  private openMeetingDialog(template: TemplateRef<unknown>): void {
    this.meetingModeOptions = withCurrent(MODES, this.meetingForm.heldVia);
    this.meetingErrors = {};
    this.meetingSaveError.set("");
    this.message.set("");
    this.dialogRef = this.dialog.open<void>(template, {
      ...CO_DIALOG_CONFIG,
      ariaLabelledBy: "daf-agenda-admin-meeting-title",
    });
    this.dialogRef.closed.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.dialogRef = undefined;
    });
  }

  /** Picking a committee takes its default mode, as DAF's page does. */
  onMeetingCommitteeChange(agcId: string): void {
    const committee = this.committees().find((c) => c.agcId === Number(agcId));
    if (committee) {
      this.meetingForm.heldVia = committee.defaultHeldVia || this.meetingForm.heldVia;
      this.meetingModeOptions = withCurrent(MODES, this.meetingForm.heldVia);
    }
  }

  closeMeetingDialog(): void {
    if (!this.savingMeeting() && !this.cancelling()) this.dialogRef?.close();
  }

  private validateMeeting(isNew: boolean): MeetingErrorKey | undefined {
    const f = this.meetingForm;
    this.meetingErrors = {
      agcId: !isNew || f.agcId ? "" : "Select the committee and level.",
      meetingDate: f.meetingDate ? "" : "Choose the meeting date.",
    };
    const order: MeetingErrorKey[] = ["agcId", "meetingDate"];
    return order.find((k) => this.meetingErrors[k]);
  }

  saveMeeting(): void {
    if (this.savingMeeting() || !this.canAdmin()) return;
    const original = this.editingMeeting();
    const firstInvalid = this.validateMeeting(original === null);
    if (firstInvalid) {
      setTimeout(() => document.getElementById(`daf-agm-${firstInvalid}`)?.focus());
      return;
    }
    const f = this.meetingForm;
    this.savingMeeting.set(true);
    this.meetingSaveError.set("");
    if (this.dialogRef) this.dialogRef.disableClose = true;

    let request: Observable<AgendaMeeting>;
    let done: string;
    let fallback: string;
    if (original === null) {
      const payload: MeetingPayload = {
        agcId: Number(f.agcId),
        meetingDate: f.meetingDate + "T00:00:00",
        startTime: f.startTime,
        endTime: f.endTime || null,
        heldVia: f.heldVia,
        venue: f.venue || null,
        teamsJoinUrl: f.teamsJoinUrl || null,
      };
      request = this.agenda.createMeeting(payload as unknown as Partial<AgendaMeeting>);
      done = "Meeting scheduled.";
      fallback = "Failed to schedule the meeting.";
    } else {
      const payload: MeetingPayload = {
        ...original,
        meetingDate: f.meetingDate + "T00:00:00",
        startTime: f.startTime,
        endTime: f.endTime,
        heldVia: f.heldVia,
        venue: f.venue,
        teamsJoinUrl: f.teamsJoinUrl,
      };
      request = this.agenda.updateMeeting(original.agmId, payload as unknown as Partial<AgendaMeeting>);
      done = "Meeting updated.";
      fallback = "Failed to update the meeting.";
    }

    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.savingMeeting.set(false);
        if (this.dialogRef) this.dialogRef.disableClose = false;
        this.dialogRef?.close();
        this.message.set(done);
        this.loadMeetings();
      },
      error: (err: unknown) => {
        this.savingMeeting.set(false);
        if (this.dialogRef) this.dialogRef.disableClose = false;
        this.meetingSaveError.set(apiMessage(err, fallback));
      },
    });
  }

  openCancelMeeting(template: TemplateRef<unknown>, meeting: AgendaMeeting): void {
    if (this.dialogRef || !this.canAdmin() || !this.canChangeMeeting(meeting)) return;
    this.cancellingMeeting.set(meeting);
    this.cancelReason = "";
    this.cancelReasonError = "";
    this.cancelError.set("");
    this.message.set("");
    this.dialogRef = this.dialog.open<void>(template, {
      ...CO_DIALOG_CONFIG,
      ariaLabelledBy: "daf-agenda-admin-cancel-title",
    });
    this.dialogRef.closed.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.dialogRef = undefined;
    });
  }

  closeCancelDialog(): void {
    if (!this.cancelling()) this.dialogRef?.close();
  }

  confirmCancelMeeting(): void {
    const meeting = this.cancellingMeeting();
    if (!meeting || this.cancelling() || !this.canAdmin()) return;
    const reason = this.cancelReason.trim();
    this.cancelReasonError = reason ? "" : "Give the reason the meeting is being cancelled.";
    if (!reason) {
      document.getElementById("daf-agm-cancelReason")?.focus();
      return;
    }
    this.cancelError.set("");
    this.confirms
      .confirm({
        heading: "Cancel meeting",
        message: "Cancel this meeting? Attendees are notified and the Teams meeting (if any) is cancelled.",
        confirmLabel: "Cancel meeting",
        cancelLabel: "Keep meeting",
        pendingLabel: "Cancelling…",
        variant: "danger",
        errorMessage: "Failed to cancel the meeting.",
        action: async () => {
          this.cancelling.set(true);
          if (this.dialogRef) this.dialogRef.disableClose = true;
          try {
            await firstValueFrom(this.agenda.cancelMeeting(meeting.agmId, reason));
          } catch (err: unknown) {
            const text = apiMessage(err, "Failed to cancel the meeting.");
            this.cancelError.set(text);
            throw new Error(text);
          } finally {
            this.cancelling.set(false);
            if (this.dialogRef) this.dialogRef.disableClose = false;
          }
        },
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((cancelled) => {
        if (!cancelled) return;
        this.cancelError.set("");
        this.dialogRef?.close();
        this.message.set("Meeting cancelled.");
        this.loadMeetings();
      });
  }

  meetingStatusKind(status: string | undefined): CoStatusKind {
    switch (status) {
      case "Cancelled":
        return "blocked";
      case "Locked":
        return "active";
      case "Held":
      case "Closed":
      case "Final":
      case "Completed":
        return "neutral";
      default:
        return "info";
    }
  }

  openMeeting(meeting: AgendaMeeting): void {
    this.navigation.navigate(`/agenda/meeting/${meeting.agmId}`);
  }

  back(): void {
    this.navigation.navigate("/agenda/dashboard");
  }

  // ---- Defaults ------------------------------------------------------------------------------

  /** DAF's `startNew` defaults. */
  private newCommitteeForm(): CommitteeForm {
    return {
      committeeName: "",
      mandateLevel: "L3",
      levelDescription: "",
      quorumSize: 5,
      pageGuidance: null,
      meetingDay: "Wednesday",
      defaultHeldVia: "Teams",
      defaultDurationMinutes: 240,
      cutOffDayOfWeek: "Thursday",
      cutOffTime: "17:00",
      effectiveFrom: "",
      effectiveTo: "",
      listName: "",
      approverComposition: "",
    };
  }

  /** DAF's `resetNewMeeting` defaults. */
  private newMeetingForm(): MeetingForm {
    return {
      agcId: "",
      meetingDate: "",
      startTime: "08:00",
      endTime: "",
      heldVia: "Teams",
      venue: "",
      teamsJoinUrl: "",
    };
  }
}

function emptyRoleRecord<T>(make: () => T): Record<StandingRoleKey, T> {
  return {
    chair: make(),
    altChair: make(),
    meetingSecretary: make(),
    minuteTaker: make(),
    administrator: make(),
  };
}

function splitList(value: string | undefined | null): string[] {
  return String(value ?? "")
    .split(SEPARATOR)
    .map((v) => v.trim())
    .filter((v) => v !== "");
}

/** Options for a fixed list, keeping a stored value that is not in it so it still shows. */
function withCurrent(values: readonly string[], current: string): CoSelectOption[] {
  const options = values.map((v) => ({ value: v, label: v }));
  if (current && !values.includes(current)) options.unshift({ value: current, label: current });
  return options;
}

function personOption(value: string, person: Person): CoSelectOption {
  return {
    value,
    label: person.name,
    subtitle: person.upn && person.upn !== person.name ? person.upn : undefined,
  };
}

function text(value: unknown): string {
  return typeof value === "string" ? value : "";
}

/** An AD user as /auth/users returns it: displayName, else name + surname, as DAF's page did. */
function userLabel(user: AgendaUser): string {
  return (user.displayName || `${text(user["name"])} ${text(user["surname"])}`).trim();
}

/** DAF stores the UPN, else the user id. */
function userId(user: AgendaUser): string | null {
  return user.userPrincipalName || text(user["userId"]) || null;
}

/** yyyy-MM-dd, the format DAF's page sends for the calendar range (UTC, via toISOString). */
function isoDate(d: Date): string {
  return d.toISOString().substring(0, 10);
}

/** A DAF timestamp (epoch millis, or an ISO string) as a native date-input value. */
function dateInputValue(value: DafTimestamp | null | undefined): string {
  if (value === null || value === undefined || value === "") return "";
  if (typeof value === "number") return formatDate(value, "yyyy-MM-dd", "en-US");
  return String(value).substring(0, 10);
}

/** DAF's error body carries `message`; fall back to the page's own wording. */
function apiMessage(err: unknown, fallback: string): string {
  const body =
    typeof err === "object" && err !== null && "error" in err
      ? (err as { error?: { message?: unknown } }).error
      : undefined;
  return body && typeof body.message === "string" && body.message ? body.message : fallback;
}
