import { DafUser } from "./auth.models";

/**
 * Named UI permissions. Templates and menus refer to these keys; nothing in a
 * component compares DAF role strings directly.
 *
 * Add a permission by adding a key here and its rule in PERMISSION_RULES.
 */
export type DafPermission =
  | "agenda.view"
  | "agenda.book"
  | "agenda.submit"
  | "agenda.review"
  | "agenda.approve"
  | "agenda.reopen"
  | "agenda.validateDocuments"
  | "agenda.manage"
  | "agenda.unlockPostMeeting"
  | "agenda.reopenClosedMeeting"
  | "agenda.cancelMeeting"
  | "agenda.admin";

export type DafPermissions = Readonly<Record<DafPermission, boolean>>;

const hasRole = (user: DafUser, role: string) =>
  user.roles.some((r) => r.toLowerCase().includes(role));

const analyst = (u: DafUser) => hasRole(u, "creditanalyst");
const reviewAnalyst = (u: DafUser) => hasRole(u, "reviewanalyst");
const approver = (u: DafUser) => hasRole(u, "approver");
const chairperson = (u: DafUser) => hasRole(u, "chairperson");
const secretary = (u: DafUser) => hasRole(u, "secretary");

/**
 * How each permission is derived from the roles in DAF's token (`CreditAnalyst`, `ReviewAnalyst`,
 * `Approver`, `Chairperson`, `Secretary`). Each rule mirrors the helper DAF's own agenda pages use
 * (DAF commit caf8b602, BRS v0.1); status conditions stay in the pages. DAF enforces the same roles
 * on every call; these rules only decide what the UI shows.
 */
const PERMISSION_RULES: Record<DafPermission, (user: DafUser) => boolean> = {
  // SessionService.canViewAgenda: every DAF role may open the module.
  "agenda.view": (u) => analyst(u) || chairperson(u) || secretary(u) || reviewAnalyst(u) || approver(u),
  // Agenda list "Book Agenda Item": analysts.
  "agenda.book": analyst,
  // agenda-existing canSubmit.
  "agenda.submit": (u) => analyst(u) || secretary(u) || chairperson(u),
  // agenda-existing canReview (return / reject).
  "agenda.review": (u) => reviewAnalyst(u) || approver(u) || chairperson(u) || secretary(u),
  // agenda-existing canApprove.
  "agenda.approve": (u) => approver(u) || chairperson(u),
  // agenda-existing canReopen, and submitting with a quorum override.
  "agenda.reopen": (u) => secretary(u) || chairperson(u),
  // agenda-existing canValidateDocuments.
  "agenda.validateDocuments": (u) => reviewAnalyst(u) || secretary(u) || chairperson(u),
  // agenda-meeting canManage: compile, lock, mark held, close (BRS RULE-008).
  "agenda.manage": (u) => secretary(u) || chairperson(u),
  // agenda-existing Unlock (isReviewAnalyst); DAF's endpoint checks af.agenda.reviewer.role, default ReviewAnalyst.
  "agenda.unlockPostMeeting": reviewAnalyst,
  // agenda-meeting reopenClosed: audited correction of a closed meeting record.
  "agenda.reopenClosedMeeting": reviewAnalyst,
  // agenda-meeting cancelMeeting.
  "agenda.cancelMeeting": secretary,
  // RoleGuard /agenda/admin and nav "Committee Administration" (BRS BR-001 / BR-002).
  "agenda.admin": secretary,
};

export const PERMISSION_KEYS = Object.keys(PERMISSION_RULES) as DafPermission[];

/** Permissions for a signed-in user; every key false when there is no session. */
export function derivePermissions(user: DafUser | null): DafPermissions {
  return Object.fromEntries(
    PERMISSION_KEYS.map((key) => [key, !!user && PERMISSION_RULES[key](user)]),
  ) as Record<DafPermission, boolean>;
}
