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

### Environment variables
This module uses S3 storage for files. AWS S3 and Minio Server are supported for files storage.
It is also necessary to specify variable S3_IS_AWS to determine if AWS S3 is used as files storage. By default,
this variable is `false` and means that MinIO server is used as storage.
This value should be `true` if AWS S3 is used.

| Name                         | Default value          | Description                                 |
|:-----------------------------|:-----------------------|:--------------------------------------------|
| KAFKA_HOST                   | localhost              | Kafka broker hostname                       |
| KAFKA_PORT                   | 9092                   | Kafka broker port                           |
| ENV                          | folio                  | Environment name                            |
| S3_URL                       | http://127.0.0.1:9000/ | S3 url                                      |
| S3_REGION                    | -                      | S3 region                                   |
| S3_BUCKET                    | -                      | S3 bucket                                   |
| S3_ACCESS_KEY_ID             | -                      | S3 access key                               |
| S3_SECRET_ACCESS_KEY         | -                      | S3 secret key                               |
| S3_IS_AWS                    | false                  | Specify if AWS S3 is used as files storage  |
