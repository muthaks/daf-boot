package za.co.rmb.approvalforum.service;

import za.co.rmb.approvalforum.model.agenda.AgendaItemDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaMeetingDTO;

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

    void sendItemReturnedMail(AgendaItemDTO item, String reason);

    void sendItemApprovedMail(AgendaItemDTO item);

    void sendItemRejectedMail(AgendaItemDTO item, String reason);

    void sendMeetingCancelledMail(AgendaMeetingDTO meeting, String recipientEmails, String reason);

    void sendMeetingClosedMail(AgendaMeetingDTO meeting, String recipientEmails);

}
