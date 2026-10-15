package za.co.rmb.approvalforum.model.agenda;

import java.util.ArrayList;
import java.util.List;

/** Result of the role-based quorum evaluation for an item (RULE-005). */
public class AgendaQuorumResultDTO {

    private int agiId;
    private boolean met;
    private int required;
    private int eligibleCount;
    private List<String> countedRoles = new ArrayList<>();
    private List<String> missingRoles = new ArrayList<>();
    private String overrideReason;

    public int getAgiId() { return agiId; }
    public void setAgiId(int agiId) { this.agiId = agiId; }

    public boolean isMet() { return met; }
    public void setMet(boolean met) { this.met = met; }

    public int getRequired() { return required; }
    public void setRequired(int required) { this.required = required; }

    public int getEligibleCount() { return eligibleCount; }
    public void setEligibleCount(int eligibleCount) { this.eligibleCount = eligibleCount; }

    public List<String> getCountedRoles() { return countedRoles; }
    public void setCountedRoles(List<String> countedRoles) { this.countedRoles = countedRoles; }

    public List<String> getMissingRoles() { return missingRoles; }
    public void setMissingRoles(List<String> missingRoles) { this.missingRoles = missingRoles; }

    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }

}
