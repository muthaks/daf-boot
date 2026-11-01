package za.co.rmb.credit.daf.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * The DAF MCP server: an MCP (Model Context Protocol) server over Streamable HTTP whose tools are the
 * daf-api endpoints. An agent connects with the user's DAF token as bearer; every tool call becomes the
 * matching daf-api request with that token, so DAF enforces the user's roles as it does for the UI.
 */
@SpringBootApplication
@EnableConfigurationProperties(DafMcpProperties.class)
public class DafMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(DafMcpApplication.class, args);
    }
}
