package za.co.rmb.approvalforum.data.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Objects;

@Entity
@Table(name = "WFD_WorkflowDocument")
public class ApprovalForumWfdWorkflowDocumentEntity {

    private int wfdId;
    private Integer workflowId;
    private String docDescription;
    private String filename;
    private String fileExtension;
    private Integer fileSize;
    private byte[] fileData;
    private String createdBy;
    private Timestamp createdOn;
    private Boolean active;

    @Id
    @Column(name = "WFD_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getWfdId() {
        return wfdId;
    }

    public void setWfdId(int wfdId) {
        this.wfdId = wfdId;
    }

    @Basic
    @Column(name = "WorkflowID", nullable = true)
    public Integer getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Integer workflowId) {
        this.workflowId = workflowId;
    }

    @Basic
    @Column(name = "DocDescription", nullable = true, length = 255)
    public String getDocDescription() {
        return docDescription;
    }

    public void setDocDescription(String docDescription) {
        this.docDescription = docDescription;
    }

    @Basic
    @Column(name = "Filename", nullable = true, length = 255)
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Basic
    @Column(name = "FileExtension", nullable = true, length = 255)
    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    @Basic
    @Column(name = "FileSize", nullable = true)
    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    @Basic
    @Column(name = "FileData", nullable = true)
    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    @Basic
    @Column(name = "CreatedBy", nullable = true, length = 255)
    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Basic
    @Column(name = "CreatedOn", nullable = true)
    public Timestamp getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Timestamp createdOn) {
        this.createdOn = createdOn;
    }

    @Basic
    @Column(name = "Active", nullable = true)
    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumWfdWorkflowDocumentEntity that = (ApprovalForumWfdWorkflowDocumentEntity) o;
        return wfdId == that.wfdId &&
                Objects.equals(workflowId, that.workflowId) &&
                Objects.equals(docDescription, that.docDescription) &&
                Objects.equals(filename, that.filename) &&
                Objects.equals(fileExtension, that.fileExtension) &&
                Objects.equals(fileSize, that.fileSize) &&
                Arrays.equals(fileData, that.fileData) &&
                Objects.equals(createdBy, that.createdBy) &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(active, that.active);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(wfdId, workflowId, docDescription, filename, fileExtension, fileSize, createdBy, createdOn, active);
        result = 31 * result + Arrays.hashCode(fileData);
        return result;
    }
}
