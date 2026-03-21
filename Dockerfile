FROM eclipse-temurin:21-jre

WORKDIR /app

ARG JAR_FILE=target/mic-gatewayservice-*.jar
COPY ${JAR_FILE} app.jar

ENV PROFILE=local \
    JAVA_OPTS=""

EXPOSE 9000

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
