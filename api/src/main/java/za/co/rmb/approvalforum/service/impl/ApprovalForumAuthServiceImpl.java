package za.co.rmb.approvalforum.service.impl;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.data.ApprovalForumAuthRepository;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAduActiveDirectoryUserEntity;
import za.co.rmb.approvalforum.model.UserDetails;
import za.co.rmb.approvalforum.service.ApprovalForumAuthService;
import za.co.rmb.approvalforum.service.exception.ResourceException;
import za.co.rmb.credit.authservice.model.ActiveDirectoryUserModel;
import za.co.rmb.credit.authservice.web.client.AuthServiceClient;

public class ApprovalForumAuthServiceImpl implements ApprovalForumAuthService {
    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumAuthServiceImpl.class);

    private final ApprovalForumAuthRepository approvalForumAuthRepository;

    @Inject
    private AuthServiceClient authClient;

    @Inject
    public ApprovalForumAuthServiceImpl(ApprovalForumAuthRepository approvalForumAuthRepository) {
        this.approvalForumAuthRepository = approvalForumAuthRepository;
    }

    @Override
    public void createUpdateActiveDirectoryUser(String alias, String displayName, String givenName, String surname, String mail) {
        approvalForumAuthRepository.createUpdateActiveDirectoryUser(alias, displayName, givenName, surname, mail);
    }

    @Override
    public UserDetails getUserDetailsByAliasIgnoreCase(String alias) {
        ApprovalForumAduActiveDirectoryUserEntity entity = approvalForumAuthRepository.getActiveDirectoryUserByAliasIgnoreCase(alias);
        if(entity == null && alias.indexOf("@") > 0){
            entity = approvalForumAuthRepository.getActiveDirectoryUserByAliasIgnoreCase(alias.substring(0, alias.indexOf("@")));
        }
        if(entity == null) return null;
        UserDetails userDetails = new UserDetails();
        userDetails.setEmail(entity.getMail());
        userDetails.setMnemonic(entity.getAlias());
//        userDetails.setName(entity.getGivenName());
        userDetails.setName(entity.getDisplayName());
        userDetails.setSurname(entity.getSurname());
        userDetails.setDisplayName(entity.getDisplayName());

        logger.info("*** userDetails.Name: {}, entity.GivenName: {}, entity.DisplayName: {}", userDetails.getName(),
                entity.getGivenName(), entity.getDisplayName());

        return userDetails;
    }

    @Override
    public String getActiveDirectoryUserDisplayNameByAliasIgnoreCase(String alias) {
        String displayName = approvalForumAuthRepository.getActiveDirectoryUserDisplayNameByAliasIgnoreCase(alias);
        if(displayName == null || "".equalsIgnoreCase(displayName.trim())) return alias;
        return displayName;
    }

    @Override
    public ActiveDirectoryUserModel getUserDetailFromAd(String userId) throws ResourceException {
        if(userId.length() == 3){
            return authClient.User("RMB", userId);
        }else if(userId.toLowerCase().startsWith("f")){
            return authClient.User("FNB", userId);
        }else if(userId.toLowerCase().startsWith("n")){
            return authClient.User("FNB Namibia", userId);
        }else if(userId.toLowerCase().startsWith("b")){
            return authClient.User("FNB Botswana", userId);
        }else if(userId.toLowerCase().startsWith("m")){
            return authClient.User("FNB Mozambique", userId);
        }else if(userId.toLowerCase().startsWith("w")){
            return authClient.User("Wesbank", userId);
        }else{
            throw new ResourceException("Could not determine domain from user_id " + userId);
        }
    }

}
