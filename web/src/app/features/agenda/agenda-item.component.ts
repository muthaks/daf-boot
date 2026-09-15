import {
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal,
  TemplateRef,
  viewChild,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { Dialog, DialogRef } from "@angular/cdk/dialog";
import { DatePipe, formatDate } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { TableModule } from "primeng/table";
import { catchError, debounceTime, distinctUntilChanged, firstValueFrom, Observable, of, Subject, switchMap, tap } from "rxjs";
import {
  CO_DIALOG_CONFIG,
  CoActionGroupComponent,
  CoAlertComponent,
  CoBreadcrumb,
  CoButtonComponent,
  CoCheckboxComponent,
  CoConfirmService,
  CoContentStackComponent,
  CoDetailCardComponent,
  CoDetailFieldComponent,
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
  CoStatCardComponent,
  CoStatusComponent,
  CoStatusKind,
  CoTabItem,
  CoTabsComponent,
} from "co-ds";
import {
  AgendaCondition,
  AgendaItem,
  AgendaItemDocument,
  AgendaMeeting,
  AgendaNotification,
  AgendaParticipant,
  AgendaPostMeeting,
  DafTimestamp,
  DECISION_OPTIONS,
} from "../../core/models/agenda.models";
import { AgendaService } from "../../core/services/agenda.service";
import { DafNavigationService } from "../../core/services/daf-navigation.service";
import { PermissionsService } from "../../core/services/permissions.service";
import { SessionService } from "../../core/services/session.service";
import { getRemoteHost } from "../../remote/remote-host";
import { CanDirective } from "../../shared/directives/can.directive";

type Tab =
  | "details"
  | "team"
  | "documents"
  | "workflow"
  | "post-meeting"
  | "conditions"
  | "notifications";

/** Single-person fields that also persist a UPN alongside the display name. */
const SINGLE_PERSON_FIELDS = ["primaryAnalyst", "reviewAnalyst", "creditSponsor", "buHead"] as const;
type SinglePersonField = (typeof SINGLE_PERSON_FIELDS)[number];
type PersonIdField = "primaryAnalystId" | "reviewAnalystId" | "creditSponsorId" | "buHeadId";
const PERSON_ID_FIELD: Record<SinglePersonField, PersonIdField> = {
  primaryAnalyst: "primaryAnalystId",
  reviewAnalyst: "reviewAnalystId",
  creditSponsor: "creditSponsorId",
  buHead: "buHeadId",
};

/** Quorum roster roles (multi-select, `;`-joined on the item), same template as the booking form. */
type RosterField =
  | "chair"
  | "independentMember"
  | "portfolioCce1"
  | "portfolioCce2"
  | "businessRep1"
  | "seniorBusinessRep"
  | "lecRep"
  | "eqMemberFrmExec"
  | "scribe";
/** The `;`-joined team and overlay fields, also edited as multi-select people pickers. */
const TEAM_FIELDS = [
  "creditTeam",
  "businessTeam",
  "mezzOverlayAttendees",
  "esgOverlayAttendees",
  "additionalTeamAttendees",
] as const;
type TeamField = (typeof TEAM_FIELDS)[number];
type MultiPersonField = RosterField | TeamField;

interface PersonPicker {
  key: MultiPersonField;
  label: string;
  placeholder: string;
}
interface RosterGroup {
  title: string;
  hint: string;
  roles: PersonPicker[];
}
const ROSTER_GROUPS: RosterGroup[] = [
  {
    title: "Standard quorum",
    hint: "",
    roles: [
      { key: "chair", label: "Chair", placeholder: "Search RMB AD" },
      { key: "independentMember", label: "Independent", placeholder: "Search RMB AD" },
      { key: "portfolioCce1", label: "Portfolio CCE / CCE 1", placeholder: "Search RMB AD" },
      { key: "portfolioCce2", label: "Portfolio CCE / CCE 2", placeholder: "Search RMB AD" },
      { key: "businessRep1", label: "Business rep", placeholder: "Search RMB AD" },
      { key: "seniorBusinessRep", label: "Senior business rep", placeholder: "Search RMB AD" },
    ],
  },
  {
    title: "Enhanced quorum",
    hint: "Added for enhanced quorum levels (L1/L2).",
    roles: [
      { key: "lecRep", label: "LEC rep", placeholder: "Search RMB AD" },
      { key: "eqMemberFrmExec", label: "EQ member / FRM exec", placeholder: "Search RMB AD" },
    ],
  },
  {
    title: "Supporting",
    hint: "",
    roles: [{ key: "scribe", label: "Scribe", placeholder: "Search RMB AD" }],
  },
];
const TEAM_PICKERS: PersonPicker[] = [
  { key: "creditTeam", label: "Credit team", placeholder: "Add a credit team member" },
  { key: "businessTeam", label: "Business team", placeholder: "Add a business team member" },
];
const OVERLAY_PICKERS: PersonPicker[] = [
  { key: "mezzOverlayAttendees", label: "Mezz / overlay", placeholder: "Search RMB AD" },
  { key: "esgOverlayAttendees", label: "ESG overlay", placeholder: "Search RMB AD" },
  {
    key: "additionalTeamAttendees",
    label: "Additional attendees",
    placeholder: "Search RMB AD to add attendees",
  },
];
const MULTI_PERSON_FIELDS: MultiPersonField[] = [
  ...ROSTER_GROUPS.flatMap((g) => g.roles.map((r) => r.key)),
  ...TEAM_FIELDS,
];

/** A person on the item: the captured display name and, when picked from AD, the UPN to invite. */
interface Person {
  name: string;
  upn?: string;
}

function perMultiField<V>(init: (key: MultiPersonField) => V): Record<MultiPersonField, V> {
  const out = {} as Record<MultiPersonField, V>;
  for (const key of MULTI_PERSON_FIELDS) out[key] = init(key);
  return out;
}

interface ParticipantRow {
  name: string;
  role: string;
  attendance: "Required" | "Optional";
}

const PURPOSES = [
  "Annual review",
  "New facility (existing client)",
  "New facility (new client)",
  "Increase in existing facilities",
];
const RATING_BANDS = ["B1 and better", "B2 - B3", "B4 - B5", "CB and worse", "Not rated"];
const DISCUSSION_TIMES = [15, 30, 45, 60, 75, 90, 105, 120];
const APPLICANT_RELATIONSHIPS = ["New Client", "Existing Client"];
const APPLICATION_TYPES = ["Complex", "Vanilla / well rated", "Annual review", "Other"];
/**
 * Document types as DAF's own page sends them. "Credit Paper" (this exact casing) is the value
 * `AgendaServiceImpl.DOC_TYPE_CREDIT_PAPER` confirms the slot on, so the shared DOCUMENT_TYPES
 * constant is not used here.
 */
const ITEM_DOCUMENT_TYPES = ["Credit Paper", "Annexure", "Supporting Document", "Other"];
const CREDIT_PAPER = "Credit Paper";
const TIME_PATTERN = /^([01]\d|2[0-3]):[0-5]\d$/;

/** DAF `agenda-existing` conditions, statuses and decision types (commit caf8b602). */
const CONDITION_TYPES = ["Condition", "Action", "Follow-up"];
const CONDITION_STATUSES = ["Open", "In progress"];
const DECISION_TYPES = ["Approve", "Reject", "Refer back", "Other"];
/** Item statuses DAF can submit from, and the statuses that put the item in the review workflow. */
const SUBMITTABLE_STATUSES = ["Captured", "Confirmed", "Returned"];
const WORKFLOW_STATUSES = ["Submitted", "Approved", "Rejected", "Removed"];
const REOPENABLE_STATUSES = ["Submitted", "Approved", "Rejected"];
const PAPER_IN_STATUSES = ["Confirmed", "Submitted", "Approved"];

/** Every reason DAF collected with window.prompt(), now captured in one co-ds dialog. */
type ReasonKind =
  | "submit"
  | "return"
  | "approve"
  | "reject"
  | "reopen"
  | "rejectDocument"
  | "closeCondition";
interface ReasonPrompt {
  kind: ReasonKind;
  heading: string;
  notice: string;
  label: string;
  required: boolean;
  requiredError: string;
  confirmLabel: string;
  variant: "primary" | "danger";
  doc?: AgendaItemDocument;
  condition?: AgendaCondition;
  status?: string;
}

/** Edit draft: strings for select-driven values, mapped back onto the item on save. */
interface ItemForm {
  counterpartyName: string;
  businessUnit: string;
  purposeOfSubmission: string;
  purposeDetails: string;
  dcpZar: string;
  ratingBand: string;
  numberOfPages: number | null;
  withinPageGuidance: boolean;
  estimatedMinutes: string;
  lateSubmission: boolean;
  lateSubmissionReason: string;
  applicantRelationship: string;
  applicationType: string;
  roundRobin: boolean;
  countryOfRisk: string;
  industryClassification: string;
  lecCounterparty: boolean;
  startTime: string;
  endTime: string;
  distributionSensitivities: string;
  proposalReceived: boolean;
  proposalDistributedEmail: boolean;
  proposalOnDiligent: boolean;
  comments: string;
  /** Select value per single-person field: the UPN when known to AD, else the captured name. */
  single: Record<SinglePersonField, string>;
  people: Record<MultiPersonField, Person[]>;
}
type FormErrorKey =
  | "counterpartyName"
  | "purposeOfSubmission"
  | "dcpZar"
  | "numberOfPages"
  | "startTime"
  | "endTime"
  | "primaryAnalyst";

const TABS: CoTabItem[] = [
  { id: "details", label: "Details" },
  { id: "team", label: "Team & participants" },
  { id: "documents", label: "Documents" },
  { id: "workflow", label: "Workflow" },
  { id: "post-meeting", label: "Post-meeting" },
  { id: "conditions", label: "Conditions & actions" },
  { id: "notifications", label: "Notifications" },
];

/**
 * One booked agenda item (DAF `agenda-existing.component`): view and edit the submission, upload
 * and download its documents, remove it with a reason, chair approvals for late and
 * out-of-guidance items, the post-meeting capture with sign-offs and lock/unlock, and the mail
 * history. Since DAF commit caf8b602 (BRS v0.1) also the review workflow (submit, return, approve,
 * reject, re-open) with its audited history and role quorum, document versions and validation,
 * actual attendance, and conditions and actions. DAF stays the authority on every rule; this page
 * mirrors them so the controls match.
 */
@Component({
  selector: "app-agenda-item",
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    TableModule,
    CoActionGroupComponent,
    CoAlertComponent,
    CoButtonComponent,
    CoCheckboxComponent,
    CoContentStackComponent,
    CoDetailCardComponent,
    CoDetailFieldComponent,
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
    CoStatCardComponent,
    CoStatusComponent,
    CoTabsComponent,
    CanDirective,
  ],
  templateUrl: "./agenda-item.component.html",
  host: { "data-co-daf": "", "data-co-pattern": "daf-agenda-item" },
})
export class AgendaItemComponent implements OnInit {
  private readonly agenda = inject(AgendaService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly permissions = inject(PermissionsService);
  private readonly confirms = inject(CoConfirmService);
  private readonly dialog = inject(Dialog);
  private dialogRef?: DialogRef<void>;
  private reasonDialogRef?: DialogRef<void>;
  private readonly reasonDialog = viewChild.required<TemplateRef<unknown>>("reasonDialog");
  readonly navigation = inject(DafNavigationService);
  readonly session = inject(SessionService);
  readonly hosted = !!getRemoteHost();
  readonly agiId = Number(this.route.snapshot.paramMap.get("agiId"));

  readonly tabs = TABS;
  readonly rosterGroups = ROSTER_GROUPS;
  readonly teamPickers = TEAM_PICKERS;
  readonly overlayPickers = OVERLAY_PICKERS;
  readonly documentTypeOptions: CoSelectOption[] = ITEM_DOCUMENT_TYPES.map((t) => ({
    value: t,
    label: t,
  }));

  // ---- Page state
  readonly item = signal<AgendaItem | null>(null);
  readonly meeting = signal<AgendaMeeting | null>(null);
  readonly postMeeting = signal<AgendaPostMeeting | null>(null);
  readonly notifications = signal<AgendaNotification[]>([]);
  readonly notificationsError = signal("");
  readonly loading = signal(false);
  readonly error = signal("");
  readonly tab = signal<Tab>("details");
  readonly message = signal("");

  // ---- Item edit
  readonly editing = signal(false);
  readonly saving = signal(false);
  readonly saveError = signal("");
  readonly submitted = signal(false);
  form: ItemForm = this.emptyForm();
  fieldErrors: Partial<Record<FormErrorKey, string>> = {};
  /** Select value each single-person field was seeded with, so an unchanged pick keeps its captured UPN. */
  private seedSingle: Record<SinglePersonField, string> = {
    primaryAnalyst: "",
    reviewAnalyst: "",
    creditSponsor: "",
    buHead: "",
  };
  /** Pending pick per multi-person picker; cleared as soon as the person is added. */
  pendingAdd: Record<MultiPersonField, string> = this.emptyPending();
  purposeOptions: CoSelectOption[] = [];
  ratingOptions: CoSelectOption[] = [];
  minuteOptions: CoSelectOption[] = [];
  relationshipOptions: CoSelectOption[] = [];
  applicationTypeOptions: CoSelectOption[] = [];

  // ---- Live RMB AD name search backing every people picker (debounced, min 2 chars)
  readonly userSearch$ = new Subject<string>();
  readonly users = signal<CoSelectOption[]>([]);
  readonly usersLoading = signal(false);
  /** Every person seen in a search so far, so a pick keeps its name and UPN after the results change. */
  private readonly userByValue = new Map<string, Person>();
  private readonly optionCache = new Map<string, CoSelectOption[]>();

  // ---- Removal
  removeReason = "";
  removeReasonError = "";
  readonly removing = signal(false);

  // ---- Documents
  selectedFile: File | null = null;
  docType = CREDIT_PAPER;
  readonly uploading = signal(false);
  readonly uploadError = signal("");
  readonly uploadMessage = signal("");
  readonly fileError = signal("");

  // ---- Post-meeting capture
  pm: AgendaPostMeeting = { agiId: this.agiId };
  pmErrors: { decision?: string } = {};
  decisionOptions: CoSelectOption[] = [];
  readonly pmSaving = signal(false);
  readonly pmError = signal("");
  readonly pmMessage = signal("");
  readonly pmSubmitted = signal(false);
  decisionTypeOptions: CoSelectOption[] = [];

  // ---- Review workflow
  readonly workflowSaving = signal(false);
  readonly workflowMessage = signal("");
  readonly workflowError = signal("");

  // ---- Document validation (feedback shares the Documents card alerts)
  readonly docSaving = signal(false);

  // ---- Actual attendance: editable copies of the item's participant register
  readonly attendance = signal<AgendaParticipant[]>([]);
  readonly attendanceSaving = signal(false);
  readonly attendanceMessage = signal("");
  readonly attendanceError = signal("");

  // ---- Conditions & actions
  readonly conditions = signal<AgendaCondition[]>([]);
  readonly conditionsSaving = signal(false);
  readonly conditionsMessage = signal("");
  readonly conditionsError = signal("");
  private readonly selectOptionCache = new Map<string, CoSelectOption[]>();

  // ---- Reason dialog (replaces DAF's window.prompt)
  readonly reasonPrompt = signal<ReasonPrompt | null>(null);
  reasonText = "";
  reasonError = "";

  // ---- Derived
  readonly locked = computed(() => this.item()?.meetingStatus === "Locked");
  readonly held = computed(() => this.item()?.meetingStatus === "Held");
  readonly closed = computed(() => this.item()?.meetingStatus === "Closed");
  /** Submitted / Approved / Rejected / Removed items are read-only until returned or re-opened (BRS 6.6). */
  readonly inWorkflow = computed(() => {
    const i = this.item();
    return !!i && WORKFLOW_STATUSES.includes(i.itemStatus);
  });
  /** Locked blocks editing until re-opened; Held is terminal - only post-meeting capture stays open. */
  readonly readOnly = computed(
    () => this.locked() || this.held() || this.closed() || this.inWorkflow(),
  );
  readonly canEdit = computed(
    () => this.permissions.can("agenda.book") || this.permissions.can("agenda.manage"),
  );
  readonly canManage = computed(() => this.permissions.can("agenda.manage"));
  readonly canModify = computed(() => this.canEdit() && !this.readOnly());
  readonly docs = computed<AgendaItemDocument[]>(() => this.item()?.documents ?? []);
  /**
   * DAF `paperUploaded`: a credit paper that is not superseded or rejected, or a status past
   * Confirmed (Submitted, Approved) that already implies the paper is in.
   */
  readonly paperUploaded = computed(() => {
    const i = this.item();
    if (!i) return false;
    return (
      (i.documents ?? []).some(
        (d) =>
          (d.docType || "").toLowerCase() === "credit paper" &&
          d.validationStatus !== "Superseded" &&
          d.validationStatus !== "Rejected",
      ) || PAPER_IN_STATUSES.includes(i.itemStatus)
    );
  });

  // DAF canSubmit / canReview / canApprove / canReopen / quorumOk / canValidateDocuments.
  readonly canSubmit = computed(() => {
    const i = this.item();
    return (
      !!i &&
      !this.locked() &&
      !this.held() &&
      !this.closed() &&
      SUBMITTABLE_STATUSES.includes(i.itemStatus) &&
      this.permissions.can("agenda.submit")
    );
  });
  readonly canReview = computed(() => {
    const i = this.item();
    return (
      !!i &&
      i.itemStatus === "Submitted" &&
      !this.held() &&
      !this.closed() &&
      this.permissions.can("agenda.review")
    );
  });
  readonly canApprove = computed(() => {
    const i = this.item();
    return (
      !!i &&
      i.itemStatus === "Submitted" &&
      !this.held() &&
      !this.closed() &&
      this.permissions.can("agenda.approve")
    );
  });
  readonly canReopen = computed(() => {
    const i = this.item();
    return (
      !!i &&
      REOPENABLE_STATUSES.includes(i.itemStatus) &&
      !this.held() &&
      !this.closed() &&
      this.permissions.can("agenda.reopen")
    );
  });
  readonly quorumOk = computed(() => !!this.item()?.quorum?.met);
  readonly canValidateDocuments = computed(() => this.permissions.can("agenda.validateDocuments"));
  readonly showWorkflow = computed(
    () => this.canSubmit() || this.canReview() || this.canApprove() || this.canReopen(),
  );
  /** Workflow buttons wait for any in-flight save and for the edit draft to be saved or cancelled. */
  readonly workflowBusy = computed(() => this.workflowSaving() || this.saving() || this.editing());
  readonly pmLocked = computed(() => !!this.postMeeting()?.locked);
  readonly title = computed(() => {
    const i = this.item();
    if (!i) return "Agenda item";
    return `Agenda item${i.agendaNumber ? " " + i.agendaNumber : ""} – ${i.counterpartyName}`;
  });
  readonly description = computed(() => {
    const i = this.item();
    if (!i) return "Details, credit paper and post-meeting outcome.";
    const date = i.meetingDate ? formatDate(i.meetingDate, "d MMM yyyy", "en-US") : "";
    return `${i.committeeName} ${i.mandateLevel ?? ""} · ${date}`.trim();
  });
  readonly breadcrumbs = computed<CoBreadcrumb[]>(() => [
    { label: "Committee Agendas", route: this.navigation.href("/home") },
    { label: "Agenda items", route: this.navigation.href("/agenda") },
    { label: this.item()?.counterpartyName ?? "Agenda item" },
  ]);
  readonly bookingRef = computed(() => {
    const i = this.item();
    if (!i) return "";
    const year = i.createdOn ? new Date(i.createdOn).getFullYear() : new Date().getFullYear();
    return `AGD-${year}-${("0000" + i.agiId).slice(-4)}`;
  });
  /** Everyone on the item: quorum roles are Required, overlay/additional are Optional. */
  readonly participantRows = computed<ParticipantRow[]>(() => {
    const i = this.item();
    if (!i) return [];
    const rows: ParticipantRow[] = [];
    const seen = new Set<string>();
    const push = (name: string, role: string, attendance: "Required" | "Optional") => {
      const n = name.trim();
      if (!n || seen.has(n.toLowerCase())) return;
      seen.add(n.toLowerCase());
      rows.push({ name: n, role, attendance });
    };
    for (const g of ROSTER_GROUPS) {
      for (const r of g.roles) {
        for (const n of this.splitNames(i[r.key])) {
          push(n.name, r.key === "chair" ? "Chairperson" : r.label, "Required");
        }
      }
    }
    this.splitNames(i.mezzOverlayAttendees).forEach((p) => push(p.name, "Mezz / Overlay", "Optional"));
    this.splitNames(i.esgOverlayAttendees).forEach((p) => push(p.name, "ESG Overlay", "Optional"));
    this.splitNames(i.additionalTeamAttendees).forEach((p) => push(p.name, "Member", "Optional"));
    return rows;
  });
  readonly completionPct = computed(() => {
    const i = this.item();
    if (!i) return 0;
    const steps = [
      !!i.counterpartyName && !!i.purposeOfSubmission,
      !!i.primaryAnalyst,
      true,
      this.participantRows().length > 0,
      this.docs().length > 0,
      !!this.postMeeting()?.decision,
    ];
    return Math.round((steps.filter(Boolean).length * 100) / steps.length);
  });
  readonly postMeetingLabel = computed(() => {
    const decision = this.postMeeting()?.decision;
    if (decision) return decision;
    return this.held() ? "Outstanding" : "After the meeting is held";
  });
  readonly needsChairApproval = computed(() => {
    const i = this.item();
    return !!i && (!!i.lateSubmission || !i.withinPageGuidance);
  });

  constructor() {
    this.destroyRef.onDestroy(() => {
      this.dialogRef?.close();
      this.reasonDialogRef?.close();
    });
    this.userSearch$
      .pipe(
        distinctUntilChanged(),
        debounceTime(300),
        tap(() => this.usersLoading.set(true)),
        switchMap((term) => {
          const q = (term ?? "").trim();
          if (q.length < 2) return of([]);
          return this.agenda.searchAdUsers("RMB", q).pipe(catchError(() => of([])));
        }),
        takeUntilDestroyed(),
      )
      .subscribe((found) => {
        const options: CoSelectOption[] = [];
        for (const u of found ?? []) {
          const name = (u.displayName || u.userPrincipalName || u.emailAddress || u.mail || "").trim();
          if (!name) continue;
          const upn = u.userPrincipalName || u.emailAddress || u.mail || undefined;
          const value = upn ?? name;
          if (options.some((o) => o.value === value)) continue;
          this.userByValue.set(value, { name, upn });
          options.push({ value, label: name, subtitle: upn && upn !== name ? upn : undefined });
        }
        this.optionCache.clear();
        this.users.set(options);
        this.usersLoading.set(false);
      });
  }

  ngOnInit(): void {
    this.session.ensure();
    this.load();
  }

  // ---- Loading -------------------------------------------------------------------------------

  load(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set("");
    this.agenda
      .getItem(this.agiId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (item) => {
          this.setItem(item);
          this.loading.set(false);
          if (item.agmId) {
            this.agenda
              .getMeeting(item.agmId)
              .pipe(takeUntilDestroyed(this.destroyRef))
              .subscribe({ next: (m) => this.meeting.set(m), error: () => this.meeting.set(null) });
          }
          this.loadPostMeeting();
          this.loadNotifications();
          this.loadConditions();
        },
        error: (err) => {
          this.error.set(this.describe(err, "The DAF service did not answer. It may be down or unreachable from this network."));
          this.loading.set(false);
        },
      });
  }

  private loadPostMeeting(): void {
    this.agenda
      .getPostMeeting(this.agiId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (pm) => this.applyPostMeeting(pm ?? null),
        error: () => this.applyPostMeeting(null),
      });
  }

  loadNotifications(): void {
    this.notificationsError.set("");
    this.agenda
      .getNotifications(this.agiId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (list) => this.notifications.set(list ?? []),
        error: () => {
          this.notifications.set([]);
          this.notificationsError.set("The notification history could not load.");
        },
      });
  }

  setTab(id: string): void {
    this.tab.set(id as Tab);
  }

  /** Sets the item and re-seeds the attendance copies from its participant register. */
  private setItem(item: AgendaItem): void {
    this.item.set(item);
    this.attendance.set((item.participants ?? []).map((p) => ({ ...p })));
  }

  /** DAF `statusPillClass`: success for Confirmed / Approved, danger for Removed / Rejected, warn otherwise. */
  statusKind(status: string | undefined): CoStatusKind {
    switch (status) {
      case "Confirmed":
      case "Approved":
        return "active";
      case "Removed":
      case "Rejected":
        return "blocked";
      default:
        return "risk";
    }
  }

  validationKind(status: string | undefined): CoStatusKind {
    if (status === "Accepted") return "active";
    if (status === "Rejected") return "blocked";
    return "neutral";
  }

  /** A DAF timestamp as the yyyy-MM-dd value a date input takes (DAF keeps the first 10 characters). */
  dateInput(value: DafTimestamp | null | undefined): string {
    if (value === null || value === undefined || value === "") return "";
    if (typeof value === "number") return formatDate(value, "yyyy-MM-dd", "en-US");
    return value.substring(0, 10);
  }

  /** Stable option lists for selects that must also show a value outside the fixed list. */
  private cachedOptions(values: readonly string[], current: string | undefined): CoSelectOption[] {
    const key = `${values.join("|")}::${current ?? ""}`;
    let options = this.selectOptionCache.get(key);
    if (!options) {
      options = this.withCurrent(values, current);
      this.selectOptionCache.set(key, options);
    }
    return options;
  }

  notificationKind(status: string): CoStatusKind {
    if (status === "Sent") return "active";
    if (status === "Failed") return "blocked";
    return "neutral";
  }

  /** Signed-in user's name as DAF records it on approvals and sign-offs. */
  private me(): string {
    const user = this.session.session();
    return user?.displayName || user?.username || "";
  }

  private now(): string {
    return new Date().toISOString();
  }

  private describe(err: unknown, fallback: string): string {
    const e = err as { status?: number; error?: { message?: string } } | null;
    if (e?.status === 401 || e?.status === 403) {
      return "DAF did not accept your session for this action. Sign in again or ask for the credit analyst or chairperson role.";
    }
    return e?.error?.message || fallback;
  }

  /** Runs a request inside a confirm action, turning any failure into the retryable message co-ds shows. */
  private run<T>(source: Observable<T>, fallback: string): Promise<T> {
    return firstValueFrom(source).catch((err: unknown) => {
      throw new Error(this.describe(err, fallback));
    });
  }

  // ---- People --------------------------------------------------------------------------------

  private splitNames(value: string | undefined): Person[] {
    if (!value) return [];
    return value
      .split(";")
      .map((s) => s.trim())
      .filter((s) => s !== "")
      .map((name) => ({ name }));
  }

  /** Current search results plus the picked value when the results no longer hold it, so the pick still shows. */
  optionsFor(value: string): CoSelectOption[] {
    const options = this.users();
    if (!value || options.some((o) => o.value === value)) return options;
    let cached = this.optionCache.get(value);
    if (!cached) {
      const known = this.userByValue.get(value);
      cached = [
        {
          value,
          label: known?.name ?? value,
          subtitle: known?.upn && known.upn !== known.name ? known.upn : "As captured on the item",
        },
        ...options,
      ];
      this.optionCache.set(value, cached);
    }
    return cached;
  }

  addPerson(field: MultiPersonField, value: string | "" | null): void {
    this.pendingAdd[field] = "";
    if (!value) return;
    const person = this.userByValue.get(value) ?? { name: value };
    const list = this.form.people[field];
    if (list.some((p) => p.name.toLowerCase() === person.name.toLowerCase())) return;
    this.form.people[field] = [...list, person];
  }

  removePerson(field: MultiPersonField, index: number): void {
    this.form.people[field] = this.form.people[field].filter((_, i) => i !== index);
  }

  // ---- Edit ----------------------------------------------------------------------------------

  private emptyPending(): Record<MultiPersonField, string> {
    return perMultiField(() => "");
  }

  private emptyForm(): ItemForm {
    return {
      counterpartyName: "",
      businessUnit: "",
      purposeOfSubmission: "",
      purposeDetails: "",
      dcpZar: "",
      ratingBand: "",
      numberOfPages: null,
      withinPageGuidance: false,
      estimatedMinutes: "",
      lateSubmission: false,
      lateSubmissionReason: "",
      applicantRelationship: "",
      applicationType: "",
      roundRobin: false,
      countryOfRisk: "",
      industryClassification: "",
      lecCounterparty: false,
      startTime: "",
      endTime: "",
      distributionSensitivities: "",
      proposalReceived: false,
      proposalDistributedEmail: false,
      proposalOnDiligent: false,
      comments: "",
      single: { primaryAnalyst: "", reviewAnalyst: "", creditSponsor: "", buHead: "" },
      people: perMultiField<Person[]>(() => []),
    };
  }

  private withCurrent(values: readonly string[], current: string | undefined): CoSelectOption[] {
    const list = current && !values.includes(current) ? [current, ...values] : [...values];
    return list.map((v) => ({ value: v, label: v }));
  }

  startEdit(): void {
    const i = this.item();
    if (!i || this.editing() || !this.canModify()) return;
    const single = { primaryAnalyst: "", reviewAnalyst: "", creditSponsor: "", buHead: "" };
    for (const field of SINGLE_PERSON_FIELDS) {
      const id = i[PERSON_ID_FIELD[field]];
      single[field] = (id && this.userByValue.has(id) ? id : i[field]) ?? "";
    }
    this.seedSingle = { ...single };
    this.form = {
      counterpartyName: i.counterpartyName ?? "",
      businessUnit: i.businessUnit ?? "",
      purposeOfSubmission: i.purposeOfSubmission ?? "",
      purposeDetails: i.purposeDetails ?? "",
      dcpZar: i.dcpZar === null || i.dcpZar === undefined ? "" : String(i.dcpZar),
      ratingBand: i.ratingBand ?? "",
      numberOfPages: i.numberOfPages ?? null,
      withinPageGuidance: !!i.withinPageGuidance,
      estimatedMinutes: i.estimatedMinutes ? String(i.estimatedMinutes) : "",
      lateSubmission: !!i.lateSubmission,
      lateSubmissionReason: i.lateSubmissionReason ?? "",
      applicantRelationship: i.applicantRelationship ?? "",
      applicationType: i.applicationType ?? "",
      roundRobin: !!i.roundRobin,
      countryOfRisk: i.countryOfRisk ?? "",
      industryClassification: i.industryClassification ?? "",
      lecCounterparty: !!i.lecCounterparty,
      startTime: i.startTime ?? "",
      endTime: i.endTime ?? "",
      distributionSensitivities: i.distributionSensitivities ?? "",
      proposalReceived: !!i.proposalReceived,
      proposalDistributedEmail: !!i.proposalDistributedEmail,
      proposalOnDiligent: !!i.proposalOnDiligent,
      comments: i.comments ?? "",
      single,
      people: perMultiField((k) => this.splitNames(i[k])),
    };
    this.pendingAdd = this.emptyPending();
    this.purposeOptions = this.withCurrent(PURPOSES, i.purposeOfSubmission);
    this.ratingOptions = this.withCurrent(RATING_BANDS, i.ratingBand);
    this.minuteOptions = this.withCurrent(
      DISCUSSION_TIMES.map(String),
      this.form.estimatedMinutes || undefined,
    ).map((o) => ({ ...o, label: `${o.value} minutes` }));
    this.relationshipOptions = this.withCurrent(APPLICANT_RELATIONSHIPS, i.applicantRelationship);
    this.applicationTypeOptions = this.withCurrent(APPLICATION_TYPES, i.applicationType);
    this.fieldErrors = {};
    this.submitted.set(false);
    this.saveError.set("");
    this.message.set("");
    if (this.tab() !== "details" && this.tab() !== "team") this.tab.set("details");
    this.editing.set(true);
  }

  cancelEdit(): void {
    if (this.saving()) return;
    this.editing.set(false);
    this.submitted.set(false);
    this.fieldErrors = {};
    this.saveError.set("");
  }

  private validate(): FormErrorKey | undefined {
    const f = this.form;
    const digits = f.dcpZar.replace(/[^0-9.]/g, "");
    this.fieldErrors = {
      counterpartyName: f.counterpartyName.trim() ? "" : "Enter the counterparty or deal name.",
      purposeOfSubmission: f.purposeOfSubmission ? "" : "Select the purpose of submission.",
      dcpZar: digits === "" || !Number.isNaN(Number(digits)) ? "" : "Enter the DCP as a number in ZAR.",
      numberOfPages:
        f.numberOfPages === null || f.numberOfPages >= 0 ? "" : "Pages cannot be negative.",
      startTime: !f.startTime || TIME_PATTERN.test(f.startTime) ? "" : "Use the 24-hour form HH:mm, e.g. 08:00.",
      endTime: !f.endTime || TIME_PATTERN.test(f.endTime) ? "" : "Use the 24-hour form HH:mm, e.g. 08:30.",
      primaryAnalyst: f.single.primaryAnalyst ? "" : "Select the primary analyst.",
    };
    const order: FormErrorKey[] = [
      "counterpartyName",
      "purposeOfSubmission",
      "dcpZar",
      "numberOfPages",
      "startTime",
      "endTime",
      "primaryAnalyst",
    ];
    return order.find((k) => this.fieldErrors[k]);
  }

  save(): void {
    const current = this.item();
    if (!current || this.saving() || !this.canModify()) return;
    this.submitted.set(true);
    const firstInvalid = this.validate();
    if (firstInvalid) {
      this.tab.set(firstInvalid === "primaryAnalyst" ? "team" : "details");
      setTimeout(() => document.getElementById(`daf-agi-${firstInvalid}`)?.focus());
      return;
    }
    this.saving.set(true);
    this.saveError.set("");
    this.message.set("");
    this.agenda
      .updateItem(this.applyForm(current, this.form))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (saved) => {
          this.setItem(saved);
          this.saving.set(false);
          this.editing.set(false);
          this.message.set("Agenda item saved.");
        },
        error: (err) => {
          this.saving.set(false);
          this.saveError.set(
            this.describe(err, "The agenda item could not be saved (the meeting may be locked)."),
          );
        },
      });
  }

  /** Maps the draft back onto the item exactly as DAF's page did before calling PUT /items. */
  private applyForm(current: AgendaItem, f: ItemForm): AgendaItem {
    const digits = f.dcpZar.replace(/[^0-9.]/g, "");
    const updated: AgendaItem = {
      ...current,
      counterpartyName: f.counterpartyName.trim(),
      businessUnit: f.businessUnit.trim(),
      purposeOfSubmission: f.purposeOfSubmission,
      purposeDetails: f.purposeDetails.trim(),
      dcpZar: digits === "" ? null : Number(digits),
      ratingBand: f.ratingBand,
      numberOfPages: f.numberOfPages,
      withinPageGuidance: f.withinPageGuidance,
      estimatedMinutes: f.estimatedMinutes ? Number(f.estimatedMinutes) : null,
      // Late is determined by DAF against the meeting cut-off; the reason is always editable.
      lateSubmission: f.lateSubmission,
      lateSubmissionReason: f.lateSubmissionReason.trim(),
      applicantRelationship: f.applicantRelationship,
      applicationType: f.applicationType,
      roundRobin: f.roundRobin,
      countryOfRisk: f.countryOfRisk.trim(),
      industryClassification: f.industryClassification.trim(),
      lecCounterparty: f.lecCounterparty,
      startTime: f.startTime.trim(),
      endTime: f.endTime.trim(),
      distributionSensitivities: f.distributionSensitivities.trim(),
      proposalReceived: f.proposalReceived,
      proposalDistributedEmail: f.proposalDistributedEmail,
      proposalOnDiligent: f.proposalOnDiligent,
      comments: f.comments.trim(),
    };

    // Single-person fields persist the display name plus the UPN of a fresh AD pick.
    const invited: string[] = [];
    for (const field of SINGLE_PERSON_FIELDS) {
      const value = f.single[field];
      const idField = PERSON_ID_FIELD[field];
      if (!value) {
        updated[field] = undefined;
        updated[idField] = undefined;
        continue;
      }
      if (value === this.seedSingle[field]) continue; // unchanged: keep the captured name and UPN
      const person = this.userByValue.get(value);
      updated[field] = person?.name ?? value;
      updated[idField] = person?.upn;
      if (person?.upn) invited.push(person.upn);
    }

    // Multi-person fields are `;`-joined names; every resolved UPN is invited.
    for (const key of MULTI_PERSON_FIELDS) {
      const people = f.people[key];
      const joined = people.map((p) => p.name).join("; ");
      updated[key] = joined || (TEAM_FIELDS.includes(key as TeamField) ? "" : undefined);
      people.forEach((p) => p.upn && invited.push(p.upn));
    }

    // Merge into attendeeEmails, preserving anyone already invited so re-editing never drops a recipient.
    const emails: string[] = [];
    const add = (e: string | undefined) => {
      const v = (e ?? "").trim();
      if (v && !emails.includes(v)) emails.push(v);
    };
    (current.attendeeEmails ?? "").split(";").forEach(add);
    invited.forEach(add);
    add(updated.primaryAnalystId);
    add(updated.reviewAnalystId);
    add(updated.creditSponsorId);
    add(updated.buHeadId);
    updated.attendeeEmails = emails.join(";");

    // Participant register (BRS 6.4) rebuilt from the roster pickers so DAF evaluates quorum by
    // role; existing attendance flags are carried over by person name and role.
    const previous = current.participants ?? [];
    const participants: AgendaParticipant[] = [];
    for (const group of ROSTER_GROUPS) {
      for (const role of group.roles) {
        for (const person of f.people[role.key]) {
          const old = previous.find(
            (p) =>
              p.personName === person.name &&
              p.committeeRole.toLowerCase() === role.key.toLowerCase(),
          );
          participants.push({
            personId: person.upn || (old ? (old.personId ?? null) : null),
            personName: person.name,
            email: person.upn || (old ? (old.email ?? null) : null),
            committeeRole: role.key.charAt(0).toUpperCase() + role.key.slice(1),
            attendanceRequired: true,
            quorumEligible: role.key !== "scribe",
            votingEligible: role.key !== "scribe",
            independentFlag: role.key === "independentMember",
            plannedAttendance: old ? old.plannedAttendance : "Planned",
            invitationStatus: old ? old.invitationStatus : undefined,
            actualAttendance: old ? old.actualAttendance : null,
          });
        }
      }
    }
    updated.participants = participants;
    return updated;
  }

  // ---- Chair approvals (late / out-of-guidance) ------------------------------------------------

  approveLate(): void {
    this.approve(
      "Approve the late submission",
      "Record your approval of this late submission. It is saved on the item with your name and the time.",
      { lateApprovedBy: this.me(), lateApprovedOn: this.now() },
      "Late submission approved.",
    );
  }

  approveOutOfGuidance(): void {
    this.approve(
      "Approve the out-of-guidance paper",
      "Record your approval of a paper outside the committee's page guidance. It is saved on the item with your name and the time.",
      { outOfGuidanceApprovedBy: this.me(), outOfGuidanceApprovedOn: this.now() },
      "Out-of-guidance paper approved.",
    );
  }

  private approve(heading: string, message: string, patch: Partial<AgendaItem>, done: string): void {
    const current = this.item();
    if (!current || this.editing() || this.saving() || !this.canManage() || this.readOnly()) return;
    this.confirms
      .confirm({
        heading,
        message,
        confirmLabel: "Approve",
        pendingLabel: "Saving approval…",
        variant: "primary",
        action: () =>
          this.run(
            this.agenda.updateItem({ ...current, ...patch }),
            "The approval could not be saved (the meeting may be locked).",
          ).then((saved) => this.setItem(saved)),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((approved) => {
        if (approved) this.message.set(done);
      });
  }

  // ---- Removal -------------------------------------------------------------------------------

  openRemoveDialog(template: TemplateRef<unknown>): void {
    const current = this.item();
    if (!current || this.dialogRef || !this.canModify() || current.itemStatus === "Removed") return;
    this.removeReason = "";
    this.removeReasonError = "";
    this.dialogRef = this.dialog.open<void>(template, {
      ...CO_DIALOG_CONFIG,
      ariaLabelledBy: "daf-agenda-remove-title",
    });
    this.dialogRef.closed.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.dialogRef = undefined;
    });
  }

  closeRemoveDialog(): void {
    if (!this.removing()) this.dialogRef?.close();
  }

  confirmRemove(): void {
    const current = this.item();
    if (!current || this.removing()) return;
    const reason = this.removeReason.trim();
    this.removeReasonError = reason ? "" : "Give the reason the item is coming off the agenda.";
    if (!reason) {
      document.getElementById("daf-agi-removeReason")?.focus();
      return;
    }
    this.confirms
      .confirm({
        heading: "Remove from agenda",
        message: `Remove ${current.counterpartyName} from the ${current.committeeName} agenda of ${formatDate(current.meetingDate, "d MMM yyyy", "en-US")}? The analyst and attendees are notified.`,
        confirmLabel: "Remove item",
        pendingLabel: "Removing…",
        variant: "danger",
        action: () => {
          this.removing.set(true);
          if (this.dialogRef) this.dialogRef.disableClose = true;
          return this.run(
            this.agenda.removeItem(current.agiId, reason),
            "The agenda item could not be removed.",
          )
            .then((saved) => {
              this.setItem(saved);
              this.editing.set(false);
            })
            .finally(() => {
              this.removing.set(false);
              if (this.dialogRef) this.dialogRef.disableClose = false;
            });
        },
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((removed) => {
        if (removed) {
          this.dialogRef?.close();
          this.message.set("Agenda item removed.");
        }
      });
  }

  // ---- Documents -----------------------------------------------------------------------------

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files && input.files.length > 0 ? input.files[0] : null;
    this.fileError.set("");
  }

  upload(): void {
    const current = this.item();
    if (!current || this.uploading() || !this.canModify()) return;
    if (!this.selectedFile) {
      this.fileError.set("Choose the PDF to upload.");
      document.getElementById("daf-agi-file")?.focus();
      return;
    }
    const docType = this.docType || CREDIT_PAPER;
    this.uploading.set(true);
    this.uploadError.set("");
    this.uploadMessage.set("");
    this.agenda
      .uploadDocument(current.agiId, this.selectedFile, docType)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (saved) => {
          this.setItem(saved);
          this.selectedFile = null;
          const input = document.getElementById("daf-agi-file") as HTMLInputElement | null;
          if (input) input.value = "";
          this.uploading.set(false);
          this.uploadMessage.set(
            docType === CREDIT_PAPER
              ? "Credit paper uploaded - the agenda slot is now confirmed."
              : `${docType} uploaded.`,
          );
        },
        error: (err) => {
          this.uploading.set(false);
          this.uploadError.set(this.describe(err, "The document could not be uploaded."));
        },
      });
  }

  download(doc: AgendaItemDocument): void {
    this.uploadError.set("");
    this.agenda
      .downloadDocument(doc.agidId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const anchor = document.createElement("a");
          anchor.href = url;
          anchor.download = doc.fileName;
          document.body.appendChild(anchor);
          anchor.click();
          anchor.remove();
          URL.revokeObjectURL(url);
        },
        error: (err) => this.uploadError.set(this.describe(err, `${doc.fileName} could not be downloaded.`)),
      });
  }

  // ---- Post-meeting capture ------------------------------------------------------------------

  private applyPostMeeting(saved: AgendaPostMeeting | null): void {
    this.postMeeting.set(saved);
    this.pm = { ...(saved ?? {}), agiId: this.agiId };
    if (this.pm.decisionDate) this.pm.decisionDate = this.dateInput(this.pm.decisionDate);
    this.decisionOptions = this.withCurrent(DECISION_OPTIONS, saved?.decision);
    this.decisionTypeOptions = this.withCurrent(DECISION_TYPES, saved?.decisionType);
    this.pmErrors = {};
    this.pmSubmitted.set(false);
  }

  /** Capture stays open only once the meeting is Held and until the chair signs off. */
  pmDisabled(): boolean {
    return !this.held() || this.pmLocked() || !this.canEdit() || this.pmSaving();
  }

  savePostMeeting(): void {
    if (this.pmDisabled()) return;
    this.persistPostMeeting({ ...this.pm }, "Post-meeting capture saved.");
  }

  signOffAnalyst(): void {
    if (this.pmDisabled() || !this.requireDecision()) return;
    this.persistPostMeeting(
      { ...this.pm, analystSignOffBy: this.me(), analystSignOffOn: this.now() },
      "Analyst sign-off recorded.",
    );
  }

  signOffChair(): void {
    if (this.pmDisabled() || !this.canManage() || !this.requireDecision()) return;
    const payload: AgendaPostMeeting = { ...this.pm, chairSignOffBy: this.me(), chairSignOffOn: this.now() };
    this.confirms
      .confirm({
        heading: "Sign off as chairperson",
        message:
          "Chair sign-off saves the capture and locks the post-meeting record. Only a reviewer can unlock it for further edits.",
        confirmLabel: "Sign off and lock",
        pendingLabel: "Signing off…",
        variant: "primary",
        action: () =>
          this.run(
            this.agenda.savePostMeeting(this.agiId, payload),
            "The chair sign-off could not be saved.",
          ).then((saved) => this.applyPostMeeting(saved)),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((signed) => {
        if (signed) this.pmMessage.set("Chair sign-off recorded; the post-meeting record is locked.");
      });
  }

  unlockPostMeeting(): void {
    if (!this.pmLocked() || !this.permissions.can("agenda.unlockPostMeeting") || this.pmSaving()) return;
    this.confirms
      .confirm({
        heading: "Unlock the post-meeting record",
        message:
          "Unlocking reopens a signed-off record for editing. Your name and the time are recorded against the unlock.",
        confirmLabel: "Unlock",
        pendingLabel: "Unlocking…",
        variant: "danger",
        action: () =>
          this.run(
            this.agenda.unlockPostMeeting(this.agiId),
            "The record could not be unlocked (review analyst role required).",
          ).then((saved) => this.applyPostMeeting(saved)),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((unlocked) => {
        if (unlocked) this.pmMessage.set("Post-meeting record unlocked for editing.");
      });
  }

  private requireDecision(): boolean {
    this.pmSubmitted.set(true);
    this.pmErrors = { decision: this.pm.decision ? "" : "Capture the decision before signing off." };
    if (this.pmErrors.decision) {
      document.getElementById("daf-agi-decision")?.focus();
      return false;
    }
    return true;
  }

  private persistPostMeeting(payload: AgendaPostMeeting, done: string): void {
    this.pmSaving.set(true);
    this.pmError.set("");
    this.pmMessage.set("");
    this.agenda
      .savePostMeeting(this.agiId, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (saved) => {
          this.applyPostMeeting(saved);
          this.pmSaving.set(false);
          this.pmMessage.set(done);
        },
        error: (err) => {
          this.pmSaving.set(false);
          this.pmError.set(this.describe(err, "The post-meeting capture could not be saved."));
        },
      });
  }

  // ---- Review & approval workflow (BRS BR-010) -----------------------------------------------

  /** DAF `runWorkflow`: the call returns the refreshed item (status, actions, quorum). */
  private runWorkflow(request: Observable<AgendaItem>, okMessage: string): void {
    this.workflowSaving.set(true);
    this.workflowMessage.set("");
    this.workflowError.set("");
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (saved) => {
        this.setItem(saved);
        this.workflowSaving.set(false);
        this.workflowMessage.set(okMessage);
        this.loadNotifications();
      },
      error: (err: unknown) => {
        this.workflowSaving.set(false);
        this.workflowError.set(this.describe(err, "The action was refused."));
      },
    });
  }

  submitItem(): void {
    const current = this.item();
    if (!current || this.workflowBusy() || !this.canSubmit()) return;
    if (!this.quorumOk()) {
      // DAF: only the secretariat or chair may submit without quorum, with a recorded reason.
      if (!this.permissions.can("agenda.reopen")) {
        this.workflowMessage.set("");
        this.workflowError.set(
          "Quorum is not met for this item - complete the roster or ask the secretariat / chair to submit with an override reason.",
        );
        return;
      }
      this.openReason({
        kind: "submit",
        heading: "Submit for review",
        notice: "Quorum is not met. Enter the quorum override reason (recorded on the item), or cancel.",
        label: "Quorum override reason",
        required: true,
        requiredError: "Enter the quorum override reason.",
        confirmLabel: "Submit for Review",
        variant: "primary",
      });
      return;
    }
    this.runWorkflow(
      this.agenda.submitItem(current.agiId, null),
      "Agenda item submitted for review.",
    );
  }

  returnItem(): void {
    if (!this.item() || this.workflowBusy() || !this.canReview()) return;
    this.openReason({
      kind: "return",
      heading: "Return to analyst",
      notice: "",
      label: "Reason for returning this item to the analyst",
      required: true,
      requiredError: "Give the reason for returning this item to the analyst.",
      confirmLabel: "Return to Analyst",
      variant: "primary",
    });
  }

  approveItem(): void {
    if (!this.item() || this.workflowBusy() || !this.canApprove()) return;
    this.openReason({
      kind: "approve",
      heading: "Approve agenda item",
      notice: "",
      label: "Approval comments",
      required: false,
      requiredError: "",
      confirmLabel: "Approve",
      variant: "primary",
    });
  }

  rejectItem(): void {
    if (!this.item() || this.workflowBusy() || !this.canApprove()) return;
    this.openReason({
      kind: "reject",
      heading: "Reject agenda item",
      notice: "",
      label: "Reason for rejecting this item",
      required: true,
      requiredError: "Give the reason for rejecting this item.",
      confirmLabel: "Reject",
      variant: "danger",
    });
  }

  reopenItem(): void {
    if (!this.item() || this.workflowBusy() || !this.canReopen()) return;
    const locked = this.locked();
    this.openReason({
      kind: "reopen",
      heading: "Re-open agenda item",
      notice: locked
        ? "The agenda is locked - re-opening is recorded as an authorised override."
        : "",
      label: locked ? "Reason" : "Reason for re-opening this item for the analyst",
      required: true,
      requiredError: "Give the reason for re-opening this item.",
      confirmLabel: "Re-open",
      variant: "primary",
    });
  }

  // ---- Document validation (BRS 6.5) ---------------------------------------------------------

  validateDocument(doc: AgendaItemDocument, status: "Accepted" | "Rejected"): void {
    if (this.docSaving() || !this.canValidateDocuments()) return;
    if (status === "Rejected") {
      this.openReason({
        kind: "rejectDocument",
        heading: "Reject document",
        notice: `${doc.docType}: ${doc.fileName}`,
        label: "Reason for rejecting this document",
        required: true,
        requiredError: "Give the reason for rejecting this document.",
        confirmLabel: "Reject",
        variant: "danger",
        doc,
      });
      return;
    }
    this.persistValidation(doc, status, "");
  }

  private persistValidation(doc: AgendaItemDocument, status: string, comment: string): void {
    this.docSaving.set(true);
    this.uploadError.set("");
    this.uploadMessage.set("");
    this.agenda
      .validateDocument(doc.agidId, status, comment)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (saved) => {
          this.docSaving.set(false);
          this.item.update((i) =>
            i
              ? {
                  ...i,
                  documents: (i.documents ?? []).map((d) =>
                    d.agidId === doc.agidId ? { ...d, ...saved } : d,
                  ),
                }
              : i,
          );
          this.uploadMessage.set("Document marked " + status + ".");
        },
        error: (err: unknown) => {
          this.docSaving.set(false);
          this.uploadError.set(this.describe(err, "Failed to validate the document."));
        },
      });
  }

  // ---- Actual attendance after the meeting (RULE-006) ----------------------------------------

  saveAttendance(): void {
    const current = this.item();
    if (!current || this.attendanceSaving() || !current.participants) return;
    this.attendanceSaving.set(true);
    this.attendanceMessage.set("");
    this.attendanceError.set("");
    this.agenda
      .saveParticipants(current.agiId, this.attendance())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (saved) => {
          this.item.update((i) => (i ? { ...i, participants: saved } : i));
          this.attendance.set((saved ?? []).map((p) => ({ ...p })));
          this.attendanceSaving.set(false);
          this.attendanceMessage.set("Attendance saved.");
        },
        error: (err: unknown) => {
          this.attendanceSaving.set(false);
          this.attendanceError.set(this.describe(err, "Failed to save attendance."));
        },
      });
  }

  // ---- Conditions & actions (BRS 6.8) --------------------------------------------------------

  loadConditions(): void {
    this.agenda
      .getConditions(this.agiId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (list) => this.conditions.set(list ?? []),
        error: () => this.conditions.set([]),
      });
  }

  conditionTypeOptions(current: string | undefined): CoSelectOption[] {
    return this.cachedOptions(CONDITION_TYPES, current);
  }

  conditionStatusOptions(current: string | undefined): CoSelectOption[] {
    return this.cachedOptions(CONDITION_STATUSES, current);
  }

  /** Closed and Waived conditions are final. */
  conditionFinal(c: AgendaCondition): boolean {
    return c.status === "Closed" || c.status === "Waived";
  }

  conditionLocked(c: AgendaCondition): boolean {
    return this.conditionFinal(c) || this.closed();
  }

  addCondition(): void {
    if (this.conditionsSaving() || this.closed()) return;
    this.conditions.update((list) => [
      ...list,
      { agcaId: 0, conditionType: "Condition", description: "", ownerName: "", dueDate: null, status: "Open" },
    ]);
  }

  removeCondition(c: AgendaCondition): void {
    if (this.conditionsSaving() || this.conditionLocked(c)) return;
    const drop = () => this.conditions.update((list) => list.filter((x) => x !== c));
    if (!((c.agcaId ?? 0) > 0)) {
      drop();
      return;
    }
    this.confirms
      .confirm({
        heading: "Remove condition",
        message: "Remove this condition? It is removed from the item when the conditions are saved.",
        confirmLabel: "Remove",
        variant: "danger",
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((ok) => {
        if (ok) drop();
      });
  }

  saveConditions(): void {
    const current = this.item();
    if (!current || this.conditionsSaving() || this.closed()) return;
    this.conditionsSaving.set(true);
    this.conditionsMessage.set("");
    this.conditionsError.set("");
    const payload: AgendaCondition[] = this.conditions().map((c) => ({
      ...c,
      dueDate: c.dueDate
        ? String(c.dueDate).length === 10
          ? c.dueDate + "T00:00:00"
          : c.dueDate
        : null,
    }));
    this.agenda
      .saveConditions(current.agiId, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (saved) => {
          this.conditions.set(saved ?? []);
          this.conditionsSaving.set(false);
          this.conditionsMessage.set("Conditions saved.");
        },
        error: (err: unknown) => {
          this.conditionsSaving.set(false);
          this.conditionsError.set(this.describe(err, "Failed to save conditions."));
        },
      });
  }

  closeCondition(c: AgendaCondition, status: "Closed" | "Waived"): void {
    if (this.conditionsSaving() || !c.agcaId) return;
    this.openReason({
      kind: "closeCondition",
      heading: status === "Closed" ? "Close condition" : "Waive condition",
      notice: c.description,
      label: "Closure comments (" + status + ")",
      required: true,
      requiredError: "Enter the closure comments.",
      confirmLabel: status === "Closed" ? "Close" : "Waive",
      variant: "primary",
      condition: c,
      status,
    });
  }

  private persistCloseCondition(c: AgendaCondition, status: string, comments: string): void {
    if (!c.agcaId) return;
    this.conditionsSaving.set(true);
    this.conditionsMessage.set("");
    this.conditionsError.set("");
    this.agenda
      .closeCondition(c.agcaId, status, comments)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (saved) => {
          this.conditions.update((list) => list.map((x) => (x === c ? { ...x, ...saved } : x)));
          this.conditionsSaving.set(false);
          this.conditionsMessage.set("Condition " + status.toLowerCase() + ".");
        },
        error: (err: unknown) => {
          this.conditionsSaving.set(false);
          this.conditionsError.set(this.describe(err, "Failed to close the condition."));
        },
      });
  }

  // ---- Reason dialog -------------------------------------------------------------------------

  private openReason(prompt: ReasonPrompt): void {
    if (this.reasonDialogRef || this.dialogRef) return;
    this.reasonPrompt.set(prompt);
    this.reasonText = "";
    this.reasonError = "";
    this.reasonDialogRef = this.dialog.open<void>(this.reasonDialog(), {
      ...CO_DIALOG_CONFIG,
      ariaLabelledBy: "daf-agenda-reason-title",
    });
    this.reasonDialogRef.closed.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.reasonDialogRef = undefined;
    });
  }

  closeReasonDialog(): void {
    this.reasonDialogRef?.close();
  }

  /** Validates the reason, closes the dialog and runs the action with feedback in its card. */
  confirmReason(): void {
    const prompt = this.reasonPrompt();
    const current = this.item();
    if (!prompt || !current) return;
    const raw = this.reasonText;
    const text = raw.trim();
    if (prompt.required && !text) {
      this.reasonError = prompt.requiredError;
      document.getElementById("daf-agi-reason")?.focus();
      return;
    }
    this.reasonError = "";
    this.reasonDialogRef?.close();
    switch (prompt.kind) {
      case "submit":
        // DAF sends the override reason as typed.
        this.runWorkflow(
          this.agenda.submitItem(current.agiId, raw),
          "Agenda item submitted for review.",
        );
        break;
      case "return":
        this.runWorkflow(
          this.agenda.returnItem(current.agiId, text),
          "Agenda item returned to the analyst.",
        );
        break;
      case "approve":
        this.runWorkflow(
          this.agenda.approveItem(current.agiId, text),
          "Agenda item approved onto the agenda.",
        );
        break;
      case "reject":
        this.runWorkflow(this.agenda.rejectItem(current.agiId, text), "Agenda item rejected.");
        break;
      case "reopen":
        this.runWorkflow(
          this.agenda.reopenItem(current.agiId, text),
          "Agenda item re-opened for editing.",
        );
        break;
      case "rejectDocument":
        if (prompt.doc) this.persistValidation(prompt.doc, "Rejected", text);
        break;
      case "closeCondition":
        if (prompt.condition && prompt.status) {
          this.persistCloseCondition(prompt.condition, prompt.status, text);
        }
        break;
    }
  }

  // ---- Navigation ----------------------------------------------------------------------------

  back(): void {
    this.navigation.navigate("/agenda");
  }

  toDashboard(): void {
    this.navigation.navigate("/agenda/dashboard");
  }

  openMeeting(): void {
    const i = this.item();
    if (i) this.navigation.navigate(`/agenda/meeting/${i.agmId}`);
  }

  // TODO export: DAF's "Submission PDF" (jsPDF), "Export Excel" (xlsx) and print pack are not
  // carried over; the item overview and participants table hold the same content on screen.
}
