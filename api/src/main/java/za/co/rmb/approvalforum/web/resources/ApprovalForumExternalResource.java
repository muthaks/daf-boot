package za.co.rmb.approvalforum.web.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.model.*;
import za.co.rmb.approvalforum.service.ApprovalForumExternalService;
import za.co.rmb.approvalforum.service.ApprovalForumService;
import za.co.rmb.approvalforum.service.exception.ExternalResourceException;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import static za.co.rmb.approvalforum.security.util.SecurityHelper.tokenValidation;

@Api(tags = "external")
@Path("/approvalForum/external")
@org.springframework.stereotype.Component
@org.springframework.context.annotation.Scope("prototype")
public class ApprovalForumExternalResource {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumExternalResource.class);

    @Context
    private SecurityContext context;

    @Inject
    private ApprovalForumExternalService approvalForumExternalService;

    @Inject
    private ApprovalForumService approvalForumService;


    @GET
    @Path("/counterparties")
    @Produces("application/json")
    @ApiOperation(value = "Return list of all CPTYs to select on UI for credit application requests",
            authorizations = @Authorization(value = "Bearer"))
    public Response counterparties(@DefaultValue("1") @QueryParam("tenantId") int tenantId) {
        logger.info("counterpartiesAll()");
        tokenValidation(context);

        try{
            return Response.ok(
                    new CounterpartyResponseDTO()
                            .withCounterpartyDetail(approvalForumExternalService.getCounterparties(true, tenantId))).build();
        }catch(ExternalResourceException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }

    @GET
    @Path("/casdm/counterparties")
    @Produces("application/json")
    @ApiOperation(value = "Search counterparties on CASDM",
            authorizations = @Authorization(value = "Bearer"))
    public Response counterparties(@QueryParam("search") String search, @QueryParam("exact") @DefaultValue("false") boolean isExact) {
        logger.info("counterparties()");
        tokenValidation(context);
        try{
            return Response.ok(
                    new CounterpartyResponseDTO()
                            .withCounterpartyDetail(approvalForumExternalService.getCounterparties(search, isExact))).build();
        }catch(ExternalResourceException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }

    //ashburton changes
    @GET
    @Path("/ashburton/counterparties")
    @Produces("application/json")
    @ApiOperation(value = "Search counterparties on ashburton",
            authorizations = @Authorization(value = "Bearer"))
    public Response counterpartiesForAshburton() {
        logger.info("counterpartiesForAshburton()");
        tokenValidation(context);
        try{
            return Response.ok(
                    new CounterpartyResponseDTO()
                            .withCounterpartyDetail(approvalForumExternalService.getCounterpartiesForAshburton(true))).build();
        }catch(ExternalResourceException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }

    @GET
    @Path("/ashburton/counterpartiesForAshburton")
    @Produces("application/json")
    @ApiOperation(value = "Search counterparties on ashburton",
            authorizations = @Authorization(value = "Bearer"))
    public Response counterpartiesForAshburtonExternal(@QueryParam("search") String search, @QueryParam("exact") @DefaultValue("false") boolean isExact) {
        logger.info("counterpartiesForAshburtonExternal()");
        tokenValidation(context);
        try{
            return Response.ok(
                    new CounterpartyResponseDTO()
                            .withCounterpartyDetail(approvalForumExternalService.getCounterpartiesForAshburtonExternal(search, isExact))).build();
        }catch(ExternalResourceException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }
    @GET
    @Path("/casdm/counterparty")
    @Produces("application/json")
    @ApiOperation(value = "Retrieve CASDM Counterparty Detail by Inkomba",
            authorizations = @Authorization(value = "Bearer"))
    public Response counterpartyDetail(@QueryParam("inkombaId") String inkombaId) {
        logger.info("counterpartyDetail({})", inkombaId);
        tokenValidation(context);
        try{
            return Response.ok(
                    new CounterpartyDetailResponseDTO()
                            .withCounterpartyDetail(approvalForumExternalService.getCounterpartyDetail(inkombaId))).build();
        }catch(ExternalResourceException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }

    @GET
    @Path("/casdm/counterparty/full")
    @Produces("application/json")
    @ApiOperation(value = "Retrieve CASDM Full Counterparty Detail by Inkomba",
            authorizations = @Authorization(value = "Bearer"))
    public Response counterpartyFullDetail(@QueryParam("inkombaId") String inkombaId,
                                           @QueryParam("facilityStatus") String facilityStatus) {
        logger.info("counterpartyFullDetail({}, {})", inkombaId, facilityStatus);
        tokenValidation(context);
        try{
            CounterpartyFullDetail fullDetail = approvalForumExternalService.getCounterpartyFullDetail(inkombaId, facilityStatus);
            return Response.ok(new CounterpartyFullDetailResponseDTO().withCounterpartyFullDetail(fullDetail)).build();
        }catch(ExternalResourceException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }

    @GET
    @Path("/casdm/counterparty/industrySector")
    @Produces("application/json")
    @ApiOperation(value = "Retrieve CASDM Counterparty Industry and Sector Detail by Inkomba",
            authorizations = @Authorization(value = "Bearer"))
    public Response counterpartyIndustrySectorDetail(@QueryParam("inkombaId") String inkombaId) {
        logger.info("counterpartyIndustrySectorDetail({})", inkombaId);
        tokenValidation(context);
        try{
            return Response.ok(
                    new CounterpartySectorIndustryModelResponseDTO()
                            .withCounterpartySectorIndustry(approvalForumExternalService.getCounterpartySectorIndustry(inkombaId))).build();
        }catch(ExternalResourceException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }

    @GET
    @Path("/casdm/facilities")
    @Produces("application/json")
    @ApiOperation(value = "Search facilities on CASDM by Inkomba and Status",
            authorizations = @Authorization(value = "Bearer"))
    public Response facilities(@QueryParam("inkombaId") String inkombaId, @QueryParam("statuses") String statuses) {
        logger.info("facilities({}, {})", inkombaId, statuses);
        tokenValidation(context);
        try{
            return Response.ok(
                    new FacilityResponseDTO()
                            .withFacility(approvalForumExternalService.getFacility(inkombaId, statuses))).build();
        }catch(ExternalResourceException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }

    @GET
    @Path("/ashburton/facilities")
    @Produces("application/json")
    @ApiOperation(value = "Search facilities on CASDM by Inkomba and Status",
            authorizations = @Authorization(value = "Bearer"))
    public Response ashburtonFacilities(@QueryParam("inkombaId") String inkombaId, @QueryParam("statuses") String statuses) {
        logger.info("facilities({}, {})", inkombaId, statuses);
        tokenValidation(context);
        try{
            return Response.ok(
                    new FacilityResponseDTO()
                            .withFacility(approvalForumExternalService.getAshburtonFacility(inkombaId, statuses))).build();
        }catch(ExternalResourceException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }
    @GET
    @Path("/casdm/application")
    @Produces("application/json")
    @ApiOperation(value = "Get application data from CASDM",
            authorizations = @Authorization(value = "Bearer"))
    public Response applications(@QueryParam("inkombaId") String inkombaId) {
        logger.info("applications({})", inkombaId);
        tokenValidation(context);
        try {
            return Response.ok(
                    new CreditApplicationModelResponseDTO()
                            .withCreditApplication(approvalForumExternalService.getCreditApplication(inkombaId))).build();
        }catch(ExternalResourceException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }

    @GET
    @Path("/ashburton/application")
    @Produces("application/json")
    @ApiOperation(value = "Get application data from CASDM",
            authorizations = @Authorization(value = "Bearer"))
    public Response ashburtonApplications(@QueryParam("inkombaId") String inkombaId) {
        logger.info("applications({})", inkombaId);
        tokenValidation(context);
        try {
            return Response.ok(
                    new CreditApplicationModelResponseDTO()
                            .withCreditApplication(approvalForumExternalService.getAshburtonCreditApplication(inkombaId))).build();
        }catch(ExternalResourceException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }

    @GET
    @Path("/casdm/creditDecisions")
    @Produces("application/json")
    @ApiOperation(value = "Retrieve list of credit decisions from CASDM",
            authorizations = @Authorization(value = "Bearer"))
    public Response creditDecisions() {
        logger.info("creditDecisions()");
        tokenValidation(context);
        try{
            return Response.ok(
                    new CreditDecisionResponseDTO()
                            .withCreditDecisions(approvalForumExternalService.getCreditDecisions()))
                    .build();
        }catch(ExternalResourceException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }

    @GET
    @Path("/casdm/meetingNames")
    @Produces("application/json")
    @ApiOperation(value = "Retrieve list of credit decisions from CASDM",
            authorizations = @Authorization(value = "Bearer"))
    public Response meetingNames(@DefaultValue("1") @QueryParam("tenantId") int tenantId) {
        logger.info("meetingNames()");
        tokenValidation(context);
        try{
            if(2 == tenantId) {
                logger.info("meetingNames():tenantId:"+tenantId);
                return Response.ok(
                                new MeetingNamesResponseDTO()
                                        .withMeetingNames(approvalForumExternalService.getAsbMeetingNames()))
                        .build();
            } else if( tenantId >2){
                logger.info("meetingNames():tenantId:"+tenantId);
                return Response.ok(
                                new MeetingNamesResponseDTO()
                                        .withMeetingNames(approvalForumService.getBroaderAfricaMeetingNames(tenantId)))
                        .build();
            }
            else {
                logger.info("meetingNames():tenantId1:"+tenantId);
                return Response.ok(
                                new MeetingNamesResponseDTO()
                                        .withMeetingNames(approvalForumExternalService.getMeetingNames()))
                        .build();
            }
        }catch(ExternalResourceException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }

}
