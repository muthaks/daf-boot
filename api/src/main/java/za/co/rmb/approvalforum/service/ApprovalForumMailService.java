package za.co.rmb.approvalforum.service;

import za.co.rmb.approvalforum.model.ListManagementList;
import za.co.rmb.approvalforum.model.WorkflowMailDTO;
import za.co.rmb.approvalforum.service.exception.ResourceException;
import za.co.rmb.credit.mailservice.model.MailResponseModel;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;

public interface ApprovalForumMailService {

    MailResponseModel sendTestMail();

    MailResponseModel sendNewRequestMail(int workflowId);

    MailResponseModel sendRequestSubmitMail(int workflowId);

    MailResponseModel sendRequestSubmitToChairpersonMail(int workflowId, List<ListManagementList> chairpersonLists);

    MailResponseModel sendRequestCompleteMail(int workflowId);

    MailResponseModel sendUploadToDocstashSuccessMail(int workflowId);

    MailResponseModel sendUploadToDocstashFailMail(int workflowId, boolean onlyToSupport);

    MailResponseModel sendSlaReminderMail(int workflowId, boolean isFirstEscalation);

    MailResponseModel sendApproverSlaReminderMail(HashMap<String, HashMap<String, List<WorkflowMailDTO>>> notificationList);

    MailResponseModel sendCapturerSlaReminderMail(HashMap<String, List<WorkflowMailDTO>> notificationList);

    void sendUnsentMails();

    void resendDailyMails();

    MailResponseModel sendAssignToMeEmailNotification(int workflowId, String username, String message) throws UnsupportedEncodingException, ResourceException;

    MailResponseModel sendAssignToMeEmailNotificationFailure(int workflowId, String username, String message, String workflowIdStr);

}
