package za.co.rmb.approvalforum.web.resources;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.inject.Inject;
import org.springframework.transaction.annotation.Transactional;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.model.UserData;
import za.co.rmb.approvalforum.model.WindowsAuthDetail;
import za.co.rmb.approvalforum.security.model.AuthenticationTokenDetails;
import za.co.rmb.approvalforum.security.service.AuthenticationTokenIssuer;
import za.co.rmb.approvalforum.service.ApprovalForumAuthService;
import za.co.rmb.approvalforum.service.ApprovalForumService;
import za.co.rmb.credit.authservice.model.*;
import za.co.rmb.approvalforum.web.auth.AuthServiceBootClient;
import za.co.rmb.credit.authservice.web.client.AuthServiceClient;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Api(tags = "auth")
@Path("/auth/")
@org.springframework.stereotype.Component
@org.springframework.context.annotation.Scope("prototype")
public class ApprovalForumAuthResource {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumAuthResource.class);

    @Context
    private SecurityContext context;

    @Inject
    private ApprovalForumAuthService approvalForumAuthService;

    private Map<String, String> afSettings;

    private AuthenticationTokenIssuer authenticationTokenIssuer;

    private AuthServiceClient authClient;

    @Inject
    public ApprovalForumAuthResource(@Named("af_settings") Map<String, String> afSettings,
                                     AuthenticationTokenIssuer authenticationTokenIssuer,
                                     ApprovalForumService listManagementService,
                                     AuthServiceClient authClient) {
        this.afSettings = afSettings;
        this.authenticationTokenIssuer = authenticationTokenIssuer;
        this.authClient = authClient;
    }

    @Context
    HttpServletRequest request;

    @GET
    @Path("/ping")
    @Produces("application/json")
    @ApiOperation(value = "Returns current status for service")
    @PermitAll
    public Response ping(){
        logger.info("ping()");
        PingModel result = new PingModel(false);
        try {
            AuthServiceClient client = new AuthServiceBootClient(afSettings.get("af.authservice.base"));
            result = client.Ping();

            if (result == null) {
                result = new PingModel(false);
            }
        } catch (Exception ex) {
            logger.info(ex.getMessage());
        }
        logger.info("ping result: " + result.getSuccess());
        return Response.ok(result).build();
    }

    @GET
    @Path("/error")
    @Produces("application/json")
    @ApiOperation(value = "Throws an error")
    @PermitAll
    public Response error() throws Exception {
        logger.info("error()");

        throw new Exception("Error this is an exception");
        
        //return Response.ok().build();
    }

    @GET
    @Path("/domains")
    @Produces("application/json")
    @ApiOperation(value = "Returns supported Active Directory domains from Authentication Service")
    @PermitAll
    public Response domains() {
        logger.info("domains()");
        List<ActiveDirectoryDomainModel> result = new ArrayList<>();
        try {
            AuthServiceClient client = new AuthServiceBootClient(afSettings.get("af.authservice.base"));
            result = client.GetDomains(true);
            result.sort(Comparator.comparing(ActiveDirectoryDomainModel::getDomain));
        } catch (Exception ex) {
            logger.info(ex.getMessage());
        }
        return Response.ok(result).build();
    }

    @GET
    @Path("/users")
    @Produces("application/json")
    @ApiOperation(value = "Returns users based on displayName")
    @PermitAll
    public Response users(@ApiParam(required = true) @QueryParam("domain") String domain,
            @ApiParam(required = true) @QueryParam("displayName") String displayName) {
        logger.info("users()");
        List<ActiveDirectoryUserModel> result = new ArrayList<>();
        try {
            AuthServiceClient client = new AuthServiceBootClient(afSettings.get("af.authservice.base"));
            result = client.UsersWithDisplayName(domain, displayName);
            result.sort(Comparator.comparing(ActiveDirectoryUserModel::getDisplayName));
        } catch (Exception ex) {
            logger.info(ex.getMessage());
        }
        return Response.ok(result).build();
    }

    @GET
    @Path("/groupusers")
    @Produces("application/json")
    @ApiOperation(value = "Returns users based on displayName")
    @PermitAll
    public Response groupusers(@ApiParam(required = true) @QueryParam("domain") String domain,
            @ApiParam(required = true) @QueryParam("dn") String dn) {
        logger.info("groupusers()");
        List<ActiveDirectoryUserModel> result = new ArrayList<>();
        try {
            AuthServiceClient client = new AuthServiceBootClient(afSettings.get("af.authservice.base"));
            result = client.Users(domain, dn);
            result.sort(Comparator.comparing(ActiveDirectoryUserModel::getDisplayName));
        } catch (Exception ex) {
            logger.info(ex.getMessage());
        }
        return Response.ok(result).build();
    }

    @GET
    @Path("/groups")
    @Produces("application/json")
    @ApiOperation(value = "Returns groups based on name")
    @PermitAll
    public Response groups(@ApiParam(required = true) @QueryParam("domain") String domain,
            @ApiParam(required = true) @QueryParam("name") String name) {
        logger.info("groups()");
        List<ActiveDirectoryGroupModel> result = new ArrayList<>();
        try {
            AuthServiceClient client = new AuthServiceBootClient(afSettings.get("af.authservice.base"));
            result = client.GroupsFromName(domain, name);
            result.sort(Comparator.comparing(ActiveDirectoryGroupModel::getName));
        } catch (Exception ex) {
            logger.info(ex.getMessage());
        }
        return Response.ok(result).build();
    }
    @GET
    @Path("/keycloakConfig")
    @Produces("application/json")
    @ApiOperation(value = "Returns keycloak Config JSON")
    @PermitAll
    public Response keycloakConfig() {
        logger.info("keycloakConfig()");
        
        Map<String, Object> credentials = new HashMap<>();
        Map<String, Object> data = new HashMap<>();

        credentials.put("secret",afSettings.get("af.keycloakConfig.secret"));

        data.put("credentials", credentials);
        
        
        data.put("resource", afSettings.get("af.keycloakConfig.clientId"));
        data.put("realm", afSettings.get("af.keycloakConfig.realm")); // should be JWT
        
        data.put("auth-server-url", afSettings.get("af.keycloakConfig.url"));
       
        return Response.ok(data).build();
    }

    @POST
    @Path("/login")
    @Produces("application/json")
    @ApiOperation(value = "Get Authentication response based on username and password")
    @PermitAll
    @Transactional
    public Response login(@ApiParam(required = true) @HeaderParam("domain") String domain,
            @ApiParam(required = true) @HeaderParam("username") String username,
            @ApiParam(required = true) @HeaderParam("password") String password) {
        logger.info("login()");

        AuthServiceClient client = new AuthServiceBootClient(afSettings.get("af.authservice.base"));
        String displayName = "";
        String status = "";
        String reason = "";

        String adName = "";
        String adSurname = "";
        String adMail = "";
        String adDomain = "";
        String adUserPrincipalName = "";
        String title = "";

        List<String> groups = new ArrayList<>();
        ActiveDirectoryLoginResultModel result = new ActiveDirectoryLoginResultModel();
        boolean isRmb = true;
        if (client.Ping().getSuccess()) {
            result = client.AuthenticateAgainstAD(domain, username, password);
            adName = result.getModel().getGivenName() == null ? "" : result.getModel().getGivenName();
            adSurname = result.getModel().getSn() == null ? "" : result.getModel().getSn();
            adMail = result.getModel().getMail() == null ? "" : result.getModel().getMail();
            adDomain = result.getModel().getDomain() == null ? "" : result.getModel().getDomain();
            title = result.getModel().getTitle() == null ? "" : result.getModel().getTitle();
            adUserPrincipalName = result.getModel().getUserPrincipalName() == null ? ""
                    : result.getModel().getUserPrincipalName();

            if (result.getModel() != null && !"RMB".equals(result.getModel().getDomain())) {
                isRmb = false;
                GroupRequestModel model = new GroupRequestModel();
                model.setDomain("RMB");
                model.setOu(afSettings.get("af.adgroup.ou"));
                model.setObjectSid(result.getModel().getObjectSid());
                List<ActiveDirectoryGroupModel> groupModels = client.GetGroups(model);

                for (ActiveDirectoryGroupModel groupModel : groupModels) {
                    result.getModel().getGroups().add(groupModel.getCn());
                }
            }

            if ("success".equals(result.getStatus())) {
                if (result.getModel().getDisplayName() == null) {
                    if (result.getModel().getCn() == null) {
                        if (isRmb) {
                            displayName = result.getModel().getDomain() + "\\" + username;
                        } else {
                            displayName = username;
                        }
                    } else {
                        displayName = result.getModel().getCn();
                        groups = result.getModel().getGroups();
                    }
                } else {
                    displayName = result.getModel().getDisplayName();
                    groups = result.getModel().getGroups();
                }
            }
            status = result.getStatus();
            reason = result.getReason();
        }
        List<String> lmsGroups = getAFGroups(groups, isDevUser(username));
        createUpdateActiveDirectoryUser(status, result);

        String id = generateTokenIdentifier();
        ZonedDateTime issuedDate = ZonedDateTime.now();
        ZonedDateTime expirationDate = calculateExpirationDate(issuedDate);

        AuthenticationTokenDetails authenticationTokenDetails = new AuthenticationTokenDetails.Builder().withId(id)
                .withUsername(adUserPrincipalName)
                .withRoles(new HashSet<String>(
                        lmsGroups.stream().map(group -> group.toLowerCase()).collect(Collectors.toList())))
                .withIssuedDate(issuedDate).withExpirationDate(expirationDate).withRefreshCount(0).withRefreshLimit(1)
                .build();

        String token = authenticationTokenIssuer.issueToken(authenticationTokenDetails);

        Map<String, String> dictionary = new HashMap<>();
        dictionary.put("Token", token); // should be JWT
        if (isRmb) {
            dictionary.put("Username", "RMB-NT\\" + username);
        } else {
            dictionary.put("Username", username);
        }
        dictionary.put("DisplayName", displayName);
        dictionary.put("Expiry", getIso8601Date());
        dictionary.put("Status", status);
        dictionary.put("Reason", reason);
        dictionary.put("Roles", Arrays.toString(lmsGroups.toArray()));
        dictionary.put("UserPrincipalName", adUserPrincipalName);
        dictionary.put("Name", adName);
        dictionary.put("Surname", adSurname);
        dictionary.put("Mail", adMail);
        dictionary.put("Domain", adDomain);

        if (result.getModel() != null && result.getModel().getProfilePhoto() != null
                && result.getModel().getProfilePhoto().getImageData() != null) {
            dictionary.put("imageType", result.getModel().getProfilePhoto().getImageType());
            dictionary.put("imageData",
                    Base64.getEncoder().encodeToString(result.getModel().getProfilePhoto().getImageData()));
        }

        UserData userData = new UserData();
        userData.setDomain(adDomain);
        userData.setEmail(adMail);
        userData.setName(adName);
        userData.setSurname(adSurname);
        userData.setUserId(adUserPrincipalName);
        userData.setTitle(title);

        approvalForumAuthService.createUpdateActiveDirectoryUser(adUserPrincipalName,
                result.getModel().getDisplayName() == null ? "" : result.getModel().getDisplayName(),
                result.getModel().getGivenName() == null ? "" : result.getModel().getGivenName(),
                result.getModel().getSn() == null ? "" : result.getModel().getSn(),
                result.getModel().getMail() == null ? "" : result.getModel().getMail());

        return Response.ok(dictionary).build();
    }

    @POST
    @Path("/roleToken")
    @Produces("application/json")
    @ApiOperation(value = "Get Role Token based on SSO Token")
    @PermitAll
    @Transactional
    public Response roleToken(@ApiParam(required = true) String ssoToken) {
        logger.info("roleToken()");
        // AuthServiceClient client = new AuthServiceClient(afSettings.get("af.authservice.base"));
        DecodedJWT jwt = JWT.decode(ssoToken);

        String userPrincipalName = jwt.getClaim("userPrincipalName").asString();

        if (userPrincipalName == null) {
            userPrincipalName = jwt.getClaim("preferred_username").asString() + "@rmb.co.za";
        }

        List<String> afGroups = getAFGroups(jwt.getClaim("memberOf").asList(String.class), isDevUser(userPrincipalName));
        //System.out.println(lmsGroups.toString());
        String id = generateTokenIdentifier();
        ZonedDateTime issuedDate = ZonedDateTime.now();
        ZonedDateTime expirationDate = calculateExpirationDate(issuedDate);

        
        AuthenticationTokenDetails authenticationTokenDetails = new AuthenticationTokenDetails.Builder().withId(id)
                .withUsername(userPrincipalName)
                .withRoles(new HashSet<String>(
                    afGroups.stream().map(group -> group).collect(Collectors.toList())))
                .withIssuedDate(issuedDate).withExpirationDate(expirationDate).withRefreshCount(0).withRefreshLimit(1)
                .build();

        UserData userData = new UserData();
        userData.setDomain(jwt.getClaim("domain").asString() == null ? "" : jwt.getClaim("domain").asString());
        userData.setEmail(jwt.getClaim("email").asString() == null ? "" : jwt.getClaim("email").asString());
        userData.setName(jwt.getClaim("given_name").asString() == null ? jwt.getClaim("name").asString(): jwt.getClaim("given_name").asString());
        userData.setSurname(jwt.getClaim("family_name").asString() == null ? "" : jwt.getClaim("family_name").asString());
        userData.setUserId(userPrincipalName);
        userData.setTitle(jwt.getClaim("title").asString() == null ? "" : jwt.getClaim("title").asString());

        approvalForumAuthService.createUpdateActiveDirectoryUser(userPrincipalName,
                jwt.getClaim("display_name").asString() == null ? "" : jwt.getClaim("display_name").asString(),
                jwt.getClaim("given_name").asString() == null ? "" : jwt.getClaim("given_name").asString(),
                jwt.getClaim("family_name").asString() == null ? "" : jwt.getClaim("family_name").asString(),
                jwt.getClaim("email").asString() == null ? "" : jwt.getClaim("email").asString());

        String token = authenticationTokenIssuer.issueToken(authenticationTokenDetails);
        //logger.info("roleToken::"+token);
        Map<String, String> dictionary = new HashMap<>();
        dictionary.put("Token", token);
        return Response.ok(dictionary).build();
    }

    @POST
    @Path("/ssoToken")
    @Produces("application/json")
    @ApiOperation(value = "Get SSO Token based on Windows Auth Details")
    @PermitAll
    @Transactional
    public Response ssoToken(@ApiParam(required = true) WindowsAuthDetail authDetail) {
        logger.info("ssoToken()");
        // AuthServiceClient client = new AuthServiceClient(afSettings.get("af.authservice.base"));

        String userPrincipalName = authDetail.getUserPrincipalName();

        List<String> afGroups = new ArrayList<>();
        
        if ("rmb.co.za".equalsIgnoreCase(authDetail.getHostDomain()) == false) {
            ActiveDirectoryUserModel adUser = this.authClient.User(userPrincipalName);

            if (!"RMB".equals(adUser.getDomain())) {
                GroupRequestModel model = new GroupRequestModel();
                model.setDomain("RMB");
                model.setOu(afSettings.get("af.adgroup.ou"));
                model.setObjectSid(adUser.getObjectSid());
                List<ActiveDirectoryGroupModel> groupModels = this.authClient.GetGroups(model);
                for (ActiveDirectoryGroupModel groupModel : groupModels) {
                    adUser.getGroups().add(groupModel.getCn());
                }
            }

            afGroups = getAFGroups(adUser.getGroups(), isDevUser(userPrincipalName));
        } else {
            afGroups = getAFGroups(authDetail.getGroups(), isDevUser(userPrincipalName));
        }

        
        //System.out.println(lmsGroups.toString());
        String id = generateTokenIdentifier();
        ZonedDateTime issuedDate = ZonedDateTime.now();
        ZonedDateTime expirationDate = calculateExpirationDate(issuedDate);

        
        AuthenticationTokenDetails authenticationTokenDetails = new AuthenticationTokenDetails.Builder().withId(id)
                .withUsername(userPrincipalName)
                .withRoles(new HashSet<String>(
                    afGroups.stream().map(group -> group).collect(Collectors.toList())))
                .withIssuedDate(issuedDate).withExpirationDate(expirationDate).withRefreshCount(0).withRefreshLimit(1)
                .build();

        UserData userData = new UserData();
        userData.setDomain(authDetail.getDomain());
        userData.setEmail(authDetail.getEmailAddress() == null ? "" : authDetail.getEmailAddress());
        userData.setName(authDetail.getGivenName() == null ? authDetail.getName() : authDetail.getGivenName());
        userData.setSurname(authDetail.getSurname() == null ? "" : authDetail.getSurname());
        userData.setUserId(userPrincipalName);
        userData.setTitle("");
        
        approvalForumAuthService.createUpdateActiveDirectoryUser(userPrincipalName,
                authDetail.getDisplayName() == null ? "" : authDetail.getDisplayName(),
                authDetail.getGivenName() == null ? "" : authDetail.getGivenName(),
                authDetail.getSurname() == null ? "" : authDetail.getSurname(),
                authDetail.getEmailAddress() == null ? "" : authDetail.getEmailAddress());

        String token = authenticationTokenIssuer.issueToken(authenticationTokenDetails);
        Map<String, String> dictionary = new HashMap<>();
        dictionary.put("Token", token);
        return Response.ok(dictionary).build();
    }

    @POST
    @Path("/tenant")
    @Produces("application/json")
    @ApiOperation(value = "Get tenant based on Windows Auth Details")
    @PermitAll
    @Transactional
    public Response ssoTenant(@ApiParam(required = true) WindowsAuthDetail authDetail) {
        logger.info("ssoTenant()");
        // AuthServiceClient client = new AuthServiceClient(afSettings.get("af.authservice.base"));

        String userPrincipalName = authDetail.getUserPrincipalName();
        logger.info("ssoTenant:userPrincipalName:" + userPrincipalName);
        int tenantId;
        boolean isAshburton;
        logger.info("ssoTenant:authDetail:" + authDetail.getHostDomain());
        if ("rmb.co.za".equalsIgnoreCase(authDetail.getHostDomain()) == false) {
            ActiveDirectoryUserModel adUser = this.authClient.User(userPrincipalName);
            logger.info("ssoTenant1()");
            isAshburton = isAshburton(adUser.getGroups(), isDevUser(userPrincipalName));
            logger.info("ssoTenant1():isAshburton:"+isAshburton);
        } else {
            logger.info("ssoTenant2----------------");
            isAshburton = isAshburton(authDetail.getGroups(), isDevUser(userPrincipalName));
            logger.info("ssoTenant2():isAshburton:"+isAshburton);
        }
        if(isAshburton){
            logger.info("ssoTenant():isAshburton- true");
            tenantId=2;
        } else {
            logger.info("ssoTenant():isAshburton- false");
            tenantId=1;
        }
        logger.info("ssoTenant112():"+tenantId);
        Map<String, Integer> dictionary = new HashMap<>();
        dictionary.put("tenantId", tenantId);
        logger.info("ssoTenant():tenantId"+dictionary.get("tenantId"));
        return Response.ok(dictionary).build();
    }

    @GET
    @Path("/ssoServers")
    @Produces("application/json")
    @ApiOperation(value = "Returns SSO Servers for UI")
    @PermitAll
    public Response ssoServers() {
        logger.info("ssoServers()");

        String[] ssoServers = afSettings.get("af.sso.servers").split(",");
       
        return Response.ok(ssoServers).build();
    }

    private ZonedDateTime calculateExpirationDate(ZonedDateTime issuedDate) {
        long validFor = 3162240000L;
        return issuedDate.plusSeconds(validFor);
    }

    /**
     * Generate a token identifier.
     */
    private String generateTokenIdentifier() {
        return UUID.randomUUID().toString();
    }

    private boolean isDevUser(String userName) {
        String devUsers = afSettings.get("af.devusers");
        if (devUsers != null) {
            if (devUsers.length() > 0) {
                String[] splitUsers = devUsers.split(",");
                for (String devUser : splitUsers) {
                    if (devUser.trim().toLowerCase().equals(userName.toLowerCase())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<String> getAFGroups(List<String> groups, boolean isDevTeam) {
        List<String> appGroups = new ArrayList<>();

        String environment = afSettings.get("af.environment");
        String ashEnvironment = afSettings.get("af.environment");
        logger.info("environment: " + environment);
        String dlPrefix = "DL_DAF";
        String fPrefix = "FL_DAF";
        String gPrefix = "G_DAF";

        String dlASHPrefix = "DL_ASH_DAF";
        String fASHPrefix = "FL_ASH_DAF";
        String ASHPrefix = "_Ashburton_";
        //G_DAF_PRD_Ashburton_Chairperson
        //gPrefix + "_" + ashEnvironment + ASHPrefix+"CreditAnalyst"
        //dlASHPrefix + "_" + environment + "CreditAnalyst"
        //DL_ASH_DAF_Uat_Approver
        if (environment.equalsIgnoreCase("PROD")) {
            logger.info("getAFGroups:environment in if block: " + environment);
            environment = "";
            ashEnvironment = "PRD";

        } else {
            logger.info("getAFGroups:environment in else block: " + environment);
            environment += "_";
            ashEnvironment+="_";
        }

        if (groups != null) {
            if (!groups.isEmpty()) {
                for (String group : groups) {
                    String lowergroup = group.toLowerCase();
                    logger.info("getAFGroups:lowergroup : " + lowergroup);
                    // Credit Analyst Role
                    if (lowergroup.contains((dlPrefix + "_" + environment + "CreditAnalyst").toLowerCase()) || lowergroup.contains((dlASHPrefix + "_" + environment + "CreditAnalyst").toLowerCase()) ) {
                        if (!appGroups.contains("CreditAnalyst")) {
                            appGroups.add("CreditAnalyst");
                        }
                    } else if (lowergroup.contains((fPrefix + "_" + environment + "CreditAnalyst").toLowerCase()) || lowergroup.contains((fASHPrefix + "_" + environment + "CreditAnalyst").toLowerCase())) {
                        if (!appGroups.contains("CreditAnalyst")) {
                            appGroups.add("CreditAnalyst");
                        }
                    } else if (lowergroup.contains((gPrefix + "_" + environment + "CreditAnalyst").toLowerCase()) || lowergroup.contains((gPrefix + "_" + ashEnvironment + ASHPrefix+"CreditAnalyst").toLowerCase())) {
                        if (!appGroups.contains("CreditAnalyst")) {
                            appGroups.add("CreditAnalyst");
                        }
                    }
                    //logger.info("getAFGroups:appGroups1 : " + appGroups);

                    // Approver Role
                    if (lowergroup.contains((dlPrefix + "_" + environment + "Approver").toLowerCase()) || lowergroup.contains((dlASHPrefix + "_" + environment + "Approver").toLowerCase()) ) {
                        if (!appGroups.contains("Approver")) {
                            appGroups.add("Approver");
                        }
                    } else if (lowergroup.contains((fPrefix + "_" + environment + "Approver").toLowerCase()) || lowergroup.contains((fASHPrefix + "_" + environment + "Approver").toLowerCase())) {
                        if (!appGroups.contains("Approver")) {
                            appGroups.add("Approver");
                        }
                    } else if (lowergroup.contains((gPrefix + "_" + environment + "Approver").toLowerCase()) || lowergroup.contains((gPrefix + "_" + ashEnvironment + ASHPrefix + "Approver").toLowerCase())) {
                        if (!appGroups.contains("Approver")) {
                            appGroups.add("Approver");
                        }
                    }
                    //logger.info("getAFGroups:appGroups2 : " + appGroups);

                    // Review Analyst Role
                    if (lowergroup.contains((dlPrefix + "_" + environment + "ReviewAnalyst").toLowerCase()) || lowergroup.contains((dlASHPrefix + "_" + environment + "ReviewAnalyst").toLowerCase())) {
                        if (!appGroups.contains("ReviewAnalyst")) {
                            appGroups.add("ReviewAnalyst");
                        }
                    } else if (lowergroup.contains((fPrefix + "_" + environment + "ReviewAnalyst").toLowerCase()) || lowergroup.contains((fASHPrefix + "_" + environment + "ReviewAnalyst").toLowerCase())) {
                        if (!appGroups.contains("ReviewAnalyst")) {
                            appGroups.add("ReviewAnalyst");
                        }
                    } else if (lowergroup.contains((gPrefix + "_" + environment + "ReviewAnalyst").toLowerCase()) || lowergroup.contains((gPrefix + "_" + ashEnvironment + ASHPrefix+ "ReviewAnalyst").toLowerCase())) {
                        if (!appGroups.contains("ReviewAnalyst")) {
                            appGroups.add("ReviewAnalyst");
                        }
                    }
                    //logger.info("getAFGroups:appGroups3 : " + appGroups);

                    // Chairperson Role
                    if (lowergroup.contains((dlPrefix + "_" + environment + "Chairperson").toLowerCase()) || lowergroup.contains((dlASHPrefix + "_" + environment + "Chairperson").toLowerCase())) {
                        if (!appGroups.contains("Chairperson")) {
                            appGroups.add("Chairperson");
                        }
                    } else if (lowergroup.contains((fPrefix + "_" + environment + "Chairperson").toLowerCase()) || lowergroup.contains((fASHPrefix + "_" + environment + "Chairperson").toLowerCase())) {
                        if (!appGroups.contains("Chairperson")) {
                            appGroups.add("Chairperson");
                        }
                    } else if (lowergroup.contains((gPrefix + "_" + environment + "Chairperson").toLowerCase()) || lowergroup.contains((gPrefix + "_" + ashEnvironment + ASHPrefix+ "Chairperson").toLowerCase())) {
                        if (!appGroups.contains("Chairperson")) {
                            appGroups.add("Chairperson");
                        }
                    }
                    //logger.info("getAFGroups:appGroups4 : " + appGroups);

                    // Secretary Role (committee secretariat / administration - agenda builder)
                    if (lowergroup.contains((dlPrefix + "_" + environment + "Secretary").toLowerCase()) || lowergroup.contains((dlASHPrefix + "_" + environment + "Secretary").toLowerCase())) {
                        if (!appGroups.contains("Secretary")) {
                            appGroups.add("Secretary");
                        }
                    } else if (lowergroup.contains((fPrefix + "_" + environment + "Secretary").toLowerCase()) || lowergroup.contains((fASHPrefix + "_" + environment + "Secretary").toLowerCase())) {
                        if (!appGroups.contains("Secretary")) {
                            appGroups.add("Secretary");
                        }
                    } else if (lowergroup.contains((gPrefix + "_" + environment + "Secretary").toLowerCase()) || lowergroup.contains((gPrefix + "_" + ashEnvironment + ASHPrefix + "Secretary").toLowerCase())) {
                        if (!appGroups.contains("Secretary")) {
                            appGroups.add("Secretary");
                        }
                    }
                    //logger.info("getAFGroups:appGroups5 : " + appGroups);
                }
            }
        }
        logger.info("appGroups: " + appGroups);
        return appGroups;
    }

    private boolean isAshburton(List<String> groups, boolean isDevTeam) {
        boolean isAshburton=false;

        logger.info("isAshburton: appGroups:" + groups.toString());

        String environment = afSettings.get("af.environment");
        String ashEnvironment = afSettings.get("af.environment");
        logger.info("environment: " + environment);

        String dlPrefix = "DL_DAF";
        String fPrefix = "FL_DAF";
        String gPrefix = "G_DAF";

        String dlASHPrefix = "DL_ASH_DAF";
        String fASHPrefix = "FL_ASH_DAF";
        String gASHPrefix = "G_ASH_DAF";

        String ASHPrefix = "Ashburton_";
        //G_DAF_PRD_Ashburton_Chairperson
        //gPrefix + "_" + ashEnvironment + ASHPrefix+"CreditAnalyst"
        //dlASHPrefix + "_" + environment + "CreditAnalyst"
        //DL_ASH_DAF_Uat_Approver
////G_DAF_DEV_Ashburton_CreditAnalyst

        if (environment.equalsIgnoreCase("PROD")) {
            logger.info("getAFGroups:environment in if block: " + environment);
            environment = "";
            ashEnvironment = "PRD_";

        } else {
            logger.info("getAFGroups:environment in else block: " + environment);
            environment += "_";
            ashEnvironment+="_";
        }

        if (groups != null) {
            if (!groups.isEmpty()) {
                for (String group : groups) {
                    String lowergroup = group.toLowerCase();
                    logger.info("isAshburton:lowergroup : " + lowergroup);
                    // Credit Analyst Role
                    if (lowergroup.contains((dlPrefix + "_" + environment + "CreditAnalyst").toLowerCase()) || lowergroup.contains((fPrefix + "_" + environment + "CreditAnalyst").toLowerCase()) || lowergroup.contains((gPrefix + "_" + environment + "CreditAnalyst").toLowerCase())) {
                       return false;
                    } else if (lowergroup.contains((dlPrefix + "_" + environment + "Approver").toLowerCase()) || lowergroup.contains((fPrefix + "_" + environment + "Approver").toLowerCase()) || lowergroup.contains((gPrefix + "_" + environment + "Approver").toLowerCase())){
                        return false;
                    }  else if (lowergroup.contains((dlPrefix + "_" + environment + "ReviewAnalyst").toLowerCase()) || lowergroup.contains((fPrefix + "_" + environment + "ReviewAnalyst").toLowerCase()) || lowergroup.contains((gPrefix + "_" + environment + "ReviewAnalyst").toLowerCase())) {
                        return false;
                    } else if (lowergroup.contains((dlPrefix + "_" + environment + "Chairperson").toLowerCase()) || lowergroup.contains((fPrefix + "_" + environment + "Chairperson").toLowerCase()) || lowergroup.contains((gPrefix + "_" + environment + "Chairperson").toLowerCase())) {
                        return false;
                    }
                    //(dlASHPrefix + "_" + environment + "Chairperson")
                    ////gPrefix + "_" + ashEnvironment + ASHPrefix+"CreditAnalyst"
                    ////gPrefix + "_" + ashEnvironment + ASHPrefix+"CreditAnalyst"
                    else if(lowergroup.contains((dlASHPrefix + "_" + environment + "CreditAnalyst").toLowerCase())
                            || lowergroup.contains((fASHPrefix + "_" + environment + "CreditAnalyst").toLowerCase())
                            || lowergroup.contains((gASHPrefix + "_" + environment + "CreditAnalyst").toLowerCase())
                            || lowergroup.contains((gPrefix + "_" + ashEnvironment+ASHPrefix + "CreditAnalyst").toLowerCase())) {
                       // logger.info("isAshburton1: " + isAshburton);
                        return true;
                    } else if (lowergroup.contains((dlASHPrefix + "_" + environment + "Approver").toLowerCase())
                            || lowergroup.contains((fASHPrefix + "_" + environment + "Approver").toLowerCase())
                            || lowergroup.contains((gASHPrefix + "_" + environment + "Approver").toLowerCase())
                            || lowergroup.contains((gPrefix + "_" + ashEnvironment+ASHPrefix + "Approver").toLowerCase())){
                       // logger.info("isAshburton2: " + isAshburton);
                        return true;
                    }  else if (lowergroup.contains((dlASHPrefix + "_" + environment + "ReviewAnalyst").toLowerCase())
                            || lowergroup.contains((fASHPrefix + "_" + environment + "ReviewAnalyst").toLowerCase())
                            || lowergroup.contains((gASHPrefix + "_" + environment + "ReviewAnalyst").toLowerCase())
                            || lowergroup.contains((gPrefix + "_" + ashEnvironment+ASHPrefix + "ReviewAnalyst").toLowerCase())) {
                       // logger.info("isAshburton3: " + isAshburton);
                        return true;
                    } else if (lowergroup.contains((dlASHPrefix + "_" + environment + "Chairperson").toLowerCase())
                            || lowergroup.contains((fASHPrefix + "_" + environment + "Chairperson").toLowerCase())
                            || lowergroup.contains((gASHPrefix + "_" + environment + "Chairperson").toLowerCase())
                            || lowergroup.contains((gPrefix + "_" + ashEnvironment+ASHPrefix + "Chairperson").toLowerCase())) {
                        //logger.info("isAshburton4: " + isAshburton);
                        return true;
                    }
                }
            }
        }
        logger.info("isAshburton:isAshburton : " + isAshburton);
        return isAshburton;
    }

    private String getIso8601Date() {
        Instant now = Instant.now();
        return now.plus(Duration.ofMinutes(10)).toString();
    }

    private void createUpdateActiveDirectoryUser(String status, Object obj){
        boolean create = false;
        String aliasAd = null, displayNameAd = null, givenNameAd = null, surnameAd = null, mailAd = null;
        if(status.equals("success")) {
            if (obj != null) {
                if(obj instanceof ActiveDirectoryLoginResultModel) {
                    ActiveDirectoryLoginResultModel result = (ActiveDirectoryLoginResultModel) obj;
                    if (result.getModel() != null) {
                        aliasAd = result.getModel().getUserPrincipalName();
                        displayNameAd = result.getModel().getDisplayName();
                        givenNameAd = result.getModel().getGivenName();
                        surnameAd = result.getModel().getSn();
                        mailAd = result.getModel().getMail();
                        create = true;
                    }
                }else{
                    ActiveDirectoryUserModel result = (ActiveDirectoryUserModel) obj;
                    aliasAd = result.getUserPrincipalName();
                    displayNameAd = result.getDisplayName();
                    givenNameAd = result.getGivenName();
                    surnameAd = result.getSn();
                    mailAd = result.getMail();
                    create = true;
                }
            }
        }

        if(create) {
            if (aliasAd == null) {
                aliasAd = "";
            } else {
                aliasAd = aliasAd.replace("@rmb.co.za", "");
            }

            if (displayNameAd == null)
                displayNameAd = "";
            if (givenNameAd == null)
                givenNameAd = "";
            if (surnameAd == null)
                surnameAd = "";
            if (mailAd == null)
                mailAd = "";

            approvalForumAuthService.createUpdateActiveDirectoryUser(aliasAd, displayNameAd, givenNameAd, surnameAd, mailAd);
        }

    }

}
