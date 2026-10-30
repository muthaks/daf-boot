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
 * The credit paper that goes out with an item's notifications and its own calendar invitation. No
 * pack of every item's paper is built: nobody receives another item's paper (BR-013, NFR-001).
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
}
