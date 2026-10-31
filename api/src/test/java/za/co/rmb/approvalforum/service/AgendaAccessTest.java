package za.co.rmb.approvalforum.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The caller's DAF roles as the web layer hands them to the agenda service. */
class AgendaAccessTest {

    @Test
    void noneHoldsNoRoleAndNoOversight() {
        AgendaAccess none = AgendaAccess.none();
        assertFalse(none.isSecretary());
        assertFalse(none.isChairperson());
        assertFalse(none.isReviewer());
        assertFalse(none.isApprover());
        assertFalse(none.hasAnyOversight());
    }

    @Test
    void anySingleRoleIsOversight() {
        assertTrue(new AgendaAccess(true, false, false, false).hasAnyOversight());
        assertTrue(new AgendaAccess(false, true, false, false).hasAnyOversight());
        assertTrue(new AgendaAccess(false, false, true, false).hasAnyOversight());
        assertTrue(new AgendaAccess(false, false, false, true).hasAnyOversight());
    }

    @Test
    void flagsAreReportedIndividuallyAndInToString() {
        AgendaAccess access = new AgendaAccess(true, false, true, false);
        assertTrue(access.isSecretary());
        assertFalse(access.isChairperson());
        assertTrue(access.isReviewer());
        assertFalse(access.isApprover());
        assertEquals("AgendaAccess{secretary=true, chairperson=false, reviewer=true, approver=false}", access.toString());
    }
}
