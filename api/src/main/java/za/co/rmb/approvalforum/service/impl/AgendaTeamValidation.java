package za.co.rmb.approvalforum.service.impl;

import java.util.Locale;
import za.co.rmb.approvalforum.model.agenda.AgendaItemDTO;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAgiAgendaItemEntity;

/** Separation of duties is enforced on the server, independently of picker restrictions. */
public final class AgendaTeamValidation {
    private AgendaTeamValidation() { }
    private static final String[] LABELS = { "Primary analyst", "Review analyst", "Credit sponsor", "BU head" };

    public static String accountKey(String value) {
        if (value == null) return "";
        String key = value.trim().toLowerCase(Locale.ROOT);
        if (key.startsWith("rmb\\")) key = key.substring(4);
        if (key.endsWith("@rmb.co.za")) key = key.substring(0, key.length() - "@rmb.co.za".length());
        return key;
    }
    public static boolean sameAccount(String a, String b) {
        return !accountKey(a).isEmpty() && accountKey(a).equals(accountKey(b));
    }
    public static void validate(AgendaItemDTO item, String bookingAnalyst) {
        if (item == null) throw new IllegalStateException("Agenda item is required");
        validate(new String[] { item.getPrimaryAnalyst(), item.getReviewAnalyst(), item.getCreditSponsor(), item.getBuHead() },
            new String[] { item.getPrimaryAnalystId(), item.getReviewAnalystId(), item.getCreditSponsorId(), item.getBuHeadId() }, bookingAnalyst);
    }
    public static void validate(ApprovalForumAgiAgendaItemEntity item) {
        validate(new String[] { item.getPrimaryAnalyst(), item.getReviewAnalyst(), item.getCreditSponsor(), item.getBuHead() },
            new String[] { item.getPrimaryAnalystId(), item.getReviewAnalystId(), item.getCreditSponsorId(), item.getBuHeadId() }, item.getCreatedBy());
    }
    private static void validate(String[] names, String[] ids, String bookingAnalyst) {
        if (names[1] == null || names[1].trim().isEmpty())
            throw new IllegalStateException("Select an independent review analyst before continuing");
        for (int i = 0; i < ids.length; i++) {
            if (names[i] != null && !names[i].trim().isEmpty() && accountKey(ids[i]).isEmpty())
                throw new IllegalStateException("Select the " + LABELS[i].toLowerCase(Locale.ROOT) + " from the directory to verify their account");
            for (int j = 0; j < i; j++) {
                // The sponsor and BU head may be the same person; analysts remain independent.
                if (!(i == 3 && j == 2) && sameAccount(ids[i], ids[j]))
                    throw new IllegalStateException(LABELS[j] + " and " + LABELS[i].toLowerCase(Locale.ROOT) + " must be different people");
            }
        }
        if (sameAccount(ids[1], bookingAnalyst))
            throw new IllegalStateException("The booking analyst cannot review their own item. Choose an independent review analyst");
    }
    public static void requireIndependentReviewer(ApprovalForumAgiAgendaItemEntity item, String actor) {
        if (sameAccount(item.getPrimaryAnalystId(), actor) || sameAccount(item.getCreatedBy(), actor))
            throw new IllegalStateException("You cannot review, approve or validate your own agenda item. An independent reviewer is required");
    }
}
