package za.co.rmb.approvalforum.model.lumos;

public class CountryInfo {
    private int id;
    private String code;
    private String name;
    private boolean disabled;
    private String twoLetterCode;
    private int countryGroup_Id;
    private String countryGroup_Name;
    private int portfolioCountryGroup_Id;
    private String portfolioCountryGroup_Name;
    private String countryCompanyCode;
    private int digitalApprovalForumTenantId;

    // Getters and Setters
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public boolean isDisabled() {
        return disabled;
    }
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getTwoLetterCode() {
        return twoLetterCode;
    }
    public void setTwoLetterCode(String twoLetterCode) {
        this.twoLetterCode = twoLetterCode;
    }

    public int getCountryGroup_Id() {
        return countryGroup_Id;
    }
    public void setCountryGroup_Id(int countryGroup_Id) {
        this.countryGroup_Id = countryGroup_Id;
    }

    public String getCountryGroup_Name() {
        return countryGroup_Name;
    }
    public void setCountryGroup_Name(String countryGroup_Name) {
        this.countryGroup_Name = countryGroup_Name;
    }

    public int getPortfolioCountryGroup_Id() {
        return portfolioCountryGroup_Id;
    }
    public void setPortfolioCountryGroup_Id(int portfolioCountryGroup_Id) {
        this.portfolioCountryGroup_Id = portfolioCountryGroup_Id;
    }

    public String getPortfolioCountryGroup_Name() {
        return portfolioCountryGroup_Name;
    }
    public void setPortfolioCountryGroup_Name(String portfolioCountryGroup_Name) {
        this.portfolioCountryGroup_Name = portfolioCountryGroup_Name;
    }

    public String getCountryCompanyCode() {
        return countryCompanyCode;
    }
    public void setCountryCompanyCode(String countryCompanyCode) {
        this.countryCompanyCode = countryCompanyCode;
    }

    public int getDigitalApprovalForumTenantId() {
        return digitalApprovalForumTenantId;
    }
    public void setDigitalApprovalForumTenantId(int digitalApprovalForumTenantId) {
        this.digitalApprovalForumTenantId = digitalApprovalForumTenantId;
    }
}
