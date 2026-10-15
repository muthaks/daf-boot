# DAF SOAP service: external create/cancel requests (the legacy approvalforum-soap) forwarded to the DAF API.
# Runtime values (AF_BASE, AF_TOKEN, AF_SOAP_USER, AF_SOAP_PASS) come from env.properties on the server.
FROM crsk-docker-private.rmb.co.za/base/openjdk:25-crsk
WORKDIR /app
COPY target/daf-soap.jar daf-soap.jar
EXPOSE 8080
# Exception messages as on the legacy Java 8: Java 15+ fills a NullPointerException's message ("Cannot invoke ...
# because ... is null") and the legacy code stores and returns e.getMessage(), which was null
ENV JAVA_TOOL_OPTIONS="-XX:-ShowCodeDetailsInExceptionMessages"
ENTRYPOINT ["java", "-jar", "/app/daf-soap.jar"]
