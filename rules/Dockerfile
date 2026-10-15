# DAF rules service: the legacy approvalforumdrools war (Drools rules from R_Rules) on Java 25.
# Runtime values (DB_*, the JWT settings) come from env.properties on the server.
FROM crsk-docker-private.rmb.co.za/base/openjdk:25-crsk
WORKDIR /app
COPY target/daf-rules.jar daf-rules.jar
EXPOSE 8080
# Exception messages as on the legacy Java 8: Java 15+ fills a NullPointerException's message ("Cannot invoke ...
# because ... is null") and the legacy code stores and returns e.getMessage(), which was null
ENV JAVA_TOOL_OPTIONS="-XX:-ShowCodeDetailsInExceptionMessages"
ENTRYPOINT ["java", "-jar", "/app/daf-rules.jar"]
