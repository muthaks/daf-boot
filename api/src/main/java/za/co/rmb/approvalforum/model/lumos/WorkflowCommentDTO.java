package za.co.rmb.approvalforum.model.lumos;
import lombok.Data;
import java.util.List;

/**
 * @author Rajesh Sunderraju
 */
@Data
public class WorkflowCommentDTO {
    private int workflowId;
    private List<CommentDTO> comments;
}
