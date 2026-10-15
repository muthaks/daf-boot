package za.co.rmb.approvalforum.service;

import za.co.rmb.approvalforum.model.CounterpartyFullDetail;
import za.co.rmb.approvalforum.model.MeetingName;
import za.co.rmb.approvalforum.model.lumos.WorkflowResponseDTO;
import za.co.rmb.approvalforum.service.exception.ResourceException;

import java.util.List;

/**
 * @author KHG
 */
public interface LumosRestService {
    CounterpartyFullDetail getLumosWorkflow(int workflowID);

    WorkflowResponseDTO findLumosWorkflowById(int workflowId);

    List<MeetingName> getBroaderAfricaMeetingNamesForTenantId(int tenantId);

    boolean processLumosBroaderAfricaWorkflow(String inkombaId, String meetingName, int decisionId, String specificReason) throws ResourceException;
}
