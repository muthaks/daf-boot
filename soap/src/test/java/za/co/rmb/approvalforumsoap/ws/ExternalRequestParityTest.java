package za.co.rmb.approvalforumsoap.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.xml.transform.StringSource;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.ws.test.server.RequestCreators.withSoapEnvelope;
import static org.springframework.ws.test.server.ResponseMatchers.clientOrSenderFault;
import static org.springframework.ws.test.server.ResponseMatchers.noFault;
import static org.springframework.ws.test.server.ResponseMatchers.payload;
import static org.springframework.ws.test.server.ResponseMatchers.serverOrReceiverFault;

/**
 * The legacy SOAP behaviour, recorded side by side with the legacy approvalforum-soap war (soap/tools/soap-parity.py):
 * which API endpoint each operation and tenant is forwarded to (tenant 2 is Ashburton, every other tenant and no
 * tenant is RMB), what is sent, the answer for each API status, and the UsernameToken check.
 */
@SpringBootTest
class ExternalRequestParityTest {

    private static final String NS = "http://credit.rmb.co.za/approvalforum/external";
    private static final String WSSE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final String PASSWORD_TEXT =
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText";
    /** The token the endpoint sends when AF_TOKEN is not set */
    private static final String DEFAULT_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI4MWM2MzIxYi0wMTQ1LTQzZDEtOTFmYS05OWE5OWYyOTUwODQiLCJpc3MiOiJodHRwOi8vY3JlZGl0LnJtYi5jby56YSIsImF1ZCI6Imh0dHA6Ly9jcmVkaXQucm1iLmNvLnphL2FwcHJvdmFsZm9ydW0iLCJzdWIiOiJTTkFQQVBQX1JSQ2FwdHVyZXJAcm1iLmNvLnphIiwiaWF0IjoxNTcyOTQwNzYyLCJleHAiOjQ3MzUxODA3NjIsInJvbGVzIjpbIkNyZWRpdEFuYWx5c3QiXSwicmVmcmVzaENvdW50IjowLCJyZWZyZXNoTGltaXQiOjF9.CLzf8a-2Sll3wiKNF2rSvf1GO59XXS-ithiIOjsZcD8";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RestTemplate restTemplate;

    private MockWebServiceClient client;
    private MockRestServiceServer api;

    @BeforeEach
    void setUp() {
        client = MockWebServiceClient.createClient(applicationContext);
        api = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void forwardsEachOperationAndTenantToTheLegacyEndpoint() {
        assertForwarded("createExternalRequest", null, "approvalForum/request/external/create");
        assertForwarded("createExternalRequest", 1, "approvalForum/request/external/create");
        assertForwarded("createExternalRequest", 2, "approvalForum/request/external/ashburton/create");
        assertForwarded("createExternalRequest", 3, "approvalForum/request/external/create");
        assertForwarded("cancelExternalRequest", null, "approvalForum/request/external/cancel");
        assertForwarded("cancelExternalRequest", 1, "approvalForum/request/external/cancel");
        assertForwarded("cancelExternalRequest", 2, "approvalForum/request/external/ashburton/cancel");
        assertForwarded("cancelExternalRequest", 3, "approvalForum/request/external/cancel");
    }

    @Test
    void anApiErrorIsAServerFaultWithTheSpring52Message() {
        api.expect(requestTo(apiBase() + "approvalForum/request/external/ashburton/create?appId=4711"))
                .andRespond(withServerError());
        client.sendRequest(withSoapEnvelope(envelope("createExternalRequest", 2, "user", "pass")))
                .andExpect(serverOrReceiverFault("500 Internal Server Error"));
        api.verify();
        api.reset();

        api.expect(requestTo(apiBase() + "approvalForum/request/external/cancel?appId=4711"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));
        client.sendRequest(withSoapEnvelope(envelope("cancelExternalRequest", null, "user", "pass")))
                .andExpect(serverOrReceiverFault("401 Unauthorized"));
        api.verify();
    }

    @Test
    void aWrongPasswordOrNoSecurityHeaderIsAClientFaultAndNothingIsForwarded() {
        client.sendRequest(withSoapEnvelope(envelope("createExternalRequest", 1, "user", "nope")))
                .andExpect(clientOrSenderFault());
        client.sendRequest(withSoapEnvelope(new StringSource(
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body>"
                                + body("createExternalRequest", 1) + "</soapenv:Body></soapenv:Envelope>")))
                .andExpect(clientOrSenderFault());
        api.verify();
    }

    private void assertForwarded(String operation, Integer tenant, String path) {
        api.expect(requestTo(apiBase() + path + "?appId=4711"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + token()))
                .andExpect(header("Content-Type", "application/json"))
                .andExpect(header("Accept", "application/json"))
                .andExpect(content().string("4711"))
                .andRespond(withSuccess());
        String response = operation.startsWith("create") ? "createExternalResponse" : "cancelExternalResponse";
        client.sendRequest(withSoapEnvelope(envelope(operation, tenant, "user", "pass")))
                .andExpect(noFault())
                .andExpect(payload(new StringSource("<ns2:" + response + " xmlns:ns2=\"" + NS + "\">"
                        + "<ns2:success>true</ns2:success></ns2:" + response + ">")));
        api.verify();
        api.reset();
    }

    private static StringSource envelope(String operation, Integer tenant, String user, String password) {
        return new StringSource("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soapenv:Header><wsse:Security xmlns:wsse=\"" + WSSE + "\" soapenv:mustUnderstand=\"1\">"
                + "<wsse:UsernameToken><wsse:Username>" + user + "</wsse:Username>"
                + "<wsse:Password Type=\"" + PASSWORD_TEXT + "\">" + password + "</wsse:Password>"
                + "</wsse:UsernameToken></wsse:Security></soapenv:Header>"
                + "<soapenv:Body>" + body(operation, tenant) + "</soapenv:Body></soapenv:Envelope>");
    }

    private static String body(String operation, Integer tenant) {
        return "<ext:" + operation + " xmlns:ext=\"" + NS + "\"><ext:applicationId>4711</ext:applicationId>"
                + (tenant != null ? "<ext:tenantId>" + tenant + "</ext:tenantId>" : "")
                + "</ext:" + operation + ">";
    }

    /** As the endpoint reads it: AF_BASE, or the legacy default, with a trailing slash */
    private static String apiBase() {
        String uri = env("AF_BASE", "http://rmb-axu1-w:8197/approvalforum/api/");
        return uri.endsWith("/") ? uri : uri + "/";
    }

    private static String token() {
        return env("AF_TOKEN", DEFAULT_TOKEN);
    }

    private static String env(String name, String def) {
        String val = System.getenv(name);
        return val == null || val.trim().isEmpty() ? def : val;
    }
}
