# DAF API: the legacy approvalforum war (every /approvalforum/api endpoint and the Quartz jobs) on Java 25.
# Runtime values are the legacy container's environment (DB_*, AF_*, the credit data tokens), from
# env.properties on the server; nothing environment-specific is baked in. The base image carries the FRG
# certificates the legacy image imported.
FROM crsk-docker-private.rmb.co.za/base/openjdk:25-crsk
WORKDIR /app
COPY target/daf-api.jar daf-api.jar
EXPOSE 8080
# Exception messages as on the legacy Java 8: Java 15+ fills a NullPointerException's message ("Cannot invoke ...
# because ... is null") and the legacy code stores and returns e.getMessage(), which was null
ENV JAVA_TOOL_OPTIONS="-XX:-ShowCodeDetailsInExceptionMessages"
# Legacy Tomcat ran with -Xms1024M -Xmx2048M (setenv.sh)
ENTRYPOINT ["java", "-Xms1024M", "-Xmx2048M", "-jar", "/app/daf-api.jar"]
# The legacy health check, same call and timings. It is also what starts the scheduler: the Quartz jobs are
# scheduled by the first GET /scheduler/scheduler. The image has no wget, so bash sends the request.
HEALTHCHECK --timeout=60s --interval=300s --retries=99 \
    CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /approvalforum/api/scheduler/scheduler HTTP/1.0\r\nHost: 127.0.0.1\r\n\r\n' >&3 && read -r _ status _ <&3 && [[ $status == 2* ]]"]
