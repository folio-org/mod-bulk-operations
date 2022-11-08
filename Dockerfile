FROM folioci/alpine-jre-openjdk17:latest

USER root

# Copy your fat jar to the container provide the actual name for your fat jar file for example mod-notes-fat.jar
ENV APP_FILE mod-bulk-operations.jar
# - should be a single jar file
ARG JAR_FILE=./target/*.jar
# - copy
COPY ${JAR_FILE} ${JAVA_APP_DIR}/${APP_FILE}

ARG RUN_ENV_FILE=run.sh

COPY ${RUN_ENV_FILE} ${JAVA_APP_DIR}/
RUN chmod 755 ${JAVA_APP_DIR}/${RUN_ENV_FILE}

# Expose this port locally in the container.
EXPOSE 8081
