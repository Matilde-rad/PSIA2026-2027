FROM eclipse-temurin:21.0.12.1_1-jre
ARG MODULE
RUN useradd --system --uid 10001 --home-dir /app app && install -d -o app /cache
WORKDIR /app
COPY ${MODULE}/target/quarkus-app/ ./
ENV JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", "-jar", "quarkus-run.jar"]
