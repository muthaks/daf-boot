package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Audited
@Table(name = "AGID_AgendaItemDocument")
public class ApprovalForumAgidAgendaItemDocumentEntity {

    private int agidId;
    private String createdBy;
    private Timestamp createdOn;
    private boolean active;
    private String docStashId;
    private String docType;
    private String fileName;
    private String uploadedBy;
    private Timestamp uploadedOn;
    private byte[] fileData;
    private Long fileSize;
    private String fileExtension;
    private boolean mandatory;
    private String validationStatus;
    private String validatedBy;
    private Timestamp validatedOn;
    private String validationComment;
    private int version = 1;

    private ApprovalForumAgiAgendaItemEntity agiAgendaItemByAgiId;

    @Id
    @Column(name = "AGID_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getAgidId() {
        return agidId;
    }

    public void setAgidId(int agidId) {
        this.agidId = agidId;
    }

    @Basic
    @Column(name = "CreatedBy", nullable = false)
    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Basic
    @Column(name = "CreatedOn", nullable = false)
    public Timestamp getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Timestamp createdOn) {
        this.createdOn = createdOn;
    }

    @Basic
    @Column(name = "Active", nullable = false)
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Basic
    @Column(name = "DocStashId")
    public String getDocStashId() {
        return docStashId;
    }

    public void setDocStashId(String docStashId) {
        this.docStashId = docStashId;
    }

    @Basic
    @Column(name = "DocType")
    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    @Basic
    @Column(name = "FileName")
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Basic
    @Column(name = "UploadedBy")
    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    @Basic
    @Column(name = "UploadedOn")
    public Timestamp getUploadedOn() {
        return uploadedOn;
    }

    public void setUploadedOn(Timestamp uploadedOn) {
        this.uploadedOn = uploadedOn;
    }

    @NotAudited
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "FileData")
    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    @Basic
    @Column(name = "FileSize")
    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    @Basic
    @Column(name = "FileExtension")
    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    // ---- BRS alignment (V13): mandatory flag, validation and versioning (section 6.5) ----

    @Basic
    @Column(name = "Mandatory", nullable = false)
    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }

    @Basic
    @Column(name = "ValidationStatus", length = 20)
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }

    @Basic
    @Column(name = "ValidatedBy")
    public String getValidatedBy() { return validatedBy; }
    public void setValidatedBy(String validatedBy) { this.validatedBy = validatedBy; }

    @Basic
    @Column(name = "ValidatedOn")
    public Timestamp getValidatedOn() { return validatedOn; }
    public void setValidatedOn(Timestamp validatedOn) { this.validatedOn = validatedOn; }

    @Basic
    @Column(name = "ValidationComment", length = 1000)
    public String getValidationComment() { return validationComment; }
    public void setValidationComment(String validationComment) { this.validationComment = validationComment; }

    @Basic
    @Column(name = "Version", nullable = false)
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AGI_ID", referencedColumnName = "AGI_ID", nullable = false)
    public ApprovalForumAgiAgendaItemEntity getAgiAgendaItemByAgiId() {
        return agiAgendaItemByAgiId;
    }

    public void setAgiAgendaItemByAgiId(ApprovalForumAgiAgendaItemEntity agiAgendaItemByAgiId) {
        this.agiAgendaItemByAgiId = agiAgendaItemByAgiId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumAgidAgendaItemDocumentEntity that = (ApprovalForumAgidAgendaItemDocumentEntity) o;
        return agidId == that.agidId &&
                Objects.equals(docStashId, that.docStashId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agidId, docStashId);
    }

}
