package za.co.rmb.approvalforum.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import za.co.rmb.approvalforum.data.ApprovalForumAgendaRepository;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAgnAgendaNotificationEntity;
import za.co.rmb.approvalforum.model.agenda.AgendaItemDTO;
import za.co.rmb.approvalforum.service.ApprovalForumAuthService;
import za.co.rmb.credit.mailservice.model.MailModel;
import za.co.rmb.credit.mailservice.web.client.MailServiceClient;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Captures the request to a mocked mail client; these checks send no email. */
class AgendaMailServiceImplTest {
    @Test
    void returnedEmailContainsVisibleEscapedReasonNextStepsAndItemLink() throws Exception {
        MailServiceClient client = mock(MailServiceClient.class);
        ApprovalForumAgendaRepository repository = mock(ApprovalForumAgendaRepository.class);
        Map<String, String> settings = new HashMap<>();
        settings.put("af.mail.enabled", "true"); settings.put("af.mail_environment", "PROD");
        settings.put("af.mailservice.token", "fixture-only"); settings.put("af.agenda.ui.base", "https://agenda.example.test/platform/agenda-builder");
        AgendaMailServiceImpl service = new AgendaMailServiceImpl(client, mock(ApprovalForumAuthService.class), repository, settings);
        AgendaItemDTO item = new AgendaItemDTO(); item.setAgiId(123); item.setCreatedBy("analyst@example.test"); item.setCounterpartyName("Example & Co");
        service.sendItemReturnedMail(item, "Correct <rating> & figures\nUpload the revised PDF.");
        ArgumentCaptor<MailModel> email = ArgumentCaptor.forClass(MailModel.class);
        verify(client).Send(eq("fixture-only"), email.capture());
        String body = email.getValue().getBody();
        assertTrue(body.contains("Reason for return"));
        assertTrue(body.contains("Correct &lt;rating&gt; &amp; figures<br/>Upload the revised PDF."));
        assertTrue(body.contains("What to do next")); assertTrue(body.contains("submit the item for review again"));
        assertTrue(body.contains("https://agenda.example.test/platform/agenda-builder/agenda/item/123"));
        assertTrue(body.contains("Example &amp; Co")); assertFalse(body.contains("<rating>"));
    }

    @Test
    void disabledDeliveryRecordsAnAttemptWithoutCallingTheMailClient() {
        MailServiceClient client = mock(MailServiceClient.class);
        ApprovalForumAgendaRepository repository = mock(ApprovalForumAgendaRepository.class);
        Map<String, String> settings = new HashMap<>(); settings.put("af.mail.enabled", "false");
        AgendaMailServiceImpl service = new AgendaMailServiceImpl(client, mock(ApprovalForumAuthService.class), repository, settings);
        AgendaItemDTO item = new AgendaItemDTO(); item.setAgiId(123); item.setCreatedBy("analyst@example.test");
        service.sendItemReturnedMail(item, "Correct the paper");
        verifyNoInteractions(client);
        ArgumentCaptor<ApprovalForumAgnAgendaNotificationEntity> notification = ArgumentCaptor.forClass(ApprovalForumAgnAgendaNotificationEntity.class);
        verify(repository).saveNotification(notification.capture());
        assertEquals("Disabled", notification.getValue().getStatus()); assertEquals("Returned", notification.getValue().getNotificationType());
    }
}
