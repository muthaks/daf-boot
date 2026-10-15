package za.co.rmb.approvalforum.service;

import za.co.rmb.approvalforum.model.UserDetails;
import za.co.rmb.approvalforum.service.exception.ResourceException;
import za.co.rmb.credit.authservice.model.ActiveDirectoryUserModel;

public interface ApprovalForumAuthService {

    void createUpdateActiveDirectoryUser(String alias, String displayName, String givenName, String surname, String mail);

    String getActiveDirectoryUserDisplayNameByAliasIgnoreCase(String alias);

    UserDetails getUserDetailsByAliasIgnoreCase(String alias);

    ActiveDirectoryUserModel getUserDetailFromAd(String userId) throws ResourceException;

}
