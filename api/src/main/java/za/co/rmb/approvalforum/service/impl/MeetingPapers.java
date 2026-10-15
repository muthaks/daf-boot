package za.co.rmb.approvalforum.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.data.ApprovalForumAgendaRepository;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAgidAgendaItemDocumentEntity;
import za.co.rmb.approvalforum.model.agenda.AgendaItemDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaMeetingDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * The credit papers that go out with agenda notifications and the meeting invitation: an item's
 * current credit paper, and the meeting pack (the current paper of every approved item).
 */
final class MeetingPapers {

    private static final Logger logger = LoggerFactory.getLogger(MeetingPapers.class);

    /** A file to attach: its name and contents. */
    static final class Paper {
        private final String fileName;
        private final byte[] data;

        Paper(String fileName, byte[] data) {
            this.fileName = fileName;
            this.data = data;
        }

        String getFileName() { return fileName; }
        byte[] getData() { return data; }
    }

    private MeetingPapers() {
    }

    /** The item's uploaded credit paper (empty when none is uploaded); superseded versions are left out. */
    static List<Paper> creditPapers(ApprovalForumAgendaRepository agendaRepository, Integer agiId) {
        List<Paper> papers = new ArrayList<>();
        if (agiId == null) {
            return papers;
        }
        try {
            for (ApprovalForumAgidAgendaItemDocumentEntity doc : agendaRepository.getDocumentsForItem(agiId)) {
                if ("Credit Paper".equalsIgnoreCase(doc.getDocType()) && doc.getFileData() != null && doc.isActive()
                        && !"Superseded".equalsIgnoreCase(doc.getValidationStatus())) {
                    papers.add(new Paper(doc.getFileName(), doc.getFileData()));
                }
            }
        } catch (Exception ex) {
            logger.warn("Could not load the credit paper for item {}: {}", agiId, ex.getMessage());
        }
        return papers;
    }

    /** The current credit paper of every approved item, named "<agenda number> <counterparty> - <file>". */
    static List<Paper> meetingPack(ApprovalForumAgendaRepository agendaRepository, AgendaMeetingDTO meeting) {
        List<Paper> pack = new ArrayList<>();
        if (meeting.getItems() == null) {
            return pack;
        }
        for (AgendaItemDTO item : meeting.getItems()) {
            if (!"Approved".equalsIgnoreCase(item.getItemStatus())) {
                continue;
            }
            String prefix = (item.getAgendaNumber() == null ? "" : item.getAgendaNumber() + " ")
                    + (item.getCounterpartyName() == null ? "" : item.getCounterpartyName() + " - ");
            for (Paper paper : creditPapers(agendaRepository, item.getAgiId())) {
                pack.add(new Paper(prefix + paper.getFileName(), paper.getData()));
            }
        }
        return pack;
    }
}
