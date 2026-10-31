package za.co.rmb.approvalforum.service.impl;

import org.junit.jupiter.api.Test;
import za.co.rmb.approvalforum.data.ApprovalForumAgendaRepository;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAgidAgendaItemDocumentEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** The credit paper that travels with an item's mails and invitation: current, active credit papers only. */
class AgendaMeetingPapersTest {

    private final ApprovalForumAgendaRepository repository = mock(ApprovalForumAgendaRepository.class);

    private static ApprovalForumAgidAgendaItemDocumentEntity doc(String type, String name, byte[] data, String status, boolean active) {
        ApprovalForumAgidAgendaItemDocumentEntity d = new ApprovalForumAgidAgendaItemDocumentEntity();
        d.setDocType(type);
        d.setFileName(name);
        d.setFileData(data);
        d.setValidationStatus(status);
        d.setActive(active);
        return d;
    }

    @Test
    void noItemMeansNoPapersAndNoLookup() {
        assertTrue(MeetingPapers.creditPapers(repository, null).isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void onlyCurrentActiveCreditPapersWithContentAreAttached() {
        when(repository.getDocumentsForItem(7)).thenReturn(Arrays.asList(
                doc("credit paper", "v2.pdf", new byte[] { 2 }, "Pending", true),
                doc("Credit Paper", "v1.pdf", new byte[] { 1 }, "superseded", true),
                doc("Credit Paper", "empty.pdf", null, "Accepted", true),
                doc("Credit Paper", "deleted.pdf", new byte[] { 3 }, "Accepted", false),
                doc("Term Sheet", "terms.pdf", new byte[] { 4 }, "Accepted", true),
                doc(null, "untyped.pdf", new byte[] { 5 }, "Accepted", true),
                doc("Credit Paper", "rejected.pdf", new byte[] { 6 }, "Rejected", true)));
        List<MeetingPapers.Paper> papers = MeetingPapers.creditPapers(repository, 7);
        assertEquals(Arrays.asList("v2.pdf", "rejected.pdf"), papers.stream().map(MeetingPapers.Paper::getFileName).collect(java.util.stream.Collectors.toList()));
        assertArrayEquals(new byte[] { 2 }, papers.get(0).getData());
    }

    @Test
    void aRepositoryFailureMeansNoAttachmentRatherThanNoMail() {
        when(repository.getDocumentsForItem(7)).thenThrow(new RuntimeException("db down"));
        assertTrue(MeetingPapers.creditPapers(repository, 7).isEmpty());
    }
}
