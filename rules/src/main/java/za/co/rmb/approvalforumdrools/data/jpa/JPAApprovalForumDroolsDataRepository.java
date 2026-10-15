package za.co.rmb.approvalforumdrools.data.jpa;

import za.co.rmb.approvalforumdrools.data.ApprovalForumDroolsDataRepository;
import za.co.rmb.approvalforumdrools.data.entity.ApprovalForumRRulesEntity;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.util.List;

public class JPAApprovalForumDroolsDataRepository implements ApprovalForumDroolsDataRepository {

    private final EntityManager em;

    @Inject
    public JPAApprovalForumDroolsDataRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public ApprovalForumRRulesEntity getActiveRules() {
        try {
            return em.createQuery("from ApprovalForumRRulesEntity t " +
                            "where t.active = :active",
                    ApprovalForumRRulesEntity.class)
                    .setParameter("active", true)
                    .getSingleResult();
        }catch(NoResultException e){
            return null;
        }
    }

    @Override
    public void updateRules(ApprovalForumRRulesEntity entity) {
        em.merge(entity);
    }

    @Override
    public void persistRules(ApprovalForumRRulesEntity entity) {
        em.persist(entity);
    }

    @Override
    public List<ApprovalForumRRulesEntity> getRulesByFileName(String fileName) {
        try {
            return em.createQuery("from ApprovalForumRRulesEntity t " +
                            "where t.fileName = :fileName",
                    ApprovalForumRRulesEntity.class)
                    .setParameter("fileName", fileName)
                    .getResultList();
        }catch(NoResultException e){
            return null;
        }
    }

}

