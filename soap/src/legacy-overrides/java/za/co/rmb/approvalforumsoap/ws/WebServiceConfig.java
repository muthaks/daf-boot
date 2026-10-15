package za.co.rmb.approvalforumsoap.ws;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurer;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.server.endpoint.interceptor.PayloadLoggingInterceptor;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;
import org.springframework.ws.soap.security.wss4j2.callback.SimplePasswordValidationCallbackHandler;
import org.springframework.ws.soap.server.endpoint.interceptor.PayloadValidatingInterceptor;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

/**
 * The legacy Spring WS configuration on Spring WS for Jakarta EE.
 *
 * <p>Everything is as before (dispatcher servlet path, WSDL definition and schema, RestTemplate, payload logging and
 * schema validation interceptors, one user from AF_SOAP_USER / AF_SOAP_PASS) except the security interceptor. Legacy
 * used XWSS ({@code XwsSecurityInterceptor} with securityPolicy.xml: {@code RequireUsernameToken}, no password digest,
 * no nonce), which has no Jakarta version. The same check is a WSS4J UsernameToken validation against the same
 * user. {@code WsConfigurerAdapter} was removed; {@link WsConfigurer} has the same default methods.</p>
 *
 * <p>The RestTemplate is built the way Spring Boot 2.2 / Spring 5.2 built it, because its errors become the SOAP
 * fault string: requests go through HttpURLConnection with the body buffered (sent with a Content-Length), and a
 * 4xx/5xx answer is thrown with the Spring 5.2 message {@code "<status> <reason phrase>"} (Spring 6+ adds the URL and
 * the response body to it).</p>
 */
@EnableWs
@Configuration
public class WebServiceConfig implements WsConfigurer {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/approvalforumexternal/ws/*");
    }

    @Bean(name = "approvalForumExternal")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema approvalForumExternalSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("ApprovalForumExternalPort");
        wsdl11Definition.setLocationUri("/approvalforumexternal/ws");
        wsdl11Definition.setTargetNamespace("https://credit.rmb.co.za/approvalforum/external");
        wsdl11Definition.setSchema(approvalForumExternalSchema());
        return wsdl11Definition;
    }

    @Bean
    public XsdSchema approvalForumExternalSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/approvalforumsoap.xsd"));
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .requestFactory(() -> new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
                .errorHandler(new LegacyResponseErrorHandler())
                .build();
    }

    /** Spring 5.2's DefaultResponseErrorHandler.handleError: the exception message is the status and reason only. */
    static class LegacyResponseErrorHandler extends DefaultResponseErrorHandler {
        @Override
        protected void handleError(ClientHttpResponse response, HttpStatusCode statusCode, URI url, HttpMethod method)
                throws IOException {
            String statusText = response.getStatusText();
            HttpHeaders headers = response.getHeaders();
            byte[] body = getResponseBody(response);
            Charset charset = getCharset(response);
            HttpStatus status = HttpStatus.resolve(statusCode.value());
            String reason = StringUtils.hasLength(statusText) ? statusText : (status != null ? status.getReasonPhrase() : "");
            String message = statusCode.value() + " " + reason;
            if (statusCode.is4xxClientError()) {
                throw HttpClientErrorException.create(message, statusCode, statusText, headers, body, charset);
            }
            if (statusCode.is5xxServerError()) {
                throw HttpServerErrorException.create(message, statusCode, statusText, headers, body, charset);
            }
            throw new UnknownHttpStatusCodeException(message, statusCode.value(), statusText, headers, body, charset);
        }
    }

    @Bean
    PayloadLoggingInterceptor payloadLoggingInterceptor() {
        return new PayloadLoggingInterceptor();
    }

    @Bean
    PayloadValidatingInterceptor payloadValidatingInterceptor() {
        final PayloadValidatingInterceptor payloadValidatingInterceptor = new PayloadValidatingInterceptor();
        payloadValidatingInterceptor.setSchema(new ClassPathResource("xsd/approvalforumsoap.xsd"));
        return payloadValidatingInterceptor;
    }

    /** securityPolicy.xml: RequireUsernameToken, passwordDigestRequired="false", nonceRequired="false" */
    @Bean
    Wss4jSecurityInterceptor securityInterceptor() {
        Wss4jSecurityInterceptor securityInterceptor = new Wss4jSecurityInterceptor();
        securityInterceptor.setValidationActions("UsernameToken");
        securityInterceptor.setValidationCallbackHandler(callbackHandler());
        return securityInterceptor;
    }

    @Bean
    SimplePasswordValidationCallbackHandler callbackHandler() {
        SimplePasswordValidationCallbackHandler callbackHandler = new SimplePasswordValidationCallbackHandler();
        callbackHandler.setUsersMap(Collections.singletonMap(getEnvVariable("AF_SOAP_USER", "user"),
                getEnvVariable("AF_SOAP_PASS", "pass")));
        return callbackHandler;
    }

    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        interceptors.add(payloadLoggingInterceptor());
        interceptors.add(payloadValidatingInterceptor());
        interceptors.add(securityInterceptor());
    }

    private String getEnvVariable(String variable, String def) {
        String val = System.getenv(variable);
        if (val == null || "".equals(val.trim())) return def;
        return val;
    }
}
