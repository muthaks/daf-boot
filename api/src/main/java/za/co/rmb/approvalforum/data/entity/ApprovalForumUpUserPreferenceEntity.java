package za.co.rmb.approvalforum.data.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "UP_UserPreference")
public class ApprovalForumUpUserPreferenceEntity {

    private int upId;
    private String userId;
    private boolean chatSortAscending;

    @Id
    @Column(name = "UP_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getUpId() {
        return upId;
    }

    public void setUpId(int upId) {
        this.upId = upId;
    }

    @Basic
    @Column(name = "UserId", nullable = false, length = 100)
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Basic
    @Column(name = "ChatSortAscending", nullable = false)
    public boolean isChatSortAscending() {
        return chatSortAscending;
    }

    public void setChatSortAscending(boolean chatSortAscending) {
        this.chatSortAscending = chatSortAscending;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumUpUserPreferenceEntity that = (ApprovalForumUpUserPreferenceEntity) o;
        return upId == that.upId &&
                chatSortAscending == that.chatSortAscending &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(upId, userId, chatSortAscending);
    }

}
