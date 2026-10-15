package za.co.rmb.approvalforum.service;

import za.co.rmb.approvalforum.model.*;
import za.co.rmb.approvalforum.service.exception.ExternalResourceException;
import za.co.rmb.credit.referencedata.model.Currency;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ApprovalForumExternalService {

    List<CounterpartyModel> getCounterparties(boolean excludeActive, int tenantId) throws ExternalResourceException;

    List<CounterpartyModel> getCounterparties(boolean excludeActive, int tenantId, List<String> applicationStatuses) throws ExternalResourceException;

    List<CounterpartyModel> getCounterparties(String search, boolean isExact) throws ExternalResourceException;

    CounterpartyFullDetail getCounterpartyFullDetail(String inkombaId, String facilityStatus) throws ExternalResourceException;

    CounterpartyModel getCounterpartyDetail(String inkombaId) throws ExternalResourceException;

    CreditApplicationModel getCreditApplication(String inkombaId) throws ExternalResourceException;

    CreditApplicationByIdModel getCreditApplicationUsingIdAllStatuses(int applicationId) throws ExternalResourceException;

    List<FacilityModel> getFacility(String inkombaId, String statuses) throws ExternalResourceException;

    List<CounterpartySectorIndustryModel> getCounterpartySectorIndustry(String inkombaId) throws ExternalResourceException;

    List<CreditDecision> getCreditDecisions() throws ExternalResourceException;

    List<WholesaleCreditPortfolioData> getWholesaleCreditPortfolios() throws ExternalResourceException;

    List<MeetingName> getMeetingNames() throws ExternalResourceException;

    Map<String, Currency> getCurrenciesAsMap();

    int hoursBetweenBusinessDates(Date fromDate, Date toDate);

    //AshburtonCreditApplication changes

    CounterpartyFullDetail getAshburtonCounterpartyFullDetail(String inkombaId, int appId, String facilityStatus) throws ExternalResourceException;

    CreditApplicationByIdModel getAshburtonCreditApplicationUsingIdAllStatuses(int applicationId) throws ExternalResourceException;

    List<CreditDecision> getAsbCreditDecisions() throws ExternalResourceException;

    List<WholesaleCreditPortfolioData> getAsbWholesaleCreditPortfolios() throws ExternalResourceException;

    List<MeetingName> getAsbMeetingNames() throws ExternalResourceException;

    List<CounterpartyModel> getCounterpartiesForAshburton(boolean excludeActive) throws ExternalResourceException;

    List<CounterpartyModel> getCounterpartiesForAshburtonExternal(String search, boolean isExact) throws ExternalResourceException;

    LimitsRequestedDTO getLimitRequest(String creditInkombaId)throws ExternalResourceException;

    ASBFundsDTO getAsbFunds(String applicationId) throws ExternalResourceException;

    List<FacilityModel> getAshburtonFacility(String inkombaId, String statuses) throws ExternalResourceException;

    CreditApplicationModel getAshburtonCreditApplication(String inkombaId) throws ExternalResourceException;

    List<CounterpartySectorIndustryModel> getAshburtonCounterpartySectorIndustry(String inkombaId) throws ExternalResourceException;

    CounterpartyModel getAshburtonCounterpartyDetail(String inkombaId) throws ExternalResourceException;

    ASBLimitFundsDTO getAshburtonLimitFunds(String applicationId) throws ExternalResourceException;

    //getBroaderAfricaMeetingNames
    List<MeetingName> getBroaderAfricaMeetingNames(int tenantId) throws ExternalResourceException;
}

