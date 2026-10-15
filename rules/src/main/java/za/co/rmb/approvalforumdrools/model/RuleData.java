package za.co.rmb.approvalforumdrools.model;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
public class RuleData implements Serializable {

    private String createdBy;
    private String rules;

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

}
