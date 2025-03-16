# https://github.com/folio-org/folio-tools/tree/master/folio-java-docker/openjdk21
FROM folioci/alpine-jre-openjdk21:latest

# Install latest patch versions of packages: https://pythonspeed.com/articles/security-updates-in-docker/
USER root
RUN apk upgrade --no-cache
USER folio

# Copy your fat jar to the container
ENV APP_FILE mod-bulk-operations-fat.jar

# - should be a single jar file
ARG JAR_FILE=./target/*.jar

# - copy
COPY ${JAR_FILE} ${JAVA_APP_DIR}/${APP_FILE}

# Add JMX exporter and config
RUN mkdir -p jmx_exporter &&\
    wget -P jmx_exporter https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.17.2/jmx_prometheus_javaagent-0.17.2.jar
COPY ./prometheus-jmx-config.yaml jmx_exporter/

# Expose this port locally in the container.
EXPOSE 8081 9991
