package za.co.rmb.approvalforum.model.lumos;

public class CountryContribution {
    private int id;
    private int country_Id;
    private int percContribution;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCountry_Id() {
        return country_Id;
    }

    public void setCountry_Id(int country_Id) {
        this.country_Id = country_Id;
    }

    public int getPercContribution() {
        return percContribution;
    }

    public void setPercContribution(int percContribution) {
        this.percContribution = percContribution;
    }
}
