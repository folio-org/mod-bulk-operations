#!/bin/sh
#
#DB_PROPS_FILE=/usr/verticles/1.txt
#touch ${DB_PROPS_FILE}
#echo "spring.datasource.username=${DB_USERNAME} spring.datasource.password=${DB_PASSWORD} spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_DATABASE}" | sed 's/\s\+/\n/g' | tee ${DB_PROPS_FILE}
#

############################################################################
# Assign fake values to variables (like DB_HOST/DB_PORT etc) if not provided
# This way DB connection won't be established but the application boots up
############################################################################

DB_URL="jdbc:postgresql://${DB_HOST:-localhost}:${DB_PORT:-5432}/${DB_DATABASE:-db}"
#
DB_OPTS="-Dspring.datasource.username=${DB_USERNAME:-user} -Dspring.datasource.password=${DB_PASSWORD:-pass} -Dspring.datasource.url=${DB_URL}"
#
export JAVA_OPTIONS="${JAVA_OPTIONS:-} ${DB_OPTS}"
