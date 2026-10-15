package za.co.rmb.approvalforum.data.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "ADU_ActiveDirectoryUser")
public class ApprovalForumAduActiveDirectoryUserEntity {

    private int aduId;
    private String alias;
    private String displayName;
    private String givenName;
    private String surname;
    private String mail;
    private Timestamp createdOn;
    private Timestamp updatedOn;

    @Id
    @Column(name = "ADU_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getAduId() {
        return aduId;
    }

    public void setAduId(int aduId) {
        this.aduId = aduId;
    }

    @Basic
    @Column(name = "Alias", nullable = true, length = 100)
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Basic
    @Column(name = "DisplayName", nullable = true, length = 100)
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Basic
    @Column(name = "GivenName", nullable = true, length = 100)
    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    @Basic
    @Column(name = "Surname", nullable = true, length = 100)
    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    @Basic
    @Column(name = "Mail", nullable = true, length = 100)
    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
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
    @Column(name = "UpdatedOn", nullable = true)
    public Timestamp getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Timestamp updatedOn) {
        this.updatedOn = updatedOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumAduActiveDirectoryUserEntity that = (ApprovalForumAduActiveDirectoryUserEntity) o;
        return aduId == that.aduId &&
                Objects.equals(alias, that.alias) &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(givenName, that.givenName) &&
                Objects.equals(surname, that.surname) &&
                Objects.equals(mail, that.mail) &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(updatedOn, that.updatedOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aduId, alias, displayName, givenName, surname, mail, createdOn, updatedOn);
    }

}
