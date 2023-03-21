## v2.0.0 - Unreleased

## v1.0.3 - Released 2023/03/21
This release contains fixes for processing bad data, updating folio-spring-base version and logging improvements

### Bugs
* [MODBULKOPS-85](https://issues.folio.org/browse/MODBULKOPS-85) - Expected errors are not populated for Holdings identifiers
* [FOLSPRINGB-95](https://issues.folio.org/browse/FOLSPRINGB-95) - non-public beginFolioExecutionContext avoids wrong tenant/user

## v1.0.2 - Released 2023/03/10
This release contains fixes for processing bad data and minor improvements

### Technical tasks
* [MODBULKOPS-50](https://issues.folio.org/browse/MODBULKOPS-50) - TD: csv <-> object conversion unit test.

### Tech debts
* [MODBULKOPS-71](https://issues.folio.org/browse/MODBULKOPS-71) - Handle bad data gracefully.

### Bugs
* [MODBULKOPS-61](https://issues.folio.org/browse/MODBULKOPS-61) - Reported "Completed" status instead of "Completed with errors"
* [MODBULKOPS-30](https://issues.folio.org/browse/MODBULKOPS-30) - Provide human readable id for bulk edit jobs.

## v1.0.1 - Released 2023/03/03
This release includes infrastructural changes (memory settings, Prometheus integration, increasing upload file size, performance optimization) and bug-fixes.

### Stories
* [RANCHER-621](https://issues.folio.org/browse/RANCHER-621) - Add config file for Prometheus.

### Bugs
* [MODEXPW-375](https://issues.folio.org/browse/MODEXPW-375) - Job runs by user not the one who created the job.

## v1.0.0 - Released 2023/02/24
The initial release of the mod-bulk operations functionality (refactored version of mod-bulk-operations).

### Stories
* [MODBULKOPS-26](https://issues.folio.org/browse/MODBULKOPS-26) - Update the module to Spring boot v3.0.0 and identify issues.
* [MODBULKOPS-20](https://issues.folio.org/browse/MODBULKOPS-20) - Download bulk edit files from Logs page
* [MODBULKOPS-16](https://issues.folio.org/browse/MODBULKOPS-16) - BulkOperationService- uploading files with identifiers
* [MODBULKOPS-12](https://issues.folio.org/browse/MODBULKOPS-12) - Create data structures for the Unified table representation of data
* [MODBULKOPS-11](https://issues.folio.org/browse/MODBULKOPS-11) - BulkOperationController
* [MODBULKOPS-10](https://issues.folio.org/browse/MODBULKOPS-10) - BulkOperationService
* [MODBULKOPS-9](https://issues.folio.org/browse/MODBULKOPS-9) - ErrorService
* [MODBULKOPS-8](https://issues.folio.org/browse/MODBULKOPS-8) - UpdateProcessor
* [MODBULKOPS-7](https://issues.folio.org/browse/MODBULKOPS-7) - DataProcessor
* [MODBULKOPS-6](https://issues.folio.org/browse/MODBULKOPS-6) - ModClient/ModClientAdapter
* [MODBULKOPS-5](https://issues.folio.org/browse/MODBULKOPS-5) - RemoteFileSystemRepository
* [MODBULKOPS-4](https://issues.folio.org/browse/MODBULKOPS-4) - DataExportClient
* [MODBULKOPS-3](https://issues.folio.org/browse/MODBULKOPS-3) - BulkOperationRepository
* [MODBULKOPS-2](https://issues.folio.org/browse/MODBULKOPS-2) - Bulk operation DB schema
* [MODBULKOPS-1](https://issues.folio.org/browse/MODBULKOPS-1) - Setup mod-bulk-operations module

### Bugs
* [MODBULKOPS-63](https://issues.folio.org/browse/MODBULKOPS-63) - Error message is cryptic
* [MODBULKOPS-53](https://issues.folio.org/browse/MODBULKOPS-53) - The number of "Records matched" does not match the number of found records by Query
* [MODBULKOPS-41](https://issues.folio.org/browse/MODBULKOPS-41) - "Item effective location" is not changed in the Preview on the Are you sure form and Preview of changed records (as well as in csv files) according to changed Temporary location
* [MODBULKOPS-40](https://issues.folio.org/browse/MODBULKOPS-40) - Fields are incorrectly formatted in Preview/Files
* [MODBULKOPS-38](https://issues.folio.org/browse/MODBULKOPS-38) - Holdings without "Source" populated cannot be updated
* [MODBULKOPS-37](https://issues.folio.org/browse/MODBULKOPS-37) - EDIT preview should display changes for all matched records
* [MODBULKOPS-36](https://issues.folio.org/browse/MODBULKOPS-36) - 10K items failed to upload propose-changes json file
* [MODBULKOPS-15](https://issues.folio.org/browse/MODBULKOPS-15) - Upgrade dependencies; fix jackson and snakeyaml vulns
