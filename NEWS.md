## v2.1.15 - Released 2025/07/23

This release contains bug fix (reverted 'date' for CirculationNote).

## v2.1.14 - Released 2025/07/21

This release contains bug fix.

### Bugs
* [MODBULKOPS-550](https://folio-org.atlassian.net/browse/MODBULKOPS-550) Ramsons - Some Item fields are edited/removed along with bulk edit of Item

## v2.1.13 - Released 2025/05/29

This release contains bug fix.

### Bugs
* [MODBULKOPS-520](https://folio-org.atlassian.net/browse/MODBULKOPS-520) Error after "Run Query" when the UI is set to a language other than English


## v2.1.12 - Released 2025/01/30

This release contains improvements of data import result processing.

## v2.1.11 - Released 2025/01/29

This release contains fix for redundant data import result processing calls.

## v2.1.10 - Released 2025/01/23

This release contains optimizations for a new platform and adding missing interfaces.

### Bugs
* [MODBULKOPS-457](https://folio-org.atlassian.net/browse/MODBULKOPS-457) "Something went wrong" error appears and list of Users is empty in Bulk edit app on Eureka environment (reverting couple of changes)

## v2.1.9 - Released 2025/01/15

This release contains optimizations for a new platform.

### Bugs
* [MODBULKOPS-457](https://folio-org.atlassian.net/browse/MODBULKOPS-457) "Something went wrong" error appears and list of Users is empty in Bulk edit app on Eureka environment
* [MODBULKOPS-456](https://folio-org.atlassian.net/browse/MODBULKOPS-456) Files downloading improvement

## v2.1.8 - Released 2024/12/23

This release contains upgrading version dependencies of fqm-query-processor.

### Bugs
* [MODBULKOPS-449](https://folio-org.atlassian.net/browse/MODBULKOPS-449) Query search failed in Bulk Operations

## v2.1.7 - Released 2024/12/13

This release contains optimization of memory utilization, upgrade dependencies.

### Bugs
* [MODBULKOPS-440](https://folio-org.atlassian.net/browse/MODBULKOPS-440) Upgrade vulnerable dependencies for Ramsons

### Technical tasks
* [MODBULKOPS-433](https://folio-org.atlassian.net/browse/MODBULKOPS-433) Optimisation of memory utilization

## v2.1.6 - Released 2024/12/09

This release contains bugfixes.

### Bugs
* [MODBULKOPS-434](https://folio-org.atlassian.net/browse/MODBULKOPS-434) For Bulk edit of MARC Instances Confirmation screen fails to display when no changes are required
* [MODBULKOPS-421](https://folio-org.atlassian.net/browse/MODBULKOPS-421) Cataloged date omitted from Are you sure? form
* [MODDATAIMP-1124](https://folio-org.atlassian.net/browse/MODDATAIMP-1124) Bulk edit MARC flow fails to commit by User, works only by Admin for single records


## v2.1.5 - Released 2024/12/05

This release has logic to avoid self invocation

### Bugs
* [EUREKA-559](https://folio-org.atlassian.net/browse/EUREKA-559) Errors on commit stage in Bulk edit when update Instances and apply changes to Holdings, Items on ECS bugfest Eureka environment

## v2.1.4 - Released 2024/12/03

This release contains bugfixes.

### Bugs
* [MODBULKOPS-423](https://folio-org.atlassian.net/browse/MODBULKOPS-423) For bulk edit of MARC Instances file with changed records is available for downloading before commit completes
* [MODBULKOPS-416](https://folio-org.atlassian.net/browse/MODBULKOPS-416) MARC instances set as deleted cannot be previewed or updated

### Technical tasks
* [MODBULKOPS-424](https://folio-org.atlassian.net/browse/MODBULKOPS-424) Check Data import profile creation and  update mod bulk operation data import job profile

## v2.1.3 - Released 2024/11/26

This release contains bugfixes.

### Bugs
* [MODBULKOPS-393](https://folio-org.atlassian.net/browse/MODBULKOPS-393) Issues related to display errors from DI on Confirmation screen of bulk edit MARC fields
* [MODBULKOPS-341](https://folio-org.atlassian.net/browse/MODBULKOPS-341) Errors on Confirmation screen when bulk edit Instances on ECS environment and apply changes to Holdings, Items
* [MODBULKOPS-322](https://folio-org.atlassian.net/browse/MODBULKOPS-322) Specific case of downloading empty file with matched Holdings records

## v2.1.2 - Released 2024/11/15

This release contains bugfixes.

### Bugs
* [MODBULKOPS-422](https://folio-org.atlassian.net/browse/MODBULKOPS-422) Missing interface dependencies in module descriptor
* [MODBULKOPS-407](https://folio-org.atlassian.net/browse/MODBULKOPS-407) Dependency upgrades for Ramsons
* [MODBULKOPS-402](https://folio-org.atlassian.net/browse/MODBULKOPS-402) Issues with bulk edit of URL relationship on non-ECS and ECS environments
* [MODBULKOPS-393](https://folio-org.atlassian.net/browse/MODBULKOPS-393) Issues related to display errors from DI on Confirmation screen of bulk edit MARC fields
* [MODBULKOPS-383](https://folio-org.atlassian.net/browse/MODBULKOPS-383) 404 Not Found error (instead of optimistic locking error) on Confirmation screen when bulk edit Items via Central tenant
* [MODBULKOPS-322](https://folio-org.atlassian.net/browse/MODBULKOPS-322) Specific case of downloading empty file with matched Holdings records

* ## v2.1.1 - Released 2024/11/08

This release contains bugfixes.

### Bugs
* [MODEXPW-396](https://folio-org.atlassian.net/browse/MODEXPW-396) Allow additional item status updates (changes on bulk-operation)
* [MODBULKOPS-393](https://folio-org.atlassian.net/browse/MODBULKOPS-393) Issues related to display errors from DI on Confirmation screen of bulk edit MARC fields
* [MODBULKOPS-348](https://folio-org.atlassian.net/browse/MODBULKOPS-348) For bulk edit of MARC fields “Are you sure“ preview is populated based on .mrc file
* [MODBULKOPS-340](https://folio-org.atlassian.net/browse/MODBULKOPS-340) "Something went wrong" error when click "Confirm changes" button while bulk edit of MARC fields

## v2.1.0 - Released 2024/11/01

### Technical tasks
* [MODBULKOPS-384](https://folio-org.atlassian.net/browse/MODBULKOPS-384) Support Eureka permissions model for bulk operations (write operations)
* [MODBULKOPS-370](https://folio-org.atlassian.net/browse/MODBULKOPS-370) Upgrade `holdings-storage` to 8.0
* [MODBULKOPS-361](https://folio-org.atlassian.net/browse/MODBULKOPS-361) Rename module permissions
* [MODBULKOPS-358](https://folio-org.atlassian.net/browse/MODBULKOPS-358) Add permissions to specify bulk-edit operations
* [MODBULKOPS-350](https://folio-org.atlassian.net/browse/MODBULKOPS-350) API version update
* [MODBULKOPS-344](https://folio-org.atlassian.net/browse/MODBULKOPS-344) Improve clients invocation logs
* [MODBULKOPS-331](https://folio-org.atlassian.net/browse/MODBULKOPS-331) Optimisation of FQM query invocation
* [MODBULKOPS-328](https://folio-org.atlassian.net/browse/MODBULKOPS-328) Update bulk-edit interface version
* [MODBULKOPS-314](https://folio-org.atlassian.net/browse/MODBULKOPS-314) Central tenant edit permissions handling
* [MODBULKOPS-306](https://folio-org.atlassian.net/browse/MODBULKOPS-306) Bump the FQM interface dependency versions in mod-bulk-operations

### Stories
* [MODBULKOPS-375](https://folio-org.atlassian.net/browse/MODBULKOPS-375) Retrieve MARC record from SRS after completion of bulk edit
* [MODBULKOPS-360](https://folio-org.atlassian.net/browse/MODBULKOPS-360) Include Tentant in columns selection
* [MODBULKOPS-351](https://folio-org.atlassian.net/browse/MODBULKOPS-351) Synchronize data used for bulk edit of MARC fields between “Bulk edit“ and “Inventory“ apps
* [MODBULKOPS-329](https://folio-org.atlassian.net/browse/MODBULKOPS-329) Include tenantId in Item's and Holdings' Notes names in ECS
* [MODBULKOPS-323](https://folio-org.atlassian.net/browse/MODBULKOPS-323) Update 005 in preview records on Are you sure form
* [MODBULKOPS-313](https://folio-org.atlassian.net/browse/MODBULKOPS-313) Importing errors processing
* [MODBULKOPS-312](https://folio-org.atlassian.net/browse/MODBULKOPS-312) Data Import client
* [MODBULKOPS-311](https://folio-org.atlassian.net/browse/MODBULKOPS-311) Import profile creating
* [MODBULKOPS-308](https://folio-org.atlassian.net/browse/MODBULKOPS-308) Investigate a better way to notify about S3 connectivity issues
* [MODBULKOPS-279](https://folio-org.atlassian.net/browse/MODBULKOPS-279) FQM flow: tenant information populating
* [MODBULKOPS-273](https://folio-org.atlassian.net/browse/MODBULKOPS-273) Instances, Items, holding - UpdateProcessor extension to support tenant information
* [MODBULKOPS-272](https://folio-org.atlassian.net/browse/MODBULKOPS-272) Identifiers flow: entities tenant information support
* [MODBULKOPS-262](https://folio-org.atlassian.net/browse/MODBULKOPS-262) If the note type name contains special characters, display the note type value
* [MODBULKOPS-254](https://folio-org.atlassian.net/browse/MODBULKOPS-254) MARC Instance - InstanceDataProcessor extending
* [MODBULKOPS-249](https://folio-org.atlassian.net/browse/MODBULKOPS-249) MARC Instance - Confirmation screen (MarcInstanceUpdateProcessor)
* [MODBULKOPS-248](https://folio-org.atlassian.net/browse/MODBULKOPS-248) MARC Instance - Download MARC Preview
* [MODBULKOPS-246](https://folio-org.atlassian.net/browse/MODBULKOPS-246) MARC Instance - Find and Replace
* [MODBULKOPS-245](https://folio-org.atlassian.net/browse/MODBULKOPS-245) MARC Instance - Find and Remove Field or Subfield
* [MODBULKOPS-244](https://folio-org.atlassian.net/browse/MODBULKOPS-244) MARC Instance - Find and Append Subfield
* [MODBULKOPS-243](https://folio-org.atlassian.net/browse/MODBULKOPS-243) MARC Instance - Add New Field
* [MODBULKOPS-204](https://folio-org.atlassian.net/browse/MODBULKOPS-204) Item record's column names cleanup

### Bugs
* [MODBULKOPS-355](https://folio-org.atlassian.net/browse/MODBULKOPS-355) "Are you sure" preview displays outdated values after User changed selection on bulk edit form and clicked "Confirm changes"
* [MODBULKOPS-348](https://folio-org.atlassian.net/browse/MODBULKOPS-348) For bulk edit of MARC fields “Are you sure“ preview is populated based on .mrc file
* [MODBULKOPS-345](https://folio-org.atlassian.net/browse/MODBULKOPS-345) Holdings fail to be bulk edited from Central tenant
* [MODBULKOPS-342](https://folio-org.atlassian.net/browse/MODBULKOPS-342) Incorrect display of Instance notes in the Preview of record matched when order of the notes differ in .csv file and Preview
* [MODBULKOPS-324](https://folio-org.atlassian.net/browse/MODBULKOPS-324) 404 Not Found error on Confirmation screen when bulk edit Items via Central tenant
* [MODBULKOPS-321](https://folio-org.atlassian.net/browse/MODBULKOPS-321) "Are you sure" form is uploading for unexpectedly long time when bulk edit is going to complete with many errors
* [MODBULKOPS-303](https://folio-org.atlassian.net/browse/MODBULKOPS-303) Error accordion disappears after refreshing the page
* [MODBULKOPS-290](https://folio-org.atlassian.net/browse/MODBULKOPS-290) 422 Unprocessable Entity error for MARC instances with Instance notes

## v2.0.0 - Released 2024/03/19
This release includes FQM Integration, separate notes by note type, updating Suppress from discovery flag and Staff suppress flag

### Technical tasks
* [UXPROD-3903](https://folio-org.atlassian.net/browse/UXPROD-3903) - Support `data-export-spring` interface 2.0. Breaking change in `data-export-spring` does not impact mod-bulk-operations use
* [MODBULKOPS-197](https://issues.folio.org/browse/MODBULKOPS-197) - Accommodate changes in holdings records schema
* [MODBULKOPS-181](https://issues.folio.org/browse/MODBULKOPS-181) - Always include column with edited properties on Are you sure form

### Stories
* [MODBULKOPS-230](https://issues.folio.org/browse/MODBULKOPS-230) - FQM Integration - Instances
* [MODBULKOPS-226](https://issues.folio.org/browse/MODBULKOPS-226) - Add new displaySummarry field to the Item schema
* [MODBULKOPS-209](https://issues.folio.org/browse/MODBULKOPS-209) - Rendering holdings electronic access properties in .csv file
* [MODBULKOPS-196](https://issues.folio.org/browse/MODBULKOPS-196) - Updating Suppress from discovery flag
* [MODBULKOPS-195](https://issues.folio.org/browse/MODBULKOPS-195) - Updating Staff suppress flag
* [MODBULKOPS-179](https://issues.folio.org/browse/MODBULKOPS-179) - Rendering Instance record data in bulk edit forms and files
* [MODBULKOPS-174](https://issues.folio.org/browse/MODBULKOPS-174) - Spike: Rendering holdings electronic access properties in .csv file
* [MODBULKOPS-172](https://issues.folio.org/browse/MODBULKOPS-172) - BE - Update Item record column names
* [MODBULKOPS-169](https://issues.folio.org/browse/MODBULKOPS-169) - Separate circulation notes in different columns
* [MODBULKOPS-149](https://issues.folio.org/browse/MODBULKOPS-149) - Separate holdings notes by note type
* [MODBULKOPS-144](https://issues.folio.org/browse/MODBULKOPS-144) - Holdings records - electronic access updates
* [MODBULKOPS-131](https://issues.folio.org/browse/MODBULKOPS-131) - Supported bulk edit actions for holdings notes
* [MODBULKOPS-44](https://issues.folio.org/browse/MODBULKOPS-44) - FQM Integration - Deprecate Bulk Operations Query API
* [MODBULKOPS-43](https://issues.folio.org/browse/MODBULKOPS-43) - FQM Integration - FQM Results handling

## v1.1.10 - Released 2024/02/21
This release contains fix for rendering note types.

### Bugs
* [MODBULKOPS-224](https://folio-org.atlassian.net/browse/MODBULKOPS-224) - Not all but up to 10 note types are displayed in Bulk edit

## v1.1.9 - Released 2024/01/15
logging improvements

## v1.1.8 - Released 2024/01/15
logging improvements

## v1.1.7 - Released 2023/12/05
This release includes only folio-s3-client version update.

## v1.1.6 - Released 2023/11/10
This release includes folio-s3-client update.

## v1.1.5 - Released 2023/11/10
This release includes only folio-s3-client version update.

## v1.1.4 - Released 2023/11/09
This release includes only folio-s3-client version update.

## v1.1.3 - Released 2023/11/08
This release includes only folio-s3-client version update.

## v1.1.2 - Released 2023/11/08
This release includes bugs fixes and update folio-s3-client.

### Bugs
* [MODBULKOPS-166](https://issues.folio.org/browse/MODBULKOPS-166) - Suppress Holdings from discovery: changes are applied to no more than 10 associated Items

## v1.1.1 - Released 2023/10/31
This release includes bugs fixes for editing users and items.

### Bugs
* [MODBULKOPS-151](https://issues.folio.org/browse/MODBULKOPS-151) - Columns for all item note types
* [MODBULKOPS-148](https://issues.folio.org/browse/MODBULKOPS-148) - Bulk edit (Local) is not done for the User with multiple departments associated
* [MODBULKOPS-78](https://issues.folio.org/browse/MODBULKOPS-78) - Bulk editing Users records with an invalid data via CSV approach

## v1.1.0 - Released 2023/10/12
This release includes file storage optimization, editing for items notes, suppression status, bugs fixes.

### Technical tasks
* [MODBULKOPS-156](https://issues.folio.org/browse/MODBULKOPS-156) - mod-bulk-operations: spring upgrade
* [MODBULKOPS-136](https://issues.folio.org/browse/MODBULKOPS-136) - Reduce impact of "No changes required" on "Are you sure?" form performance
* [MODBULKOPS-130](https://issues.folio.org/browse/MODBULKOPS-130) - Remove files generated by incompleted jobs after a given number of days
* [MODBULKOPS-121](https://issues.folio.org/browse/MODBULKOPS-121) - Remove files after a given number of days
* [MODBULKOPS-117](https://issues.folio.org/browse/MODBULKOPS-117) - Update required data model for "Suppress from discovery" (Items)
* [MODBULKOPS-112](https://issues.folio.org/browse/MODBULKOPS-112) - File deletion API for Bulk Operations
* [MODBULKOPS-105](https://issues.folio.org/browse/MODBULKOPS-105) - Migrate to folio-spring-support v7.0.0
* [MODBULKOPS-103](https://issues.folio.org/browse/MODBULKOPS-103) - Remote storage writer usage optimization
* [MODBULKOPS-65](https://issues.folio.org/browse/MODBULKOPS-65) - BulkOps: handling CSV <-> Object exceptions

### Stories
* [MODBULKOPS-153](https://issues.folio.org/browse/MODBULKOPS-153) - Provide additional details for Instance (Title, Publisher, Publication date) for holdings record
* [MODBULKOPS-139](https://issues.folio.org/browse/MODBULKOPS-139) - Separate notes in different columns
* [MODBULKOPS-127](https://issues.folio.org/browse/MODBULKOPS-127) - Add endpoint to get distinct list of users on Logs tab
* [MODBULKOPS-126](https://issues.folio.org/browse/MODBULKOPS-126) - Apply to item records when item suppression status differs from holding record
* [MODBULKOPS-113](https://issues.folio.org/browse/MODBULKOPS-113) - Handling system updated fields in csv approach
* [MODBULKOPS-102](https://issues.folio.org/browse/MODBULKOPS-102) - Suppress from discovery holdings records in bulk
* [MODBULKOPS-101](https://issues.folio.org/browse/MODBULKOPS-101) - Supported bulk edit action for item notes
* [MODBULKOPS-98](https://issues.folio.org/browse/MODBULKOPS-98) - Previews reflect fewer records if the record has a field with line breaks

### Bugs
* [MODBULKOPS-137](https://issues.folio.org/browse/MODBULKOPS-137) - Inability to Add and Remove notes of the same type in one Bulk edit
* [MODBULKOPS-135](https://issues.folio.org/browse/MODBULKOPS-135) - Bulk Edit is not done for Holdings, Items without populated "URI" in electronic access
* [MODBULKOPS-94](https://issues.folio.org/browse/MODBULKOPS-94) - Incorrect sort order on the Logs screen
* [MODBULKOPS-93](https://issues.folio.org/browse/MODBULKOPS-93) - Special characters are incorrectly displayed in some fields of bulk edit previews and files
* [MODBULKOPS-74](https://issues.folio.org/browse/MODBULKOPS-74) - Preview of records changed populated on Users app incorrectly when file is not modified
* [MODBULKOPS-57](https://issues.folio.org/browse/MODBULKOPS-57) - Empty "File with updated records" is included in the list of files for downloading when no records have been successfully changed
* [MODBULKOPS-52](https://issues.folio.org/browse/MODBULKOPS-52) - Name of downloaded file from "Logs" tab does not adhere to the naming standard

## v1.0.6 - Released 2023/06/16
This release contains Spring Boot upgrade to 3.0.7

### Bugs
* [MODBULKOPS-109](https://issues.folio.org/browse/MODBULKOPS-109) - Spring Boot 3.0.7 fixing vulns for Orchid

## v1.0.5 - Released 2023/03/30
This release contains minor improvements of retrieving records flow

### Technical tasks
* [MODBULKOPS-90](https://issues.folio.org/browse/MODBULKOPS-90) - Remove redundant data-export-worker call

## v1.0.4 - Released 2023/03/28
This release contains minor performance improvements and bug fixes

### Technical tasks
* [MODBULKOPS-88](https://issues.folio.org/browse/MODBULKOPS-88) - instanceTitle converter rework

### Bugs
* [MODBULKOPS-67](https://issues.folio.org/browse/MODBULKOPS-67) - Item with status "Checked Out" or "Paged" is not edited if bulk edit includes editing by "Status" field

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
