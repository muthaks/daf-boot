package za.co.rmb.approvalforumdrools.web.resources.util;

import jakarta.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforumdrools.engine.ApprovalForumDroolsEngine;
import za.co.rmb.approvalforumdrools.rules.approvalforum.data.ApprovalForumData;
import za.co.rmb.approvalforumdrools.service.ApprovalForumDroolsService;
import za.co.rmb.approvalforumrules.model.*;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class ApprovalForumDroolsResourceUtil {

    @Inject
    ApprovalForumDroolsEngine approvalForumDroolsEngine;

    @Inject
    private ApprovalForumDroolsService approvalForumDroolsService;

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumDroolsResourceUtil.class);


    protected Response processApprovalForumRules(ApprovalForumRulesRequestDTO requestDTO) {
        ApprovalForumData data = new ApprovalForumData();
        data.setMeetingRule(requestDTO.getMeetingRule());
        data.setProposalType(requestDTO.getProposalType());
        data.setDistressStatus(requestDTO.getDistressStatus());
        data.setBank(requestDTO.isBank());
        data.setFit(requestDTO.getFit());
        data.setRiskTakerGroupCode(requestDTO.getRiskTakerGroupCode());
        HashMap<String,String> facilityMovementMap = new HashMap<>();
        for(FacilityMovementDTO facilityMovement : requestDTO.getFacilityMovement()){
            if(!facilityMovementMap.containsKey(facilityMovement.getRiskTakerGroupCode())){
                facilityMovementMap.put(facilityMovement.getRiskTakerGroupCode(),facilityMovement.getMovement());
            }
        }
        data.setFacilityMovement(facilityMovementMap);
        try {
            approvalForumDroolsEngine.executeApprovalForumRules(data);
        } catch (Exception e) {
            logger.error("processApprovalForumRules threw exception", e);
            throw new InternalServerErrorException(e);
        }
        return Response.ok(mapApprovalForumResponseFromApprovalForumData(data)).build();
    }

    private ApprovalForumRulesResponseDTO mapApprovalForumResponseFromApprovalForumData(ApprovalForumData data) {
        ApprovalForumRulesResponseDTO responseDTO = new ApprovalForumRulesResponseDTO();
        if(data.isRuleExecuted()) {
            responseDTO.setValid(true);
            ApprovalForumRulesResponse response = new ApprovalForumRulesResponse().withModelData(new ArrayList<>());
            for(ApprovalForumData.ApprovalForumModelData modelData: data.getApprovalForumModelDataList()){
                ApprovalForumRulesModel model = new ApprovalForumRulesModel();
                model.setTotalNumberOfApprovalsRequired(modelData.getTotalNumberOfApprovalsRequired());
                List<ListModelData> modelDataList = new ArrayList<>();
                modelData.getListModelData().forEach(listModelData -> {
                    modelDataList.add(new ListModelData()
                            .withListName(listModelData.getListName())
                            .withMinRequired(listModelData.getMinRequired())
                            .withMaxRequired(listModelData.getMaxRequired())
                            .withRank(listModelData.getRank())
                    );
                });
                model.setListModelData(modelDataList);
                response.getModelData().add(model);
            }
            responseDTO.setResponseData(response);
        }else{
            responseDTO.setValid(false);
        }
        return responseDTO;
    }

    protected void processReloadRules(String md5sum, String path, String user) throws Exception{
        if(approvalForumDroolsService.hasFileBeenLoadedPreviously(path)){
            throw new Exception("File " + path + " has been loaded previously!");
        }
        approvalForumDroolsService.updateRules(getRules(path, md5sum), user, path, md5sum);
        approvalForumDroolsEngine.reloadRules(approvalForumDroolsService.getRules().getBytes());
    }

    private String getRules(String path, String md5sum) throws Exception{
        File file = new File(path);
        if(!file.exists()){
            throw new Exception("File '" + path + "' not found!");
        }
        if(!getMd5Checksum(file).equalsIgnoreCase(md5sum)){
            throw new Exception("Checksums do not match!");
        }

        String rules = "";
        List<String> lines = Files.readAllLines(file.toPath());
        for (String line : lines) {
            rules = rules + line + "\n";
        }
        return rules;
    }

    private String getMd5Checksum(File file) throws Exception{
        MessageDigest md = MessageDigest.getInstance("MD5");

        byte[] messageDigest = md.digest(IOUtils.toByteArray(Files.newInputStream(file.toPath())));

        return new BigInteger(1, messageDigest).toString(16);
    }

}
