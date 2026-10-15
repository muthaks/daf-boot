package za.co.rmb.approvalforum.service;

/**
 * The DAF roles the caller holds, passed from the web layer into the agenda service so the
 * visibility rules (BR-024) live in one place and cannot be bypassed by calling a different
 * endpoint.
 *
 * <p>The roles themselves come from the AD groups mapped in
 * {@code ApprovalForumAuthResource.getAFGroups}. This type carries only the answer to "does the
 * caller hold this role", never the AD group names, so the policy stays independent of the
 * environment-specific group naming.</p>
 *
 * <p>Holding a role is never sufficient on its own: an oversight role is additionally scoped to
 * the committee the caller serves, or - for an approver - to items that have been submitted.
 * Ownership is what grants the right to change an item.</p>
 */
public class AgendaAccess {

    private final boolean secretary;
    private final boolean chairperson;
    private final boolean reviewer;
    private final boolean approver;

    public AgendaAccess(boolean secretary, boolean chairperson, boolean reviewer, boolean approver) {
        this.secretary = secretary;
        this.chairperson = chairperson;
        this.reviewer = reviewer;
        this.approver = approver;
    }

    /** No oversight at all - an analyst sees only what they own. */
    public static AgendaAccess none() {
        return new AgendaAccess(false, false, false, false);
    }

    public boolean isSecretary() {
        return secretary;
    }

    public boolean isChairperson() {
        return chairperson;
    }

    public boolean isReviewer() {
        return reviewer;
    }

    public boolean isApprover() {
        return approver;
    }

    /** True when the caller holds at least one role that can check another analyst's item. */
    public boolean hasAnyOversight() {
        return secretary || chairperson || reviewer || approver;
    }

    @Override
    public String toString() {
        return "AgendaAccess{secretary=" + secretary + ", chairperson=" + chairperson
                + ", reviewer=" + reviewer + ", approver=" + approver + '}';
    }
}
