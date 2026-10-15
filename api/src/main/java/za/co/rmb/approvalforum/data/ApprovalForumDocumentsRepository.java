package za.co.rmb.approvalforum.data;

import za.co.rmb.approvalforum.model.*;

public interface ApprovalForumDocumentsRepository {

    WorkflowDocumentListResponse getWorkflowDocuments(String workflowId);

    WorkflowDocumentUploadResponse uploadDocument(UploadDocForm newForm);

    ApplicationDocumentModel downloadDocument(String documentId);

    WorkflowDocumentArchiveResponse archiveDocument(String documentId);

    void cloneDocuments(int oldWorkflowId, int newWorkflowId);

}
