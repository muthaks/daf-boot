package za.co.rmb.approvalforum.model.lumos;

import lombok.Data;

@Data
public class CountryInfoDTO {
    /*CountryInfo countryInfo;*/
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
}
