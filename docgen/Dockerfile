# DAF document generation: the legacy approvalforum-docgen (PDF agendas and minutes) on Java 25.
# Runtime values (authentication.jwt.secret, DOCGEN_DEBUG_LOGGING) come from env.properties on the server.
FROM crsk-docker-private.rmb.co.za/base/openjdk:25-crsk
WORKDIR /app
COPY target/daf-docgen.jar daf-docgen.jar
EXPOSE 8080
# Exception messages as on the legacy Java 8: Java 15+ fills a NullPointerException's message ("Cannot invoke ...
# because ... is null") and the legacy code stores and returns e.getMessage(), which was null
ENV JAVA_TOOL_OPTIONS="-XX:-ShowCodeDetailsInExceptionMessages"
ENTRYPOINT ["java", "-jar", "/app/daf-docgen.jar"]
