package za.co.rmb.approvalforum.model.lumos;

import java.time.LocalDateTime;

import lombok.Data;


/**
 * @author Rajesh Sunderraju
 */
@Data
public class CommentDTO {
    private int id;
    private String comment;
    private String user;
    private String userId;
    private LocalDateTime meetingDate;
    private int meetingNameId;
    private int workflowDecisionId;
    private LocalDateTime  createdOn;
    private LocalDateTime updatedOn;

    // Getters and setters
}
