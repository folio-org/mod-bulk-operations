# mod-bulk-operations

Copyright (C) 2022-2023 The Open Library Foundation

This software is distributed under the terms of the Apache License, Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

The purpose of this Bulk Operations (Edit/Delete) application is to modify and delete different entities in bulks.

## Additional information

### Required Permissions

### Issue tracker

See project [MODBULKOPS](https://issues.folio.org/browse/MODBULKOPS)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Other documentation

Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at
[dev.folio.org](https://dev.folio.org/).
More information you can find here: [bulk-operations-design](https://wiki.folio.org/display/FOLIJET/Bulk+Operations+redesign)

### FQM integration
The data fetching component from FQM is configured using two parameters: FQM_MAX_CHUNK_SIZE - the maximum 
size of a single chunk, which should not exceed 10,000 for most environments, FQM_MAX_PARALLEL_CHUNKS - the maximum number 
of chunks that can be processed in parallel.  The value of FQM_MAX_PARALLEL_CHUNKS should ideally be calculated using the formula:

FQM_MAX_PARALLEL_CHUNKS = ROUND(MAX_EXPECTED_DATASET_SIZE / FQM_MAX_CHUNK_SIZE).

However, this value must not exceed the maximum allowable number of parallel chunks for the specific environment in order to ensure optimal 
stability and performance of the client-provider system.

### Environment variables
This module uses S3 storage for files. AWS S3 and Minio Server are supported for files storage.
It is also necessary to specify variable S3_IS_AWS to determine if AWS S3 is used as files storage. By default,
this variable is `false` and means that MinIO server is used as storage.
This value should be `true` if AWS S3 is used. To ensure the correct operation of the module, a system user must be
configured. This requires setting the values of environment variables: OKAPI_URL, SYSTEM_USER_NAME, SYSTEM_USER_PASSWORD,
SYSTEM_USER_ENABLED (by default is `true`).

| Name                       | Default value                   | Description                                                                                                 |
|:---------------------------|:--------------------------------|:------------------------------------------------------------------------------------------------------------|
| KAFKA_HOST                 | localhost                       | Kafka broker hostname                                                                                       |
| KAFKA_PORT                 | 9092                            | Kafka broker port                                                                                           |
| ENV                        | folio                           | Environment name                                                                                            |
| S3_URL                     | http://127.0.0.1:9000/          | S3 url                                                                                                      |
| S3_REGION                  | -                               | S3 region                                                                                                   |
| S3_BUCKET                  | -                               | S3 bucket                                                                                                   |
| S3_ACCESS_KEY_ID           | -                               | S3 access key                                                                                               |
| S3_SECRET_ACCESS_KEY       | -                               | S3 secret key                                                                                               |
| S3_IS_AWS                  | false                           | Specify if AWS S3 is used as files storage                                                                  |
| MAX_UPLOADED_FILE_SIZE     | 40MB                            | Specifies multipart upload file size                                                                        |
| PLATFORM                   | okapi                           | Specifies if okapi or eureka platform                                                                       |
| OKAPI_URL                  | http://okapi:9130               | Okapi url                                                                                                   |
| SYSTEM\_USER\_NAME         | mod-bulk-operations-system-user | Username of the system user                                                                                 |
| SYSTEM\_USER\_PASSWORD     | -                               | Password of the system user                                                                                 |
| SYSTEM\_USER\_ENABLED      | true                            | Defines if system user must be created at service tenant initialization or used for egress service requests |
| FQM\_MAX\_CHUNK\_SIZE      | 10000                           | Max chunk size of FQM Fetcher                                                                               |
| FQM\_MAX\_PARALLEL\_CHUNKS | 10                              | Max number of parallel chunks processed at the same time                                                    |
| FQM\_QUERY\_APPROACH       | false                           | Approach to use with query: false if identifiers flow, otherwise FQM flow                                   |

### Memory configuration
To stable module operating the following mod-data-export-worker configuration is required: Java args -XX:MetaspaceSize=384m -XX:MaxMetaspaceSize=512m -Xmx2048m,
AWS container: memory - 3072, memory (soft limit) - 2600, cpu - 1024.