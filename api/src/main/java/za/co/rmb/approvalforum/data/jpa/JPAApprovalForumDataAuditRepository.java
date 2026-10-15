package za.co.rmb.approvalforum.data.jpa;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import za.co.rmb.approvalforum.data.ApprovalForumDataAuditRepository;
import za.co.rmb.approvalforum.data.entity.ApprovalForumRiRevisionInfoEntity;
import za.co.rmb.approvalforum.data.entity.ApprovalForumWfWorkflowEntity;
import za.co.rmb.approvalforum.data.entity.ApprovalForumWfaWorkflowApprovalsEntity;
import za.co.rmb.approvalforum.model.*;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import java.time.LocalDateTime;
import java.util.*;

public class JPAApprovalForumDataAuditRepository implements ApprovalForumDataAuditRepository {

    private final AuditReader auditReader;

    @Inject
    public JPAApprovalForumDataAuditRepository(EntityManager em) {
        this.auditReader = AuditReaderFactory.get(em);
    }

    @Override
    public List<Revision> getWorkflowRevisions(int workflowId) {
        List<Revision> revisions = new ArrayList<>();
        int displayRevision = 1;
        for (Number rev : auditReader.getRevisions(ApprovalForumWfWorkflowEntity.class, workflowId)) {
            revisions.add(getApprovalForumEntityRevisionInfo(rev, displayRevision, ApprovalForumWfWorkflowEntity.class));
            displayRevision++;
        }
        return revisions;
    }

    @Override
    public List<Revision> getWorkflowApprovalRevisions(int workflowId) {
        List<Revision> revisions = new ArrayList<>();
        int displayRevision = 1;
        for (Number rev : auditReader.getRevisions(ApprovalForumWfaWorkflowApprovalsEntity.class, workflowId)) {
            revisions.add(getApprovalForumEntityRevisionInfo(rev, displayRevision, ApprovalForumWfaWorkflowApprovalsEntity.class));
            displayRevision++;
        }
        return revisions;
    }

    @Override
    public ApprovalForumWfWorkflowEntity getWorkflowRevision(int workflowId, int revisionId) {
        return auditReader.find(ApprovalForumWfWorkflowEntity.class, workflowId, revisionId);
    }

    @Override
    public ApprovalForumWfaWorkflowApprovalsEntity getWorkflowApprovalRevision(int workflowId, int revisionId) {
        return auditReader.find(ApprovalForumWfaWorkflowApprovalsEntity.class, workflowId, revisionId);
    }

    private Revision getApprovalForumEntityRevisionInfo(Number rev, int displayRevision, Class c){
        org.hibernate.envers.RevisionType revisionType = null;
        ApprovalForumRiRevisionInfoEntity entity = null;
        List objects =
                auditReader.createQuery().forRevisionsOfEntity
                        (c, c.getName(), false, true)
                        .add(AuditEntity.revisionNumber().eq(rev)).getResultList();
        Object[] objects1 = (Object[])objects.get(0);
        if(objects1 != null && objects1.length > 0) {
            for (Object object : objects1) {
                if(object instanceof ApprovalForumRiRevisionInfoEntity){
                    entity = (ApprovalForumRiRevisionInfoEntity) object;
                }else if(object instanceof org.hibernate.envers.RevisionType){
                    revisionType = (org.hibernate.envers.RevisionType) object;
                }
            }
        }

        if(revisionType != null && entity != null) {
            Calendar revisionTime = Calendar.getInstance();
            revisionTime.setTimeInMillis(entity.getRevTstmp());
            Revision revision = new Revision();
            revision.setDisplayRevision(displayRevision);
            revision.setInternalRevision(rev.intValue());
            revision.setRevisionDate(getXmlGregorianCalendarFromTimestamp(revisionTime.getTime()));
            revision.setUserId(entity.getUserName());
            revision.setDescription(entity.getDescription());
            revision.setType(RevisionType.fromValue(revisionType.name()));
            return revision;
        }
        return null;
    }

    private LocalDateTime getXmlGregorianCalendarFromTimestamp(Date timestamp){
        if(timestamp == null) return null;
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(timestamp);
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gc).toGregorianCalendar().toZonedDateTime().toLocalDateTime();
        }catch(DatatypeConfigurationException e){
            return null;
        }
    }

    @Override
    public void setAuditDetails(AuditDetails auditDetails) {
        ApprovalForumRiRevisionInfoEntity entity = auditReader.getCurrentRevision(ApprovalForumRiRevisionInfoEntity.class, false);
        entity.setUserName(auditDetails.getUser());
        entity.setDescription(auditDetails.getDescription());
        entity.setWorkflowId(auditDetails.getWorkflowId());
    }

    @Override
    public LastUpdateDetails getWorkflowLastUpdatedDetails(int workflowId) {
        Number lastRevision = null;
        for (Number rev : auditReader.getRevisions(ApprovalForumWfWorkflowEntity.class, workflowId)) {
            lastRevision = rev;
        }
        Revision rev = getApprovalForumEntityRevisionInfo(lastRevision, 0, ApprovalForumWfWorkflowEntity.class);
        if(rev != null && !rev.getType().equals(RevisionType.ADD)) {
            return getLastUpdatedDetails(rev);
        }
        return null;
    }

    @Override
    public LastUpdateDetails getWorkflowApprovalLastUpdatedDetails(int workflowId) {
        Number lastRevision = null;
        for (Number rev : auditReader.getRevisions(ApprovalForumWfaWorkflowApprovalsEntity.class, workflowId)) {
            lastRevision = rev;
        }
        Revision rev = getApprovalForumEntityRevisionInfo(lastRevision, 0, ApprovalForumWfaWorkflowApprovalsEntity.class);
        if(rev != null && !rev.getType().equals(RevisionType.ADD)) {
            return getLastUpdatedDetails(rev);
        }
        return null;
    }

    private LastUpdateDetails getLastUpdatedDetails(Revision rev){
        LastUpdateDetails lastUpdateDetails = new LastUpdateDetails();
        lastUpdateDetails.setLastUpdatedDate(rev.getRevisionDate());
        UserData userData = new UserData();
        userData.setUserId(rev.getUserId());
        lastUpdateDetails.setLastUpdatedUser(userData);
        return lastUpdateDetails;
    }

}
