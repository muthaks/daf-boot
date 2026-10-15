package za.co.rmb.approvalforumdrools.model;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.sql.Timestamp;

@XmlRootElement
public class RulesDataResponeDTO implements Serializable {

    private boolean success;
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
