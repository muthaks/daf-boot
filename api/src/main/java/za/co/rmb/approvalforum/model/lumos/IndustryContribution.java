package za.co.rmb.approvalforum.model.lumos;

public class IndustryContribution {
    private int id;
    private int industry_Id;
    private int percContribution;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIndustry_Id() {
        return industry_Id;
    }

    public void setIndustry_Id(int industry_Id) {
        this.industry_Id = industry_Id;
    }

    public int getPercContribution() {
        return percContribution;
    }

    public void setPercContribution(int percContribution) {
        this.percContribution = percContribution;
    }
}
