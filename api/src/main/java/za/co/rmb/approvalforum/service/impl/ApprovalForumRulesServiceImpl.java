package za.co.rmb.approvalforum.service.impl;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.service.ApprovalForumRulesService;
import za.co.rmb.approvalforum.service.exception.RulesException;
import za.co.rmb.approvalforumrules.model.ApprovalForumRulesClient;
import za.co.rmb.approvalforumrules.model.ApprovalForumRulesRequestDTO;
import za.co.rmb.approvalforumrules.model.ApprovalForumRulesResponseDTO;
import za.co.rmb.approvalforumrules.model.FacilityMovementDTO;

import java.util.List;

public class ApprovalForumRulesServiceImpl implements ApprovalForumRulesService {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumRulesServiceImpl.class);

    @Inject
    private ApprovalForumRulesClient rulesClient;


    @Override
    public ApprovalForumRulesResponseDTO executeApprovalForumRules(String meetingType, String distressStatus, String financialInstitutionType,
                                                                   boolean bank, List<String> riskTakerGroupCode, List<FacilityMovementDTO> facilityMovement, String proposalType) throws RulesException {
        logger.info("executeApprovalForumRules({}, {})", meetingType, distressStatus);
        ApprovalForumRulesRequestDTO requestDTO = new ApprovalForumRulesRequestDTO();
        requestDTO.setMeetingRule(meetingType);
        requestDTO.setProposalType(proposalType);
        requestDTO.setDistressStatus(distressStatus);
        requestDTO.setFit(financialInstitutionType);
        requestDTO.setBank(bank);
        requestDTO.setRiskTakerGroupCode(riskTakerGroupCode);
        requestDTO.setFacilityMovement(facilityMovement);
        ApprovalForumRulesResponseDTO responseDTO = rulesClient.processApprovalForumRules(requestDTO);
        if(!responseDTO.isValid()){
            throw new RulesException("Rules not defined for MeetingType:MEETINGTYPE, DistressStatus:DISTRESSSTATUS"
                    .replaceAll("MEETINGTYPE", meetingType)
                    .replaceAll("DISTRESSSTATUS", distressStatus)
                    .replaceAll("FININSTTYPE", financialInstitutionType));
        }
        return responseDTO;
    }

}
