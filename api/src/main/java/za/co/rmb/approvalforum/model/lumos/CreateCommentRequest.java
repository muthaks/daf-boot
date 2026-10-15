package za.co.rmb.approvalforum.model.lumos;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@ToString
@Data
public class CreateCommentRequest
{
    protected int workflowId;
    protected String displayName;
    protected String comment;
    protected LocalDateTime meetingDate;
    protected String meetingName;
    protected int workflowDecisionId;
    protected int meetingNameId;

}
