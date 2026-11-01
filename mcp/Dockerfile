# DAF MCP server: the daf-api endpoints as Model Context Protocol tools over Streamable HTTP (/mcp).
# Runtime values come from env.properties on the server (DAF_API_BASE, DAF_MCP_*); nothing is baked in.
FROM crsk-docker-private.rmb.co.za/base/openjdk:25-crsk
WORKDIR /app
COPY target/daf-mcp.jar daf-mcp.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xms256M", "-Xmx512M", "-jar", "/app/daf-mcp.jar"]
# The image has no wget, so bash sends the health request.
HEALTHCHECK --timeout=10s --interval=60s --retries=5 \
    CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /actuator/health HTTP/1.0\r\nHost: 127.0.0.1\r\n\r\n' >&3 && read -r _ status _ <&3 && [[ $status == 2* ]]"]
