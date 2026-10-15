package za.co.rmb.approvalforum.model.lumos;

public class PartyRoleDto {
    private int partyRoleId;
    private Party party;
    private Client client;

    // Getters and Setters


    public int getPartyRoleId() {
        return partyRoleId;
    }

    public void setPartyRoleId(int partyRoleId) {
        this.partyRoleId = partyRoleId;
    }

    public Party getParty() {
        return party;
    }

    public void setParty(Party party) {
        this.party = party;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}



