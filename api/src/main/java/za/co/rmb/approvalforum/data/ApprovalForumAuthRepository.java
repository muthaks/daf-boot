package za.co.rmb.approvalforum.data;

import za.co.rmb.approvalforum.data.entity.ApprovalForumAduActiveDirectoryUserEntity;
import za.co.rmb.approvalforum.model.UserDetails;

import java.util.List;

public interface ApprovalForumAuthRepository {

    void createUpdateActiveDirectoryUser(String alias, String displayName, String givenName, String surname, String mail);

    UserDetails getUserDetails(String username);

    ApprovalForumAduActiveDirectoryUserEntity getActiveDirectoryUserByAliasIgnoreCase(String alias);

    ApprovalForumAduActiveDirectoryUserEntity getActiveDirectoryUserByMailIgnoreCase(String mail);

    String getActiveDirectoryUserDisplayNameByAliasIgnoreCase(String alias);

    void deleteActiveDirectoryUserByDisplayNameAndAlias(String displayName, String alias);

    ApprovalForumAduActiveDirectoryUserEntity getActiveDirectoryUserByDisplayNameAndAlias(String displayName, String alias);

    List<ApprovalForumAduActiveDirectoryUserEntity> getAllActiveDirectoryUsers();

}
