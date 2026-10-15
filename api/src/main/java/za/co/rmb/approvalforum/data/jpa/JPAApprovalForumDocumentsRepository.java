package za.co.rmb.approvalforum.data.jpa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.data.ApprovalForumAuthRepository;
import za.co.rmb.approvalforum.data.ApprovalForumDocumentsRepository;
import za.co.rmb.approvalforum.data.entity.ApprovalForumWfdWorkflowDocumentEntity;
import za.co.rmb.approvalforum.model.*;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import javax.sql.rowset.serial.SerialBlob;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JPAApprovalForumDocumentsRepository implements ApprovalForumDocumentsRepository {

    private static final Logger logger = LoggerFactory.getLogger(JPAApprovalForumDocumentsRepository.class);

    private final EntityManager em;

    @Inject
    private ApprovalForumAuthRepository approvalForumAuthRepository;

    @Inject
    JPAApprovalForumDocumentsRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public WorkflowDocumentListResponse getWorkflowDocuments(String workflowId) {
        WorkflowDocumentListResponse response = new WorkflowDocumentListResponse();
        response.setSuccess(false);

        List<ApplicationDocumentListModel> result = new ArrayList<>();
        List<ApprovalForumWfdWorkflowDocumentEntity> entities = em.createQuery("from ApprovalForumWfdWorkflowDocumentEntity t " +
                        "where t.workflowId = :workflowId " +
                        "and t.active = :active",
                ApprovalForumWfdWorkflowDocumentEntity.class)
                .setParameter("workflowId", Integer.parseInt(workflowId))
                .setParameter("active", true)
                .getResultList();

        entities.forEach(x -> {
            UserDetails user = approvalForumAuthRepository.getUserDetails(x.getCreatedBy());
            String fullName = x.getCreatedBy();
            if (!user.getName().equalsIgnoreCase(x.getCreatedBy())) {
                fullName = user.getName() + " " + user.getSurname();
            }

            ApplicationDocumentListModel listModel = new ApplicationDocumentListModel();
            listModel.setActive(x.getActive());
            listModel.setAsdID(x.getWfdId());
            listModel.setCreatedOn(x.getCreatedOn());
            listModel.setCreatedBy(x.getCreatedBy());
            listModel.setCreatedByDisplayName(fullName);
            listModel.setDocDescription(x.getDocDescription());
            listModel.setDocType(x.getDocDescription());
            listModel.setDocKey("");
            listModel.setFileExtension(x.getFileExtension());
            listModel.setFileName(x.getFilename());
            listModel.setFileSize(x.getFileSize());

            result.add(listModel);
        });
        response.setDocuments(result);
        response.setSuccess(true);

        return response;
    }

    @Override
    public WorkflowDocumentUploadResponse uploadDocument(UploadDocForm newForm) {
        WorkflowDocumentUploadResponse response = new WorkflowDocumentUploadResponse();
        response.setSuccess(false);

        System.out.println("Upload Doc 3: " + newForm.getCreatedBy());

        try {
            StoredProcedureQuery storedProcedure = em.createStoredProcedureQuery("dbo.sp_DocumentUpload")
                    .registerStoredProcedureParameter(1, String.class, ParameterMode.IN)
                    .registerStoredProcedureParameter(2, String.class, ParameterMode.IN)
                    .registerStoredProcedureParameter(3, String.class, ParameterMode.IN)
                    .registerStoredProcedureParameter(4, String.class, ParameterMode.IN)
                    .registerStoredProcedureParameter(5, Long.class, ParameterMode.IN)
                    .registerStoredProcedureParameter(6, Blob.class, ParameterMode.IN)
                    .registerStoredProcedureParameter(7, String.class, ParameterMode.IN)
                    .setParameter(1, newForm.getAppID())
                    .setParameter(2, newForm.getDocDescription()).setParameter(3, newForm.getFilename())
                    .setParameter(4, newForm.getFileExtension()).setParameter(5, newForm.getFileSize())
                    .setParameter(6, new SerialBlob(newForm.getFiledata())).setParameter(7, newForm.getCreatedBy());

            storedProcedure.execute();
            response.setSuccess(true);

        } catch (NumberFormatException | SQLException ex) {
            ex.printStackTrace();
        }

        return response;
    }

    @Override
    public ApplicationDocumentModel downloadDocument(String documentId) {
        ApplicationDocumentModel result = new ApplicationDocumentModel();

        try {
            StoredProcedureQuery storedProcedure = em
                    .createStoredProcedureQuery("dbo.sp_DocumentDownload", ApplicationDocumentModel.class)
                    .registerStoredProcedureParameter(1, String.class, ParameterMode.IN).setParameter(1, documentId);

            result = (ApplicationDocumentModel) storedProcedure.getResultList().get(0);
        }
        catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

        return result;
    }

    @Override
    public WorkflowDocumentArchiveResponse archiveDocument(String documentId) {
        WorkflowDocumentArchiveResponse response = new WorkflowDocumentArchiveResponse();
        response.setSuccess(false);

        try {
            StoredProcedureQuery storedProcedure = em.createStoredProcedureQuery("dbo.sp_DocumentArchive")
                    .registerStoredProcedureParameter(1, String.class, ParameterMode.IN).setParameter(1, documentId);

            storedProcedure.execute();
            response.setSuccess(true);
        }
        catch(Exception ex) {
            logger.error(ex.getMessage());
        }

        return response;
    }

    @Override
    public void cloneDocuments(int oldWorkflowId, int newWorkflowId) {
        List<ApprovalForumWfdWorkflowDocumentEntity> entities = em.createQuery("from ApprovalForumWfdWorkflowDocumentEntity t " +
                        "where t.workflowId = :workflowId " +
                        "and t.active = :active",
                ApprovalForumWfdWorkflowDocumentEntity.class)
                .setParameter("workflowId", oldWorkflowId)
                .setParameter("active", true)
                .getResultList();
        entities.forEach(approvalForumWfdWorkflowDocumentEntity -> {
            ApprovalForumWfdWorkflowDocumentEntity newDocumentEntity = new ApprovalForumWfdWorkflowDocumentEntity();
            newDocumentEntity.setWorkflowId(newWorkflowId);
            newDocumentEntity.setDocDescription(approvalForumWfdWorkflowDocumentEntity.getDocDescription());
            newDocumentEntity.setFilename(approvalForumWfdWorkflowDocumentEntity.getFilename());
            newDocumentEntity.setFileExtension(approvalForumWfdWorkflowDocumentEntity.getFileExtension());
            newDocumentEntity.setFileSize(approvalForumWfdWorkflowDocumentEntity.getFileSize());
            newDocumentEntity.setFileData(approvalForumWfdWorkflowDocumentEntity.getFileData());
            newDocumentEntity.setCreatedBy(approvalForumWfdWorkflowDocumentEntity.getCreatedBy());
            newDocumentEntity.setCreatedOn(approvalForumWfdWorkflowDocumentEntity.getCreatedOn());
            newDocumentEntity.setActive(approvalForumWfdWorkflowDocumentEntity.getActive());
            em.persist(newDocumentEntity);
        });
    }

}
