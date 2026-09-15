import { inject, Injectable } from "@angular/core";
import { HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import {
  AgendaCasdmApplication,
  AgendaCommittee,
  AgendaCondition,
  AgendaItem,
  AgendaItemDocument,
  AgendaMeeting,
  AgendaMeetingRecord,
  AgendaNotification,
  AgendaParticipant,
  AgendaPostMeeting,
  AgendaQuorumResult,
  AgendaReportSummary,
  AgendaUser,
  AgendaUserList,
  AgendaWorkflowAction,
  AvailableDate,
  BookAgendaItemRequest,
  CounterpartyDetailList,
  ListManagementList,
} from "../models/agenda.models";
import { DafHttpClient } from "./daf-http-client";

/**
 * DAF's agenda API (`ApprovalForumAgendaResource`, /approvalForum/agenda), one method per
 * endpoint, unchanged from DAF's own `agenda.service.ts` so the backend needs no change.
 */
@Injectable({ providedIn: "root" })
export class AgendaService {
  private readonly http = inject(DafHttpClient);
  private readonly base = "/approvalForum/agenda";

  // Reference data
  /** DAF's List Management user directory, wrapped as `{ users }` (DAF `ListManagementUsers`). */
  getUsers(): Observable<AgendaUserList> {
    return this.http.get<AgendaUserList>(`${this.base}/users`);
  }
  getAvailableLists(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/lists`);
  }
  /** Agenda-scoped creditdata counterparties, wrapped as `{ counterpartyDetail }`. */
  getCounterparties(): Observable<CounterpartyDetailList> {
    return this.http.get<CounterpartyDetailList>(`${this.base}/counterparties`);
  }
  /** Live CASDM counterparty search, as DAF's booking page uses (`CounterpartyService.getCounterparties`). */
  searchCasdmCounterparties(search: string, exact = false): Observable<CounterpartyDetailList> {
    return this.http.get<CounterpartyDetailList>("/approvalForum/external/casdm/counterparties", {
      params: new HttpParams({ fromObject: { search, exact: String(exact) } }),
    });
  }
  /** Live Active Directory people search through DAF (`AdService.getUsers`), e.g. domain "RMB". */
  searchAdUsers(domain: string, displayName: string): Observable<AgendaUser[]> {
    return this.http.get<AgendaUser[]>("/auth/users", {
      params: new HttpParams({ fromObject: { domain, displayName } }),
    });
  }
  getCasdmApplication(inkombaId: string): Observable<AgendaCasdmApplication> {
    return this.http.get<AgendaCasdmApplication>(`${this.base}/casdm/application`, {
      params: new HttpParams({ fromObject: { inkombaId } }),
    });
  }
  getCreditApplication(inkombaId: string): Observable<unknown> {
    return this.http.get("/approvalForum/external/casdm/application", {
      params: new HttpParams({ fromObject: { inkombaId } }),
    });
  }

  // Committees
  getCommittees(): Observable<AgendaCommittee[]> {
    return this.http.get<AgendaCommittee[]>(`${this.base}/committees`);
  }
  updateCommittee(committee: AgendaCommittee): Observable<AgendaCommittee> {
    return this.http.put<AgendaCommittee>(`${this.base}/committees`, committee);
  }
  getAvailableDates(agcId: number): Observable<AvailableDate[]> {
    return this.http.get<AvailableDate[]>(`${this.base}/committees/${agcId}/dates`);
  }
  /** The committee's List Management list (`{ listManagementName, users }`). */
  getCommitteeMembers(agcId: number): Observable<ListManagementList> {
    return this.http.get<ListManagementList>(`${this.base}/committees/${agcId}/members`);
  }
  getLastRoster(agcId: number): Observable<Partial<AgendaItem>> {
    return this.http.get<Partial<AgendaItem>>(`${this.base}/committees/${agcId}/lastRoster`);
  }
  getMeetingsForCommittee(agcId: number): Observable<AgendaMeeting[]> {
    return this.http.get<AgendaMeeting[]>(`${this.base}/committees/${agcId}/meetings`);
  }

  // Meetings
  getMeeting(agmId: number): Observable<AgendaMeeting> {
    return this.http.get<AgendaMeeting>(`${this.base}/meetings/${agmId}`);
  }
  compileAgenda(agmId: number): Observable<AgendaMeeting> {
    return this.http.post<AgendaMeeting>(`${this.base}/meetings/${agmId}/compile`, {});
  }
  rescheduleAgenda(agmId: number): Observable<AgendaMeeting> {
    return this.http.post<AgendaMeeting>(`${this.base}/meetings/${agmId}/reschedule`, {});
  }
  lockMeeting(agmId: number): Observable<AgendaMeeting> {
    return this.http.post<AgendaMeeting>(`${this.base}/meetings/${agmId}/lock`, {});
  }
  setMeetingStatus(agmId: number, status: string): Observable<AgendaMeeting> {
    return this.http.post<AgendaMeeting>(`${this.base}/meetings/${agmId}/status`, {}, {
      params: new HttpParams({ fromObject: { status } }),
    });
  }
  markMeetingHeld(agmId: number): Observable<AgendaMeeting> {
    return this.http.post<AgendaMeeting>(`${this.base}/meetings/${agmId}/held`, {});
  }

  // Items
  getAllItems(): Observable<AgendaItem[]> {
    return this.http.get<AgendaItem[]>(`${this.base}/items`);
  }
  getMyItems(): Observable<AgendaItem[]> {
    return this.http.get<AgendaItem[]>(`${this.base}/items/my`);
  }
  getItem(agiId: number): Observable<AgendaItem> {
    return this.http.get<AgendaItem>(`${this.base}/items/${agiId}`);
  }
  bookItem(request: BookAgendaItemRequest): Observable<AgendaItem> {
    return this.http.post<AgendaItem>(`${this.base}/items`, request);
  }
  updateItem(item: AgendaItem): Observable<AgendaItem> {
    return this.http.put<AgendaItem>(`${this.base}/items`, item);
  }
  removeItem(agiId: number, reason: string): Observable<AgendaItem> {
    return this.http.post<AgendaItem>(`${this.base}/items/${agiId}/remove`, {}, {
      params: new HttpParams({ fromObject: { reason } }),
    });
  }

  // Documents
  uploadDocument(agiId: number, file: File, docType: string): Observable<AgendaItem> {
    const form = new FormData();
    form.append("file", file, file.name);
    form.append("docType", docType);
    return this.http.post<AgendaItem>(`${this.base}/items/${agiId}/upload`, form);
  }
  downloadDocument(agidId: number): Observable<Blob> {
    return this.http.get(`${this.base}/documents/${agidId}/download`, { responseType: "blob" });
  }

  // Post-meeting and notifications
  getPostMeeting(agiId: number): Observable<AgendaPostMeeting> {
    return this.http.get<AgendaPostMeeting>(`${this.base}/items/${agiId}/postMeeting`);
  }
  savePostMeeting(agiId: number, postMeeting: AgendaPostMeeting): Observable<AgendaPostMeeting> {
    return this.http.post<AgendaPostMeeting>(`${this.base}/items/${agiId}/postMeeting`, postMeeting);
  }
  unlockPostMeeting(agiId: number): Observable<AgendaPostMeeting> {
    return this.http.post<AgendaPostMeeting>(`${this.base}/items/${agiId}/postMeeting/unlock`, {});
  }
  getNotifications(agiId: number): Observable<AgendaNotification[]> {
    return this.http.get<AgendaNotification[]>(`${this.base}/items/${agiId}/notifications`);
  }

  // Committee administration (BRS BR-001 / BR-002)
  /** Every committee, including inactive ones. */
  getAllCommittees(): Observable<AgendaCommittee[]> {
    return this.http.get<AgendaCommittee[]>(`${this.base}/committees/all`);
  }
  createCommittee(committee: Partial<AgendaCommittee>): Observable<AgendaCommittee> {
    return this.http.post<AgendaCommittee>(`${this.base}/committees`, committee);
  }
  deactivateCommittee(agcId: number): Observable<AgendaCommittee> {
    return this.http.post<AgendaCommittee>(`${this.base}/committees/${agcId}/deactivate`, {});
  }

  // Meeting calendar and lifecycle
  createMeeting(meeting: Partial<AgendaMeeting>): Observable<AgendaMeeting> {
    return this.http.post<AgendaMeeting>(`${this.base}/meetings`, meeting);
  }
  updateMeeting(agmId: number, meeting: Partial<AgendaMeeting>): Observable<AgendaMeeting> {
    return this.http.put<AgendaMeeting>(`${this.base}/meetings/${agmId}`, meeting);
  }
  cancelMeeting(agmId: number, reason: string): Observable<AgendaMeeting> {
    return this.http.post<AgendaMeeting>(`${this.base}/meetings/${agmId}/cancel`, {}, {
      params: new HttpParams({ fromObject: { reason } }),
    });
  }
  /** Meetings in a date range (`from` / `to` as DAF accepts them, e.g. yyyy-MM-dd). */
  getMeetingsBetween(from: string, to: string): Observable<AgendaMeeting[]> {
    return this.http.get<AgendaMeeting[]>(`${this.base}/meetings`, {
      params: new HttpParams({ fromObject: { from, to } }),
    });
  }
  /** Mark held with the actual times and whether quorum was achieved (BRS 6.7). */
  markMeetingHeldWithActuals(
    agmId: number,
    actualStartTime: string,
    actualEndTime: string,
    quorumAchieved: boolean | null,
  ): Observable<AgendaMeeting> {
    const params: Record<string, string> = {};
    if (actualStartTime) params["actualStartTime"] = actualStartTime;
    if (actualEndTime) params["actualEndTime"] = actualEndTime;
    if (quorumAchieved !== null && quorumAchieved !== undefined) params["quorumAchieved"] = String(quorumAchieved);
    return this.http.post<AgendaMeeting>(`${this.base}/meetings/${agmId}/held`, {}, {
      params: new HttpParams({ fromObject: params }),
    });
  }
  closeMeeting(agmId: number): Observable<AgendaMeeting> {
    return this.http.post<AgendaMeeting>(`${this.base}/meetings/${agmId}/close`, {});
  }
  reopenMeeting(agmId: number, reason: string): Observable<AgendaMeeting> {
    return this.http.post<AgendaMeeting>(`${this.base}/meetings/${agmId}/reopen`, {}, {
      params: new HttpParams({ fromObject: { reason } }),
    });
  }
  getMeetingRecords(agmId: number): Observable<AgendaMeetingRecord[]> {
    return this.http.get<AgendaMeetingRecord[]>(`${this.base}/meetings/${agmId}/records`);
  }
  /** Returns the created record (DAF `AgendaService.addMeetingRecord`), not the meeting. */
  addMeetingRecord(agmId: number, record: Partial<AgendaMeetingRecord>): Observable<AgendaMeetingRecord> {
    return this.http.post<AgendaMeetingRecord>(`${this.base}/meetings/${agmId}/records`, record);
  }

  // Item review workflow (BR-010), audited as workflow actions
  submitItem(agiId: number, quorumOverrideReason?: string | null): Observable<AgendaItem> {
    return this.http.post<AgendaItem>(`${this.base}/items/${agiId}/submit`, {}, {
      params: new HttpParams({ fromObject: quorumOverrideReason ? { quorumOverrideReason } : {} }),
    });
  }
  returnItem(agiId: number, reason: string): Observable<AgendaItem> {
    return this.http.post<AgendaItem>(`${this.base}/items/${agiId}/return`, {}, {
      params: new HttpParams({ fromObject: { reason } }),
    });
  }
  approveItem(agiId: number, comments: string): Observable<AgendaItem> {
    return this.http.post<AgendaItem>(`${this.base}/items/${agiId}/approve`, {}, {
      params: new HttpParams({ fromObject: comments ? { comments } : {} }),
    });
  }
  rejectItem(agiId: number, reason: string): Observable<AgendaItem> {
    return this.http.post<AgendaItem>(`${this.base}/items/${agiId}/reject`, {}, {
      params: new HttpParams({ fromObject: { reason } }),
    });
  }
  reopenItem(agiId: number, reason: string): Observable<AgendaItem> {
    return this.http.post<AgendaItem>(`${this.base}/items/${agiId}/reopen`, {}, {
      params: new HttpParams({ fromObject: { reason } }),
    });
  }
  getWorkflowActions(agiId: number): Observable<AgendaWorkflowAction[]> {
    return this.http.get<AgendaWorkflowAction[]>(`${this.base}/items/${agiId}/actions`);
  }

  // Document validation (BRS 6.5)
  /** `status`: Accepted | Rejected | Waived. */
  validateDocument(agidId: number, status: string, comment: string): Observable<AgendaItemDocument> {
    return this.http.post<AgendaItemDocument>(`${this.base}/documents/${agidId}/validate`, {}, {
      params: new HttpParams({ fromObject: { status, comment: comment || "" } }),
    });
  }

  // Participants and quorum (BRS 6.4, RULE-005)
  getParticipants(agiId: number): Observable<AgendaParticipant[]> {
    return this.http.get<AgendaParticipant[]>(`${this.base}/items/${agiId}/participants`);
  }
  saveParticipants(agiId: number, participants: AgendaParticipant[]): Observable<AgendaParticipant[]> {
    return this.http.put<AgendaParticipant[]>(`${this.base}/items/${agiId}/participants`, participants);
  }
  evaluateQuorum(agiId: number): Observable<AgendaQuorumResult> {
    return this.http.get<AgendaQuorumResult>(`${this.base}/items/${agiId}/quorum`);
  }

  // Conditions and actions
  getConditions(agiId: number): Observable<AgendaCondition[]> {
    return this.http.get<AgendaCondition[]>(`${this.base}/items/${agiId}/conditions`);
  }
  saveConditions(agiId: number, conditions: AgendaCondition[]): Observable<AgendaCondition[]> {
    return this.http.put<AgendaCondition[]>(`${this.base}/items/${agiId}/conditions`, conditions);
  }
  closeCondition(agcaId: number, status: string, comments: string): Observable<AgendaCondition> {
    return this.http.post<AgendaCondition>(`${this.base}/conditions/${agcaId}/close`, {}, {
      params: new HttpParams({ fromObject: { status, comments } }),
    });
  }

  // Reporting
  getReportSummary(from: string, to: string): Observable<AgendaReportSummary> {
    return this.http.get<AgendaReportSummary>(`${this.base}/reports/summary`, {
      params: new HttpParams({ fromObject: { from, to } }),
    });
  }
}
