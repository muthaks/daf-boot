import { Component, computed, DestroyRef, inject, OnInit, signal, TemplateRef } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { Dialog, DialogRef } from "@angular/cdk/dialog";
import { DatePipe, formatDate } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { firstValueFrom, Observable } from "rxjs";
import { TableModule } from "primeng/table";
import { jsPDF } from "jspdf";
import autoTable, { type RowInput } from "jspdf-autotable";
import * as XLSX from "xlsx";
import {
  CO_DIALOG_CONFIG,
  CoActionGroupComponent,
  CoAlertComponent,
  CoBreadcrumb,
  CoButtonComponent,
  CoCheckboxComponent,
  CoCheckboxGroupComponent,
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
  CoSearchFieldComponent,
  CoSectionHeadingComponent,
  CoSelectOption,
  CoSpinnerComponent,
  CoStatCardComponent,
  CoStatusComponent,
  CoStatusKind,
} from "co-ds";
import {
  AgendaCommittee,
  AgendaItem,
  AgendaMeeting,
  AgendaMeetingRecord,
  AgendaPostMeeting,
} from "../../core/models/agenda.models";
import { AgendaService } from "../../core/services/agenda.service";
import { DafNavigationService } from "../../core/services/daf-navigation.service";
import { PermissionsService } from "../../core/services/permissions.service";
import { SessionService } from "../../core/services/session.service";
import { getRemoteHost } from "../../remote/remote-host";
import { CanDirective } from "../../shared/directives/can.directive";

/** Post-meeting fields captured per item but describing the meeting as a whole. */
type MeetingWideField = "minuteTaker" | "administrator";

/** Roster roles that must be physically present - a clash means one person is double-booked. */
const ROSTER_KEYS = [
  "chair",
  "lecRep",
  "eqMemberFrmExec",
  "independentMember",
  "businessRep1",
  "seniorBusinessRep",
  "portfolioCce1",
  "portfolioCce2",
  "scribe",
] as const satisfies readonly (keyof AgendaItem)[];

/** Same 4-hour session rule the booking wizard applies (agenda-new sessionMinutes). */
const SESSION_MINUTES = 240;

/** Which card shows the success / error alert of the last action. */
type FeedbackScope = "page" | "management" | "correction" | "records";

/** Quorum achieved as captured when marking held; "unknown" leaves it to DAF (sent as null). */
type QuorumChoice = "unknown" | "yes" | "no";

/** Reason-capturing meeting actions (DAF used window.prompt for both). */
type ReasonMode = "cancel" | "reopenClosed";

/** DAF's record types (agenda-meeting recordTypes). */
const RECORD_TYPES = ["Minutes", "Transcript", "Recording", "Agenda pack", "Other"];

const QUORUM_OPTIONS: CoSelectOption[] = [
  { value: "unknown", label: "Unknown - DAF derives it from attendance" },
  { value: "yes", label: "Yes - quorum achieved" },
  { value: "no", label: "No - quorum not achieved" },
];

const REASON_COPY: Record<
  ReasonMode,
  {
    heading: string;
    description: string;
    label: string;
    hint: string;
    required: string;
    submit: string;
    confirmHeading: string;
    confirmMessage: string;
    confirmLabel: string;
    pendingLabel: string;
    success: string;
    fallback: string;
    scope: FeedbackScope;
  }
> = {
  cancel: {
    heading: "Cancel meeting",
    description: "Reason for cancelling this meeting (attendees are notified).",
    label: "Cancellation reason",
    hint: "Sent to the attendees and kept on the meeting.",
    required: "Give the reason for cancelling this meeting.",
    submit: "Cancel meeting",
    confirmHeading: "Cancel this meeting?",
    confirmMessage:
      "The meeting is cancelled and the attendees are notified with this reason. This cannot be undone from this page.",
    confirmLabel: "Cancel meeting",
    pendingLabel: "Cancelling…",
    success: "Meeting cancelled.",
    fallback: "Failed to cancel the meeting.",
    scope: "management",
  },
  reopenClosed: {
    heading: "Re-open closed meeting record",
    description: "Correction reason for re-opening this closed meeting record (audited).",
    label: "Correction reason",
    hint: "Recorded in the meeting workflow history.",
    required: "Give the correction reason for re-opening this closed meeting record.",
    submit: "Re-open record",
    confirmHeading: "Re-open the closed meeting record?",
    confirmMessage:
      "Decisions, conditions and attendance become editable again. The correction is audited with this reason.",
    confirmLabel: "Re-open record",
    pendingLabel: "Re-opening…",
    success: "Meeting record re-opened for correction.",
    fallback: "Failed to re-open the meeting record.",
    scope: "correction",
  },
};

/**
 * Committee meeting page (DAF `agenda-meeting.component`): the meeting header, the running order
 * with agenda numbers and times, scheduling-clash detection, the committee member-list link, the
 * chair's compile / lock / re-open / mark-held (with actuals) / close / cancel actions, the audited
 * re-open of a closed record, the meeting's governance records and its workflow history.
 */
@Component({
  selector: "app-agenda-meeting",
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
    CoDetailFieldComponent,
    CoDialogLayoutComponent,
    CoEmptyStateComponent,
    CoErrorStateComponent,
    CoFieldComponent,
    CoFieldGridComponent,
    CoInputDirective,
    CoPageHeaderComponent,
    CoSearchableSelectComponent,
    CoSearchFieldComponent,
    CoSectionHeadingComponent,
    CoSpinnerComponent,
    CoStatCardComponent,
    CoStatusComponent,
    CanDirective,
  ],
  templateUrl: "./agenda-meeting.component.html",
  host: { "data-co-daf": "", "data-co-pattern": "daf-agenda-meeting" },
})
export class AgendaMeetingComponent implements OnInit {
  private readonly agenda = inject(AgendaService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly confirm = inject(CoConfirmService);
  private readonly permissions = inject(PermissionsService);
  private readonly dialog = inject(Dialog);
  private dialogRef?: DialogRef<void>;
  readonly navigation = inject(DafNavigationService);
  readonly session = inject(SessionService);
  readonly hosted = !!getRemoteHost();
  readonly sessionMinutes = SESSION_MINUTES;

  private readonly agmId = Number(this.route.snapshot.paramMap.get("agmId"));

  // Meeting
  readonly meeting = signal<AgendaMeeting | null>(null);
  readonly loading = signal(false);
  readonly error = signal("");
  readonly saving = signal(false);
  readonly message = signal("");
  readonly actionError = signal("");
  /** Card that shows `message` / `actionError`: the alert sits inside the card whose action ran. */
  readonly feedbackScope = signal<FeedbackScope>("page");

  // Mark-held dialog (DAF prompted for the actual start / end times)
  heldStart = "";
  heldEnd = "";
  heldQuorum: QuorumChoice = "unknown";
  heldStartError = "";
  heldEndError = "";
  readonly quorumOptions = QUORUM_OPTIONS;

  // Cancel / re-open-closed reason dialog (DAF prompted for the reason)
  readonly reasonMode = signal<ReasonMode>("cancel");
  readonly reasonCopy = computed(() => REASON_COPY[this.reasonMode()]);
  reasonText = "";
  reasonError = "";

  /** Pending state and API error shown inside whichever dialog is open. */
  readonly dialogPending = signal(false);
  readonly dialogError = signal("");

  // Meeting records (BR-020): minutes, transcript, recording, agenda pack references
  readonly recordTypeOptions: CoSelectOption[] = RECORD_TYPES.map((t) => ({ value: t, label: t }));
  newRecord = { recordType: "Minutes", name: "", location: "" };

  // Committee record behind this meeting - used by the secretary to link the List Management
  // list that pre-populates the people pickers on booking.
  readonly committee = signal<AgendaCommittee | null>(null);
  readonly availableLists = signal<string[]>([]);
  readonly savingList = signal(false);
  listNameEdit = "";

  // Register search / filters / pagination (screen list only - exports use all items).
  readonly query = signal("");
  readonly selectedStatuses = signal<Record<string, boolean>>({});
  readonly lateOnly = signal(false);
  readonly first = signal(0);

  /** DAF canManage: secretary or chairperson (BRS RULE-008). */
  readonly canManage = computed(() => this.permissions.can("agenda.manage"));

  readonly items = computed<AgendaItem[]>(() => this.meeting()?.items ?? []);
  readonly activeItems = computed(() => this.items().filter((i) => i.itemStatus !== "Removed"));
  readonly statusOptions = computed(() =>
    this.items()
      .map((i) => i.itemStatus)
      .filter((s, ix, arr) => !!s && arr.indexOf(s) === ix)
      .sort(),
  );
  readonly listOptions = computed<CoSelectOption[]>(() => [
    { value: "", label: "None - capture people one by one" },
    ...this.availableLists().map((name) => ({ value: name, label: name })),
  ]);

  readonly meetingStart = computed(() => {
    let start = "";
    for (const i of this.items()) {
      if (i.startTime && (start === "" || toMinutes(i.startTime) < toMinutes(start))) {
        start = i.startTime;
      }
    }
    return start;
  });
  readonly meetingEnd = computed(() => {
    let end = "";
    for (const i of this.items()) {
      if (i.endTime && (end === "" || toMinutes(i.endTime) > toMinutes(end))) end = i.endTime;
    }
    return end;
  });

  readonly bookedMinutes = computed(() =>
    this.activeItems().reduce((n, i) => n + (i.estimatedMinutes ?? 0), 0),
  );
  readonly sessionOver = computed(() => this.bookedMinutes() > SESSION_MINUTES);

  readonly isLocked = computed(() => this.meeting()?.status === "Locked");
  readonly isCompiled = computed(() => this.meeting()?.status === "Compiled");
  /** DAF isHeld: a closed meeting has also been held. */
  readonly isHeld = computed(() => {
    const status = this.meeting()?.status;
    return status === "Held" || status === "Closed";
  });
  readonly isClosed = computed(() => this.meeting()?.status === "Closed");
  readonly isCancelled = computed(() => this.meeting()?.status === "Cancelled");
  /** Items still awaiting review / approval (only approved items are compiled onto the agenda). */
  readonly pendingReview = computed(
    () => this.activeItems().filter((i) => i.itemStatus === "Submitted").length,
  );

  /** DAF shows the chair actions only while `canManage() && !isCancelled()`. */
  readonly canRunChairActions = computed(() => this.canManage() && !this.isCancelled());
  /** DAF Close Meeting Record: canManage, held and not yet closed. */
  readonly canCloseMeeting = computed(
    () => this.canRunChairActions() && this.isHeld() && !this.isClosed(),
  );
  /** DAF Cancel Meeting: secretary, not held (and not already cancelled). */
  readonly canCancelMeeting = computed(
    () => this.permissions.can("agenda.cancelMeeting") && !this.isHeld() && !this.isCancelled(),
  );
  /** DAF reopenClosed: review analyst, closed record. */
  readonly canReopenClosedMeeting = computed(
    () => this.permissions.can("agenda.reopenClosedMeeting") && this.isClosed(),
  );
  /** DAF records card shows once held; adding needs canManage and a record that is not closed. */
  readonly canAddRecord = computed(() => this.canManage() && this.isHeld() && !this.isClosed());
  readonly meetingDatePassed = computed(() => {
    const m = this.meeting();
    return !!m && !!m.meetingDate && new Date(m.meetingDate).getTime() <= Date.now();
  });

  /** Overlapping time slots that share a person (chair/quorum) - that person can't be in both. */
  readonly conflicts = computed<string[]>(() => {
    const result: string[] = [];
    const items = this.items().filter(
      (i) => i.itemStatus !== "Removed" && toMinutes(i.startTime) >= 0 && toMinutes(i.endTime) >= 0,
    );
    for (let a = 0; a < items.length; a++) {
      for (let b = a + 1; b < items.length; b++) {
        const iA = items[a];
        const iB = items[b];
        const sA = toMinutes(iA.startTime);
        const eA = toMinutes(iA.endTime);
        const sB = toMinutes(iB.startTime);
        const eB = toMinutes(iB.endTime);
        if (sA < eB && sB < eA) {
          const peopleB = itemPeople(iB);
          const shared = itemPeople(iA).filter((p) =>
            peopleB.some((q) => q.toLowerCase() === p.toLowerCase()),
          );
          if (shared.length > 0) {
            const labelA = iA.agendaNumber || iA.counterpartyName;
            const labelB = iB.agendaNumber || iB.counterpartyName;
            result.push(
              `${labelA} (${iA.startTime}-${iA.endTime}) and ${labelB} (${iB.startTime}-${iB.endTime}) overlap and share: ${shared.join(", ")}`,
            );
          }
        }
      }
    }
    return result;
  });

  readonly hasActiveFilters = computed(
    () => this.lateOnly() || this.statusOptions().some((s) => this.selectedStatuses()[s]),
  );

  readonly filtered = computed(() => {
    const selected = this.selectedStatuses();
    const anyStatus = this.statusOptions().some((s) => selected[s]);
    const lateOnly = this.lateOnly();
    const text = this.query().trim().toLowerCase();
    const contains = (v: unknown) =>
      String(v ?? "")
        .toLowerCase()
        .includes(text);
    return this.items().filter(
      (i) =>
        (!anyStatus || selected[i.itemStatus]) &&
        (!lateOnly || i.lateSubmission) &&
        (text === "" ||
          contains(i.counterpartyName) ||
          contains(i.purposeOfSubmission) ||
          contains(i.primaryAnalyst) ||
          contains(i.creditTeam) ||
          contains(i.businessTeam) ||
          contains(i.agendaNumber) ||
          contains(i.countryOfRisk) ||
          contains(i.industryClassification)),
    );
  });

  readonly title = computed(() => {
    const m = this.meeting();
    return m ? `${m.committeeName} ${m.mandateLevel ?? ""}`.trim() : "Committee meeting";
  });
  readonly breadcrumbs = computed<CoBreadcrumb[]>(() => {
    const m = this.meeting();
    return [
      { label: "Committee Agendas", route: this.navigation.href("/home") },
      { label: "Agenda items", route: this.navigation.href("/agenda") },
      {
        label: m
          ? `${m.committeeName} · ${formatDate(m.meetingDate, "d MMM yyyy", "en-US")}`
          : "Committee meeting",
      },
    ];
  });

  ngOnInit(): void {
    this.session.ensure();
    this.load();
  }

  load(): void {
    if (this.loading()) return;
    if (!Number.isFinite(this.agmId) || this.agmId <= 0) {
      this.error.set("This link does not name a committee meeting.");
      return;
    }
    this.loading.set(true);
    this.error.set("");
    this.agenda
      .getMeeting(this.agmId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (meeting) => {
          this.setMeeting(meeting);
          this.loading.set(false);
          this.loadCommittee();
        },
        error: (err: unknown) => {
          this.error.set(
            statusOf(err) === 401 || statusOf(err) === 403
              ? "DAF did not accept your session for the agenda module. Sign in again or ask for the credit analyst or chairperson role."
              : statusOf(err) === 404
                ? "This committee meeting does not exist or has been removed."
                : "Failed to load the meeting. The DAF service may be down or unreachable from this network.",
          );
          this.loading.set(false);
        },
      });
  }

  private loadCommittee(): void {
    const agcId = this.meeting()?.agcId;
    this.agenda
      .getCommittees()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (committees) => {
          const found = (committees ?? []).find((c) => c.agcId === agcId) ?? null;
          this.committee.set(found);
          this.listNameEdit = found?.listName ?? "";
        },
        error: () => this.committee.set(null),
      });
    this.agenda
      .getAvailableLists()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (names) => this.availableLists.set(names ?? []),
        error: () => this.availableLists.set([]),
      });
  }

  saveListName(): void {
    const committee = this.committee();
    if (committee === null || this.savingList() || !this.canManage()) return;
    this.savingList.set(true);
    this.clearFeedback("management");
    const trimmed = this.listNameEdit.trim();
    // DAF clears the link when listName is null (an omitted key leaves it unchanged), so send
    // null explicitly as DAF's own UI does; the DTO type only declares the string case.
    const payload = {
      ...committee,
      listName: trimmed === "" ? null : trimmed,
    } as unknown as AgendaCommittee;
    this.agenda
      .updateCommittee(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (saved) => {
          this.committee.set(saved);
          this.listNameEdit = saved?.listName ?? "";
          this.savingList.set(false);
          this.message.set(
            "Committee member list updated - new bookings will offer these members in the people pickers.",
          );
        },
        error: (err: unknown) => {
          this.savingList.set(false);
          this.actionError.set(apiMessage(err, "Failed to update the committee member list."));
        },
      });
  }

  private setMeeting(meeting: AgendaMeeting): void {
    this.meeting.set(meeting);
    this.first.set(0);
  }

  /** Clears the last alert and moves the next one into the card of the action that is running. */
  clearFeedback(scope: FeedbackScope = this.feedbackScope()): void {
    this.message.set("");
    this.actionError.set("");
    this.feedbackScope.set(scope);
  }

  /** True when the page alert belongs in this card. */
  feedbackIn(scope: FeedbackScope): boolean {
    return this.feedbackScope() === scope && (!!this.message() || !!this.actionError());
  }

  // Filters

  applyFilter(value: string): void {
    this.query.set(value);
    this.first.set(0);
  }

  toggleStatus(status: string, checked: boolean): void {
    this.selectedStatuses.update((s) => ({ ...s, [status]: checked }));
    this.first.set(0);
  }

  setLateOnly(checked: boolean): void {
    this.lateOnly.set(checked);
    this.first.set(0);
  }

  clearFilters(): void {
    this.selectedStatuses.set({});
    this.lateOnly.set(false);
    this.query.set("");
    this.first.set(0);
  }

  // Status presentation

  meetingStatusKind(status: string | undefined): CoStatusKind {
    switch (status) {
      case "Locked":
        return "active";
      case "Held":
      case "Closed":
      case "Final":
      case "Completed":
        return "neutral";
      case "Cancelled":
        return "blocked";
      default:
        return "info";
    }
  }

  itemStatusKind(status: string): CoStatusKind {
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

  // Chair / secretary actions (DAF is the authority; these gates only decide what the UI shows)

  compile(): void {
    if (this.saving() || !this.canManage()) return;
    this.runAction(
      this.agenda.compileAgenda(this.agmId),
      () => "Agenda compiled - item numbers assigned.",
      "Failed to compile the agenda (the meeting may be locked).",
    );
  }

  /**
   * DAF BR-016: capture the actual start / end when the meeting is marked held. Opens the dialog
   * pre-filled with the scheduled times (DAF's prompt defaults); blanks are allowed.
   */
  openHeldDialog(template: TemplateRef<unknown>): void {
    const m = this.meeting();
    if (!m || this.saving() || this.dialogRef || !this.canRunChairActions() || this.isHeld()) return;
    this.heldStart = toTimeInput(m.startTime || this.meetingStart());
    this.heldEnd = toTimeInput(m.endTime || this.meetingEnd());
    this.heldQuorum = "unknown";
    this.heldStartError = "";
    this.heldEndError = "";
    this.openDialog(template, "daf-meeting-held-title");
  }

  markHeld(): void {
    if (this.saving() || this.dialogPending() || !this.canManage()) return;
    const start = this.heldStart.trim();
    const end = this.heldEnd.trim();
    this.heldStartError =
      start === "" || TIME_PATTERN.test(start) ? "" : "Enter the actual start time as HH:mm, or leave it blank.";
    this.heldEndError =
      end === "" || TIME_PATTERN.test(end) ? "" : "Enter the actual end time as HH:mm, or leave it blank.";
    if (this.heldStartError || this.heldEndError) {
      document.getElementById(this.heldStartError ? "daf-meeting-held-start" : "daf-meeting-held-end")?.focus();
      return;
    }
    const quorum = this.heldQuorum === "yes" ? true : this.heldQuorum === "no" ? false : null;
    const fallback = "Failed to mark the meeting as held (it must be locked and past its meeting date).";
    this.saving.set(true);
    this.setDialogPending(true);
    this.clearFeedback("management");
    this.agenda
      .markMeetingHeldWithActuals(this.agmId, start, end, quorum)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (meeting) => {
          this.setMeeting(meeting);
          this.saving.set(false);
          this.setDialogPending(false);
          this.dialogRef?.close();
          this.message.set("Meeting marked as held - post-meeting capture is now open.");
        },
        error: (err: unknown) => {
          this.saving.set(false);
          this.setDialogPending(false);
          const text = apiMessage(err, fallback);
          this.dialogError.set(text);
          this.actionError.set(text);
        },
      });
  }

  closeMeeting(): void {
    if (this.saving() || !this.canCloseMeeting()) return;
    this.confirm
      .confirm({
        heading: "Close the meeting record?",
        message: "Decisions, conditions and attendance become final.",
        confirmLabel: "Close meeting record",
        pendingLabel: "Closing…",
        variant: "danger",
        errorMessage: "Failed to close the meeting record.",
        action: () =>
          this.runConfirmedAction(
            this.agenda.closeMeeting(this.agmId),
            () => "Meeting record closed.",
            "Failed to close the meeting record.",
            "management",
          ),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe();
  }

  /** Cancel (secretary) or re-open a closed record (review analyst): both need a reason. */
  openReasonDialog(template: TemplateRef<unknown>, mode: ReasonMode): void {
    if (this.saving() || this.dialogRef) return;
    if (mode === "cancel" ? !this.canCancelMeeting() : !this.canReopenClosedMeeting()) return;
    this.reasonMode.set(mode);
    this.reasonText = "";
    this.reasonError = "";
    this.openDialog(template, "daf-meeting-reason-title");
  }

  submitReason(): void {
    if (this.saving() || this.dialogPending()) return;
    const mode = this.reasonMode();
    const copy = REASON_COPY[mode];
    const allowed = mode === "cancel" ? this.canCancelMeeting() : this.canReopenClosedMeeting();
    if (!allowed) return;
    const reason = this.reasonText.trim();
    this.reasonError = reason ? "" : copy.required;
    if (!reason) {
      document.getElementById("daf-meeting-reason")?.focus();
      return;
    }
    this.confirm
      .confirm({
        heading: copy.confirmHeading,
        message: copy.confirmMessage,
        confirmLabel: copy.confirmLabel,
        pendingLabel: copy.pendingLabel,
        variant: "danger",
        errorMessage: copy.fallback,
        action: () => {
          this.setDialogPending(true);
          const request =
            mode === "cancel"
              ? this.agenda.cancelMeeting(this.agmId, reason)
              : this.agenda.reopenMeeting(this.agmId, reason);
          return this.runConfirmedAction(request, () => copy.success, copy.fallback, copy.scope).finally(
            () => this.setDialogPending(false),
          );
        },
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((done) => {
        if (done) this.dialogRef?.close();
      });
  }

  closeDialog(): void {
    if (!this.dialogPending()) this.dialogRef?.close();
  }

  private openDialog(template: TemplateRef<unknown>, titleId: string): void {
    this.dialogError.set("");
    this.dialogPending.set(false);
    this.dialogRef = this.dialog.open<void>(template, {
      ...CO_DIALOG_CONFIG,
      ariaLabelledBy: titleId,
    });
    this.dialogRef.closed.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.dialogRef = undefined;
      this.dialogPending.set(false);
      this.dialogError.set("");
    });
  }

  private setDialogPending(pending: boolean): void {
    this.dialogPending.set(pending);
    if (this.dialogRef) this.dialogRef.disableClose = pending;
  }

  // Meeting records (BR-020)

  isLink(location: string | undefined): boolean {
    return !!location && location.indexOf("http") === 0;
  }

  addRecord(): void {
    const record = this.newRecord;
    if (this.saving() || !record.name || !record.location || !this.canAddRecord()) return;
    this.saving.set(true);
    this.clearFeedback("records");
    this.agenda
      .addMeetingRecord(this.agmId, { recordType: record.recordType, name: record.name, location: record.location })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (saved) => {
          // DAF answers with the new AgendaMeetingRecordDTO, which DAF's page appends to the meeting's records.
          const current = this.meeting();
          if (current) this.meeting.set({ ...current, records: [...(current.records ?? []), saved] });
          this.newRecord = { recordType: "Minutes", name: "", location: "" };
          this.saving.set(false);
          this.message.set("Record reference added.");
        },
        error: (err: unknown) => {
          this.saving.set(false);
          this.actionError.set(apiMessage(err, "Failed to add the record reference."));
        },
      });
  }

  lock(): void {
    if (this.saving() || !this.canManage()) return;
    this.confirm
      .confirm({
        heading: "Lock the meeting agenda?",
        message:
          "Locking closes the agenda for editing and fixes the item numbers and times sent to attendees. It can be re-opened by the chairperson if needed.",
        confirmLabel: "Lock agenda",
        pendingLabel: "Locking…",
        variant: "danger",
        errorMessage: "Failed to lock the meeting (compile the agenda first).",
        action: () =>
          this.runConfirmedAction(
            this.agenda.lockMeeting(this.agmId),
            () => "Meeting agenda locked for editing.",
            "Failed to lock the meeting (compile the agenda first).",
            "management",
          ),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe();
  }

  /**
   * Re-open a locked agenda back to Draft so items (roster, attendees, deal details) can be
   * edited again. The same people who lock it can re-open it.
   */
  reopen(): void {
    if (this.saving() || !this.canManage()) return;
    this.confirm
      .confirm({
        heading: "Re-open this locked agenda?",
        message:
          "Attendees have already received the final agenda. Re-opening returns the meeting to Draft so items can be edited again.",
        confirmLabel: "Re-open agenda",
        pendingLabel: "Re-opening…",
        variant: "danger",
        errorMessage: "Failed to re-open the meeting agenda.",
        action: () =>
          this.runConfirmedAction(
            this.agenda.setMeetingStatus(this.agmId, "Draft"),
            () => "Agenda re-opened - items can be edited again.",
            "Failed to re-open the meeting agenda.",
            "management",
          ),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe();
  }

  autoAdjust(): void {
    if (this.saving() || !this.canManage()) return;
    this.confirm
      .confirm({
        heading: "Auto-adjust the discussion times?",
        message:
          "The discussion times are re-sequenced back-to-back to clear the scheduling clash. Manually set times will be re-sequenced.",
        confirmLabel: "Auto-adjust times",
        pendingLabel: "Adjusting…",
        variant: "primary",
        errorMessage: "Failed to adjust the times (the meeting may be locked).",
        action: () =>
          this.runConfirmedAction(
            this.agenda.rescheduleAgenda(this.agmId),
            () =>
              this.conflicts().length > 0
                ? "Times adjusted, but some clashes remain - please review."
                : "Times adjusted - the clash has been cleared.",
            "Failed to adjust the times (the meeting may be locked).",
            "page",
          ),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe();
  }

  private runAction(
    request: Observable<AgendaMeeting>,
    success: () => string,
    fallback: string,
    scope: FeedbackScope = "management",
  ): void {
    this.saving.set(true);
    this.clearFeedback(scope);
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (meeting) => {
        this.setMeeting(meeting);
        this.saving.set(false);
        this.message.set(success());
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.actionError.set(apiMessage(err, fallback));
      },
    });
  }

  /** Runs inside the confirm dialog: rejecting with an Error keeps the dialog open with the message. */
  private async runConfirmedAction(
    request: Observable<AgendaMeeting>,
    success: () => string,
    fallback: string,
    scope: FeedbackScope,
  ): Promise<void> {
    this.saving.set(true);
    this.clearFeedback(scope);
    try {
      const meeting = await firstValueFrom(request);
      this.setMeeting(meeting);
      this.message.set(success());
    } catch (err: unknown) {
      throw new Error(apiMessage(err, fallback));
    } finally {
      this.saving.set(false);
    }
  }

  // Navigation

  openItem(item: AgendaItem): void {
    this.navigation.navigate(`/agenda/item/${item.agiId}`);
  }

  toDashboard(): void {
    this.navigation.navigate("/agenda/dashboard");
  }

  back(): void {
    this.navigation.navigate("/home");
  }

  // Exports (client-side only; same sections as DAF's print/PDF documents)

  /** First non-empty post-meeting value across items (minute taker / administrator are per item but describe the meeting). */
  minutesValue(field: MeetingWideField): string {
    for (const i of this.activeItems()) {
      const value = i.postMeeting?.[field];
      if (value) return value;
    }
    return "";
  }

  downloadAgendaPdf(): void {
    const meeting = this.meeting();
    if (!meeting) return;
    const doc = new jsPDF("l", "pt", "a4");
    let y = this.pdfHeader(doc, meeting, "Credit Committee Agenda");
    const items = meeting.items ?? [];

    y = this.pdfSectionTitle(doc, "1. Agenda & Schedule", y);
    y = this.pdfTable(
      doc,
      ["Item", "Start", "End", "Deal Name", "Purpose", "Mandate", "RR", "Time", "Pages", "Paper In", "Status"],
      items.map((i) => [
        i.agendaNumber,
        i.startTime,
        i.endTime,
        i.counterpartyName,
        i.purposeOfSubmission,
        i.mandateLevel,
        yn(i.roundRobin),
        `${i.estimatedMinutes ?? 0} min`,
        i.numberOfPages,
        yn(i.itemStatus === "Confirmed"),
        i.itemStatus,
      ]),
      y,
    );

    y = this.pdfSectionTitle(doc, "2. Teams & Classification", y);
    y = this.pdfTable(
      doc,
      ["Item", "Deal Name", "Applicant Relationship", "Application Type", "Country of Risk", "Industry", "Credit Team", "Business Team"],
      items.map((i) => [
        i.agendaNumber,
        i.counterpartyName,
        i.applicantRelationship,
        i.applicationType,
        i.countryOfRisk,
        i.industryClassification,
        i.creditTeam,
        i.businessTeam,
      ]),
      y,
    );

    if (this.isHeld()) {
      y = this.pdfSectionTitle(doc, "3. Post-Meeting Outcomes", y);
      this.pdfTable(
        doc,
        ["Item", "Deal Name", "Decision", "Act. Pages", "Act. Time", "Act. Rating", "Act. DCP", "Date of CC"],
        items.map((i) => [
          i.agendaNumber,
          i.counterpartyName,
          i.postMeeting?.decision,
          i.postMeeting?.actualPages,
          i.postMeeting?.actualMinutes,
          i.postMeeting?.actualRating,
          i.postMeeting?.actualDcp,
          isoDate(i.postMeeting?.dateOfCc),
        ]),
        y,
      );
    }

    this.pdfFooter(doc, "agenda");
    doc.save(`Agenda_${meeting.committeeName}_${isoDate(meeting.meetingDate)}.pdf`);
  }

  /** Minutes document - available once the meeting is Held. */
  downloadMinutesPdf(): void {
    const meeting = this.meeting();
    if (!meeting) return;
    const doc = new jsPDF("p", "pt", "a4");
    let y = this.pdfHeader(doc, meeting, "Minutes of the Credit Committee Meeting");
    doc.setFontSize(9);
    let line = `Chairperson: ${meeting.chair ?? ""}`;
    if (this.minutesValue("minuteTaker")) line += `   |   Minutes prepared by: ${this.minutesValue("minuteTaker")}`;
    if (this.minutesValue("administrator")) line += `   |   Administrator: ${this.minutesValue("administrator")}`;
    doc.text(line, 40, y - 8);
    y += 14;

    y = this.pdfSectionTitle(doc, "1. Attendance", y);
    y = this.pdfTable(
      doc,
      ["Item", "Deal Name", "Attendees Present", "Scribe"],
      this.activeItems().map((i) => [
        i.agendaNumber,
        i.counterpartyName,
        i.postMeeting?.attendeesPresent,
        i.postMeeting?.scribe || i.scribe,
      ]),
      y,
    );

    y = this.pdfSectionTitle(doc, "2. Items & Decisions", y);
    y += 4;
    for (const i of this.activeItems()) {
      const pageHeight = doc.internal.pageSize.getHeight();
      if (y > pageHeight - 120) {
        doc.addPage();
        y = 40;
      }
      doc.setFontSize(9);
      doc.setFont("helvetica", "bold");
      doc.text(
        `${i.agendaNumber ?? ""} - ${i.counterpartyName}   (${i.postMeeting?.decision || "No decision captured"})`,
        40,
        y,
      );
      y += 6;
      const pm: AgendaPostMeeting = i.postMeeting ?? { agiId: i.agiId };
      y =
        this.pdfTable(
          doc,
          null,
          [
            ["Purpose", i.purposeOfSubmission ?? ""],
            ["Discussion slot", `${i.startTime ?? ""}-${i.endTime ?? ""} (${i.estimatedMinutes ?? 0} min)`],
            ["Actual time", `${pm.actualMinutes ?? ""} min`],
            ["Pages (planned / actual)", `${i.numberOfPages ?? ""} / ${pm.actualPages ?? ""}`],
            ["Rating (planned / actual)", `${i.ratingBand ?? ""} / ${pm.actualRating ?? ""}`],
            ["DCP (planned / actual)", `${i.dcpZar ?? ""} / ${pm.actualDcp ?? ""}`],
            ["Date of CC", isoDate(pm.dateOfCc)],
            ["Credit team", i.creditTeam ?? ""],
            ["Credit conditions", pm.creditConditions ?? ""],
          ],
          y,
        ) - 10;
    }

    y = this.pdfSectionTitle(doc, "3. Sign-off", y + 10);
    y += 30;
    doc.setFontSize(9);
    doc.setFont("helvetica", "normal");
    doc.line(40, y, 240, y);
    doc.text(`Chairperson: ${meeting.chair ?? ""}`, 40, y + 14);
    doc.text("Date: ____________________", 40, y + 28);
    doc.line(320, y, 520, y);
    doc.text("Meeting Secretary", 320, y + 14);
    doc.text("Date: ____________________", 320, y + 28);

    this.pdfFooter(doc, "minutes");
    doc.save(`Minutes_${meeting.committeeName}_${isoDate(meeting.meetingDate)}.pdf`);
  }

  exportExcel(): void {
    const meeting = this.meeting();
    if (!meeting) return;
    const rows = (meeting.items ?? []).map((i) => ({
      "Committee Name": meeting.committeeName,
      Item: i.agendaNumber,
      "Agenda item": i.counterpartyName,
      Purpose: i.purposeOfSubmission,
      "Purpose detail": i.purposeDetails,
      "Start time": i.startTime,
      "Duration (minutes)": i.estimatedMinutes,
      "End time": i.endTime,
      Mandate: i.mandateLevel,
      Quorum: meeting.quorumSize,
      "Round Robin": yn(i.roundRobin),
      Chairperson: i.chair,
      "LEC Rep": i.lecRep,
      "EQ Member / FRM Executive": i.eqMemberFrmExec,
      "Business Rep 1": i.businessRep1,
      "Senior Business Rep / Segment": i.seniorBusinessRep,
      "Portfolio CCE / CCE 1": i.portfolioCce1,
      "Portfolio CCE / CCE 2": i.portfolioCce2,
      "Independent Member": i.independentMember,
      "Credit Sponsor": i.creditSponsor,
      Scribe: i.scribe,
      "Mezz / Early Overlay / Additional attendees": i.mezzOverlayAttendees,
      "ESG Overlay and Committee attendees": i.esgOverlayAttendees,
      "Distribution sensitivities and confidential issues": i.distributionSensitivities,
      "LEC Counterparty": yn(i.lecCounterparty),
      "Country of Risk": i.countryOfRisk,
      "Industry description / classification": i.industryClassification,
      "Credit team and Sponsor": i.creditTeam,
      "Transactors and BU Signoff": i.businessTeam,
      "Additional team attendees": i.additionalTeamAttendees,
      "Applicant relationship": i.applicantRelationship,
      "Application type": i.applicationType,
      "Number of pages": i.numberOfPages,
      "Within Page Guidance": yn(i.withinPageGuidance),
      "DCP (ZAR)": i.dcpZar,
      Rating: i.ratingBand,
      "Primary Analyst": i.primaryAnalyst,
      "Review Analyst": i.reviewAnalyst,
      "Late submission": yn(i.lateSubmission),
      "Proposal Received": yn(i.proposalReceived),
      "Proposal distributed via email": yn(i.proposalDistributedEmail),
      "Proposal on Diligent": yn(i.proposalOnDiligent),
      "Document Submitted": i.itemStatus === "Confirmed" ? "Y" : "N",
      Status: i.itemStatus,
      "Comments and notes": i.comments,
      "Reasons for late submission": i.lateSubmissionReason,
      Decision: i.postMeeting?.decision ?? "",
      "Actual Pages": i.postMeeting?.actualPages ?? "",
      "Actual Time (min)": i.postMeeting?.actualMinutes ?? "",
      "Actual Rating": i.postMeeting?.actualRating ?? "",
      "Actual Purpose": i.postMeeting?.actualPurpose ?? "",
      "Actual DCP (ZAR)": i.postMeeting?.actualDcp ?? "",
      "Date of CC": isoDate(i.postMeeting?.dateOfCc),
      "Credit Conditions": i.postMeeting?.creditConditions ?? "",
      "Attendees Present": i.postMeeting?.attendeesPresent ?? "",
      "Minute Taker": i.postMeeting?.minuteTaker ?? "",
      "Post-Meeting Scribe": i.postMeeting?.scribe ?? "",
      Administrator: i.postMeeting?.administrator ?? "",
    }));

    const worksheet = XLSX.utils.json_to_sheet(rows);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, "Agenda");
    XLSX.writeFile(workbook, `Agenda_${meeting.committeeName}_${isoDate(meeting.meetingDate)}.xlsx`);
  }

  private pdfSectionTitle(doc: jsPDF, title: string, y: number): number {
    const pageHeight = doc.internal.pageSize.getHeight();
    if (y > pageHeight - 60) {
      doc.addPage();
      y = 40;
    }
    doc.setFontSize(10);
    doc.setFont("helvetica", "bold");
    doc.text(title, 40, y);
    return y + 8;
  }

  private pdfTable(doc: jsPDF, head: string[] | null, body: unknown[][], y: number): number {
    const rows: RowInput[] = body.length > 0
      ? body.map((row) => row.map((cell) => (cell === null || cell === undefined ? "" : String(cell))))
      : [["No items booked for this meeting."]];
    autoTable(doc, {
      head: head === null ? undefined : [head],
      body: rows,
      startY: y,
      margin: { left: 40, right: 40 },
      styles: { fontSize: 7, cellPadding: 3 },
      headStyles: { fillColor: [236, 236, 236], textColor: 20, fontStyle: "bold" },
      theme: "grid",
    });
    const finalY = (doc as unknown as { lastAutoTable?: { finalY?: number } }).lastAutoTable?.finalY ?? y;
    return finalY + 24;
  }

  private pdfHeader(doc: jsPDF, meeting: AgendaMeeting, title: string): number {
    doc.setFontSize(16);
    doc.setFont("helvetica", "bold");
    doc.text(title, 40, 46);
    doc.setDrawColor(250, 157, 30);
    doc.setLineWidth(2);
    doc.line(40, 54, doc.internal.pageSize.getWidth() - 40, 54);
    doc.setFontSize(12);
    doc.text(`${meeting.committeeName} ${meeting.mandateLevel ?? ""}`, 40, 72);
    doc.setFontSize(9);
    doc.setFont("helvetica", "normal");
    doc.text(
      `Meeting date: ${isoDate(meeting.meetingDate)}   |   Status: ${meeting.status}   |   Held via: ${meeting.heldVia ?? ""}   |   Quorum: ${meeting.quorumSize ?? ""}`,
      40,
      88,
    );
    const y = 102;
    let line = `Paper cut-off: ${isoDate(meeting.cutOffOn)}`;
    if (meeting.lockedBy) line += `   |   Agenda locked by ${meeting.lockedBy} on ${isoDate(meeting.lockedOn)}`;
    doc.text(line, 40, y);
    return y + 22;
  }

  private pdfFooter(doc: jsPDF, label: string): void {
    const pageCount = doc.getNumberOfPages();
    const generated = new Date().toISOString().substring(0, 16).replace("T", " ");
    for (let p = 1; p <= pageCount; p++) {
      doc.setPage(p);
      doc.setFontSize(7);
      doc.setFont("helvetica", "normal");
      doc.setTextColor(120);
      doc.text(
        `Digital Approval Forum - ${label} generated ${generated}   |   Page ${p} of ${pageCount}`,
        40,
        doc.internal.pageSize.getHeight() - 20,
      );
      doc.setTextColor(20);
    }
  }
}

/** 24-hour HH:mm, the value an `<input type="time">` produces. */
const TIME_PATTERN = /^([01]\d|2[0-3]):[0-5]\d$/;

/** DAF times may be `8:00`; a time input needs `08:00`. Anything else starts blank. */
function toTimeInput(t: string | undefined | null): string {
  const m = /^(\d{1,2}):(\d{2})/.exec(String(t ?? "").trim());
  if (!m) return "";
  const value = `${m[1].padStart(2, "0")}:${m[2]}`;
  return TIME_PATTERN.test(value) ? value : "";
}


function toMinutes(t: string | undefined | null): number {
  if (!t) return -1;
  const m = /^(\d{1,2}):(\d{2})$/.exec(String(t).trim());
  return m ? parseInt(m[1], 10) * 60 + parseInt(m[2], 10) : -1;
}

/** Roster fields hold `;`-joined names (roles allow more than one person), compared one by one. */
function itemPeople(item: AgendaItem): string[] {
  const people: string[] = [];
  for (const key of ROSTER_KEYS) {
    const value = item[key];
    if (value === undefined || value === null || String(value).trim() === "") continue;
    for (const name of String(value).split(";")) {
      const trimmed = name.trim();
      if (trimmed !== "") people.push(trimmed);
    }
  }
  return people;
}

function yn(value: unknown): "Y" | "N" {
  return value ? "Y" : "N";
}

function isoDate(value: string | undefined | null): string {
  if (!value) return "";
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? "" : d.toISOString().substring(0, 10);
}

function statusOf(err: unknown): number | undefined {
  return typeof err === "object" && err !== null && "status" in err
    ? (err as { status?: number }).status
    : undefined;
}

/** DAF's error body carries `message`; fall back to the page's own wording. */
function apiMessage(err: unknown, fallback: string): string {
  const body =
    typeof err === "object" && err !== null && "error" in err
      ? (err as { error?: { message?: unknown } }).error
      : undefined;
  return body && typeof body.message === "string" && body.message ? body.message : fallback;
}
