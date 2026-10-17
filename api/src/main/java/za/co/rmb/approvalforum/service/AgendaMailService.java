package za.co.rmb.approvalforum.service;

import za.co.rmb.approvalforum.model.agenda.AgendaItemDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaMeetingDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaQuorumResultDTO;

public interface AgendaMailService {

    /** The analysts on the item and the committee's meeting secretary (user ids, ';'-separated). */
    void sendItemCapturedMail(AgendaItemDTO item, String secretaryIds);

    void sendItemConfirmedMail(AgendaItemDTO item);

    void sendItemRemovedMail(AgendaItemDTO item);

    void sendItemReminderMail(AgendaItemDTO item, java.sql.Timestamp cutOff, int daysToCutOff);

    void sendPreliminaryAgendaMail(AgendaMeetingDTO meeting, String chairEmail);

    void sendFinalAgendaMail(AgendaMeetingDTO meeting, String recipientEmails);

    // ---- BRS alignment: workflow and meeting lifecycle notifications ----

    void sendItemSubmittedMail(AgendaItemDTO item, String reviewerEmails);

    void sendItemReviewedMail(AgendaItemDTO item, String approverIds);

    void sendItemReturnedMail(AgendaItemDTO item, String reason);

    void sendItemApprovedMail(AgendaItemDTO item);

    void sendItemRejectedMail(AgendaItemDTO item, String reason);

    /**
     * An item in review changed: its meeting date, its quorum roster or a new document. `recipientIds`
     * are user ids (the reviewer and the committee secretary); the analysts are copied.
     */
    void sendItemChangedMail(AgendaItemDTO item, String recipientIds, String change, String detail);

    /** The analyst asks for a quorum exception: to the committee's secretary and chair (user ids), analysts copied. */
    void sendQuorumExceptionRequestedMail(AgendaItemDTO item, String deciderIds, AgendaQuorumResultDTO quorum);

    /** The secretary or chair approved or declined the quorum exception: to the analysts. */
    void sendQuorumExceptionDecidedMail(AgendaItemDTO item, boolean approved, String note);

    void sendMeetingCancelledMail(AgendaMeetingDTO meeting, String recipientEmails, String reason);

    void sendMeetingClosedMail(AgendaMeetingDTO meeting, String recipientEmails);

}
