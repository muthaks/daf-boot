package za.co.rmb.approvalforum.service.impl;

import jakarta.inject.Inject;
import za.co.rmb.approvalforum.data.ApprovalForumDocumentsRepository;
import za.co.rmb.approvalforum.model.*;
import za.co.rmb.approvalforum.service.ApprovalForumDocumentsService;

public class ApprovalForumDocumentsServiceImpl implements ApprovalForumDocumentsService {

    private final ApprovalForumDocumentsRepository approvalForumDocumentsRepository;

    @Inject
    public ApprovalForumDocumentsServiceImpl(ApprovalForumDocumentsRepository approvalForumDocumentsRepository) {
        this.approvalForumDocumentsRepository = approvalForumDocumentsRepository;
    }

    @Override
    public WorkflowDocumentListResponse getWorkflowDocuments(String workflowId) {
        return approvalForumDocumentsRepository.getWorkflowDocuments(workflowId);
    }

    @Override
    public WorkflowDocumentUploadResponse uploadDocument(UploadDocForm newForm) {
        return approvalForumDocumentsRepository.uploadDocument(newForm);
    }

    @Override
    public ApplicationDocumentModel downloadDocument(String documentId) {
        return approvalForumDocumentsRepository.downloadDocument(documentId);
    }

    @Override
    public WorkflowDocumentArchiveResponse archiveDocument(String documentId) {
        return approvalForumDocumentsRepository.archiveDocument(documentId);
    }

    @Override
    public void cloneDocuments(int oldWorkflowId, int newWorkflowId) {
        approvalForumDocumentsRepository.cloneDocuments(oldWorkflowId, newWorkflowId);
    }

}