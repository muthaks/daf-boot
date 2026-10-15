package za.co.rmb.approvalforum.model.agenda;

import java.sql.Timestamp;

public class AgendaItemDocumentDTO {

    private int agidId;
    private int agiId;
    private String docStashId;
    private String docType;
    private String fileName;
    private String uploadedBy;
    private Timestamp uploadedOn;
    private Long fileSize;
    private String fileExtension;

    public int getAgidId() { return agidId; }
    public void setAgidId(int agidId) { this.agidId = agidId; }

    public int getAgiId() { return agiId; }
    public void setAgiId(int agiId) { this.agiId = agiId; }

    public String getDocStashId() { return docStashId; }
    public void setDocStashId(String docStashId) { this.docStashId = docStashId; }

    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public Timestamp getUploadedOn() { return uploadedOn; }
    public void setUploadedOn(Timestamp uploadedOn) { this.uploadedOn = uploadedOn; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFileExtension() { return fileExtension; }
    public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }

    // ---- BRS alignment (V13) ----
    private boolean mandatory;
    private String validationStatus;
    private String validatedBy;
    private Timestamp validatedOn;
    private String validationComment;
    private int version;

    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }

    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }

    public String getValidatedBy() { return validatedBy; }
    public void setValidatedBy(String validatedBy) { this.validatedBy = validatedBy; }

    public Timestamp getValidatedOn() { return validatedOn; }
    public void setValidatedOn(Timestamp validatedOn) { this.validatedOn = validatedOn; }

    public String getValidationComment() { return validationComment; }
    public void setValidationComment(String validationComment) { this.validationComment = validationComment; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

}
