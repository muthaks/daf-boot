package za.co.rmb.approvalforum.service;

import za.co.rmb.approvalforum.service.exception.RulesException;
import za.co.rmb.approvalforumrules.model.ApprovalForumRulesResponseDTO;
import za.co.rmb.approvalforumrules.model.FacilityMovementDTO;

import java.util.List;

public interface ApprovalForumRulesService {

    ApprovalForumRulesResponseDTO executeApprovalForumRules(String meetingType, String distressStatus, String financialInstitutionType,
                                                            boolean bank, List<String> riskTakerGroupCode, List<FacilityMovementDTO> facilityMovement, String proposalType) throws RulesException;

}

