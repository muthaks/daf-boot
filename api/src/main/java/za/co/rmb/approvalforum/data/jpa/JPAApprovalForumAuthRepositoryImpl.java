package za.co.rmb.approvalforum.data.jpa;

import jakarta.inject.Inject;
import za.co.rmb.approvalforum.data.ApprovalForumAuthRepository;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAduActiveDirectoryUserEntity;
import za.co.rmb.approvalforum.model.ActiveDirectoryUser;
import za.co.rmb.approvalforum.model.UserDetails;

import jakarta.persistence.*;
import java.util.List;

public class JPAApprovalForumAuthRepositoryImpl implements ApprovalForumAuthRepository {

    private final EntityManager em;

    @Inject
    public JPAApprovalForumAuthRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public void createUpdateActiveDirectoryUser(String alias, String displayName, String givenName, String surname, String mail) {
        StoredProcedureQuery storedProcedure = em
                .createStoredProcedureQuery("dbo.sp_CreateUpdateActiveDirectoryUser")
                .registerStoredProcedureParameter(1, String.class, ParameterMode.IN).setParameter(1, alias)
                .registerStoredProcedureParameter(2, String.class, ParameterMode.IN).setParameter(2, displayName)
                .registerStoredProcedureParameter(3, String.class, ParameterMode.IN).setParameter(3, givenName)
                .registerStoredProcedureParameter(4, String.class, ParameterMode.IN).setParameter(4, surname)
                .registerStoredProcedureParameter(5, String.class, ParameterMode.IN).setParameter(5, mail);

        storedProcedure.execute();
    }

    @Override
    public UserDetails getUserDetails(String username) {
        UserDetails result = new UserDetails();

        StoredProcedureQuery storedProcedure = em
                .createStoredProcedureQuery("dbo.sp_GetActiveDirectoryUser", ActiveDirectoryUser.class)
                .registerStoredProcedureParameter(1, String.class, ParameterMode.IN).setParameter(1, username);

        ActiveDirectoryUser adUser = (ActiveDirectoryUser)storedProcedure.getResultList().get(0);

        result.setMnemonic(adUser.getAlias());
        result.setName(adUser.getGivenName());
        result.setSurname(adUser.getSurname());
        result.setEmail(adUser.getMail());

        return result;
    }

    @Override
    public ApprovalForumAduActiveDirectoryUserEntity getActiveDirectoryUserByAliasIgnoreCase(String alias) {
        try {
            List<ApprovalForumAduActiveDirectoryUserEntity> results = em.createQuery("from ApprovalForumAduActiveDirectoryUserEntity t " +
                            "where lower(t.alias) = :alias ",
                    ApprovalForumAduActiveDirectoryUserEntity.class).setParameter("alias", alias.toLowerCase())
                    .setMaxResults(1)
                    .getResultList();
            return (results != null && !results.isEmpty()) ? results.get(0) : null;
        }catch(NoResultException e){
            return null;
        }
    }

    @Override
    public ApprovalForumAduActiveDirectoryUserEntity getActiveDirectoryUserByMailIgnoreCase(String mail) {
        try {
            return em.createQuery("from ApprovalForumAduActiveDirectoryUserEntity t " +
                                    "where lower(t.mail) = :mail ",
                            ApprovalForumAduActiveDirectoryUserEntity.class).setParameter("mail", mail.toLowerCase())
                    .getSingleResult();
        }catch(NoResultException e){
            return null;
        }
    }

    @Override
    public String getActiveDirectoryUserDisplayNameByAliasIgnoreCase(String alias) {
        try {
            List<?> results = em.createNativeQuery
                    ("select t.displayName from ADU_ActiveDirectoryUser t where lower(t.alias) = :dbAlias")
                    .setParameter("dbAlias", alias.toLowerCase())
                    .setMaxResults(1)
                    .getResultList();
            return (results != null && !results.isEmpty()) ? (String) results.get(0) : null;
        }catch(NoResultException e){
            return null;
        }
    }

    @Override
    public void deleteActiveDirectoryUserByDisplayNameAndAlias(String displayName, String alias) {

        em.createQuery("delete from ApprovalForumAduActiveDirectoryUserEntity t " +
                                "where lower(t.displayName) = :displayName " +
                                "and lower(t.alias) = :alias ", ApprovalForumAduActiveDirectoryUserEntity.class)
                .setParameter("displayName", displayName.toLowerCase())
                .setParameter("alias", alias.toLowerCase())
                .executeUpdate();

    }

    @Override
    public ApprovalForumAduActiveDirectoryUserEntity getActiveDirectoryUserByDisplayNameAndAlias(String displayName, String alias) {
        try {
            return em.createQuery("from ApprovalForumAduActiveDirectoryUserEntity t " +
                                    "where lower(t.displayName) = :displayName " +
                                    "and lower(t.alias) = :alias ",
                            ApprovalForumAduActiveDirectoryUserEntity.class)
                    .setParameter("displayName", displayName.toLowerCase())
                    .setParameter("alias", alias.toLowerCase())
                    .getSingleResult();
        }catch(NoResultException e){
            return null;
        }
    }

    @Override
    public List<ApprovalForumAduActiveDirectoryUserEntity> getAllActiveDirectoryUsers() {
        try {
            return em.createQuery("from ApprovalForumAduActiveDirectoryUserEntity t ",
                            ApprovalForumAduActiveDirectoryUserEntity.class)
                    .getResultList();
        }catch(NoResultException e){
            return null;
        }
    }
}
