package za.co.rmb.approvalforum.service;

import za.co.rmb.approvalforum.model.*;

public interface ApprovalForumDocumentsService {

    WorkflowDocumentListResponse getWorkflowDocuments(String workflowId);

    WorkflowDocumentUploadResponse uploadDocument(UploadDocForm newForm);

    ApplicationDocumentModel downloadDocument(String documentId);

    WorkflowDocumentArchiveResponse archiveDocument(String documentId);

    void cloneDocuments(int oldWorkflowId, int newWorkflowId);

}
