package za.co.rmb.approvalforum.service;

import za.co.rmb.approvalforum.service.exception.ResourceException;

public interface ApprovalForumDocstashService {

    int uploadDocumentsToDocstash(int workflowId) throws ResourceException;

    void processDocStashRequests() throws ResourceException;

    void processAduCleanup() throws ResourceException;

}
