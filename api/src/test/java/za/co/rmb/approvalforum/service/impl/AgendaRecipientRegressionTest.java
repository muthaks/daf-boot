package za.co.rmb.approvalforum.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import za.co.rmb.approvalforum.data.ApprovalForumAgendaRepository;
import za.co.rmb.approvalforum.model.agenda.AgendaItemDTO;
import za.co.rmb.approvalforum.service.ApprovalForumAuthService;
import za.co.rmb.credit.authservice.model.ActiveDirectoryUserModel;
import za.co.rmb.credit.mailservice.model.MailModel;
import za.co.rmb.credit.mailservice.web.client.MailServiceClient;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** G22 uses real recipient construction and mocked AD/mail clients. No email is sent. */
class AgendaRecipientRegressionTest {
    @Test
    void G22_submissionResolvesReviewerAndSecretaryUsernamesToTheirDirectoryMailAddresses() throws Exception {
        MailServiceClient client = mock(MailServiceClient.class);
        ApprovalForumAuthService directory = mock(ApprovalForumAuthService.class);
        Map<String, String> settings = new HashMap<>();
        settings.put("af.mail.enabled", "true");
        settings.put("af.mail_environment", "PROD");
        settings.put("af.mailservice.token", "fixture-only");
        settings.put("af.agenda.ui.base", "https://rmb.co.za/platform/agenda-builder");
        for (String[] person : new String[][]{{"reviewer.user", "review.analyst@rmb.co.za"},
                {"secretary.user", "meeting.secretary@rmb.co.za"}, {"analyst.user", "credit.analyst@rmb.co.za"}}) {
            ActiveDirectoryUserModel record = new ActiveDirectoryUserModel(); record.setMail(person[1]);
            when(directory.getUserDetailFromAd(person[0])).thenReturn(record);
        }
        AgendaMailServiceImpl mail = new AgendaMailServiceImpl(client, directory, mock(ApprovalForumAgendaRepository.class), settings);
        AgendaItemDTO item = new AgendaItemDTO();
        item.setAgiId(100); item.setCreatedBy("analyst.user"); item.setPrimaryAnalystId("analyst.user");
        item.setReviewAnalystId("reviewer.user"); item.setCounterpartyName("Example Holdings");
        mail.sendItemSubmittedMail(item, "reviewer.user;secretary.user");
        ArgumentCaptor<MailModel> envelope = ArgumentCaptor.forClass(MailModel.class);
        verify(client).Send(eq("fixture-only"), envelope.capture());
        Set<String> recipients = new HashSet<>(Arrays.asList(envelope.getValue().getTo().split(";")));
        assertEquals(Set.of("review.analyst@rmb.co.za", "meeting.secretary@rmb.co.za", "credit.analyst@rmb.co.za"), recipients);
        verify(directory, atLeastOnce()).getUserDetailFromAd("reviewer.user");
        verify(directory, atLeastOnce()).getUserDetailFromAd("secretary.user");
    }
}
