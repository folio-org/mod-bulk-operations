## v2.3.0 - Released 2025/04/16

This release contains performance and maintenance improvements:
* record fetching logic transferred from mod-data-export-worker
* using FQM as data source for UUID identifiers
* update logic for instances, holdings records and items switched from PUT to PATCH
* bugfixes
* dependencies and Soring upgrades

### Bug fixes
[MODBULKOPS-505](https://folio-org.atlassian.net/browse/MODBULKOPS-505) Incorrect error text in .csv file with errors when optimistic locking error is reported for bulk edit of Instances with source MARC
[MODBULKOPS-532](https://folio-org.atlassian.net/browse/MODBULKOPS-532) 422 error when via FOLIO flow do bulk edit of MARC Instance with linked authority record to 6XX field
[MODBULKOPS-516](https://folio-org.atlassian.net/browse/MODBULKOPS-516) FAILED (UNKNOWN) in Electronic access, Subject, Classification columns in .csv file with updated records
[MODBULKOPS-632](https://folio-org.atlassian.net/browse/MODBULKOPS-632) "Errors" accordion is not displayed and .csv file with errors is not available in some cases
[MODBULKOPS-531](https://folio-org.atlassian.net/browse/MODBULKOPS-531) Values populated in "Subject" column of bulk edit MARC Instances do not correspond to how they are displayed in "Inventory" app
[MODBULKOPS-548](https://folio-org.atlassian.net/browse/MODBULKOPS-548) On ECS, local reference data not found due to the wrong tenant
[MODBULKOPS-545](https://folio-org.atlassian.net/browse/MODBULKOPS-545) User field "pronouns" is cleared along with bulk edit of User
[MODBULKOPS-557](https://folio-org.atlassian.net/browse/MODBULKOPS-557) Thrillium - Some Item fields are edited/removed along with bulk edit of Item
[MODBULKOPS-582](https://folio-org.atlassian.net/browse/MODBULKOPS-582) ECS - Filter out shadow users when bulk editing users in a member tenant
[MODBULKOPS-504](https://folio-org.atlassian.net/browse/MODBULKOPS-504) Incorrect updates-preview .csv file is downloaded from "Logs" when note, subject are edited but changes are not applied
[MODBULKOPS-560](https://folio-org.atlassian.net/browse/MODBULKOPS-560) Notes, Subject columns are populated with not removed values on .csv files when it is expected to be empty
[MODBULKOPS-586](https://folio-org.atlassian.net/browse/MODBULKOPS-586) ECS - Incorrect error message when editing instances shared from  a member tenant
[MODBULKOPS-590](https://folio-org.atlassian.net/browse/MODBULKOPS-590) Upload .csv file with large number of identifiers fails on Sprint testing environment
[MODBULKOPS-559](https://folio-org.atlassian.net/browse/MODBULKOPS-559) Make link to download file with identifiers of the records affected by bulk update available only when file is ready for download
[MODBULKOPS-568](https://folio-org.atlassian.net/browse/MODBULKOPS-568) Items with corrupted circulationNotes.date go to Errors on the Preview of records matched
[MODBULKOPS-578](https://folio-org.atlassian.net/browse/MODBULKOPS-578) Incorrect changed-records .csv file is downloaded from Confirmation screen, "Logs" when subject is edited but changes are not applied
[MODBULKOPS-530](https://folio-org.atlassian.net/browse/MODBULKOPS-530) Number of fields are not mapped to Subject in bulk edit of Instances with source MARC
[MODBULKOPS-591](https://folio-org.atlassian.net/browse/MODBULKOPS-591) ECS | Upload of Holdings records by Instance HRIDs fails in Central tenant
[MODBULKOPS-627](https://folio-org.atlassian.net/browse/MODBULKOPS-627) ECS | Upload of Inventory records by UUIDs fails in Central, Member tenants
[MODBULKOPS-635](https://folio-org.atlassian.net/browse/MODBULKOPS-635) ECS, Central tenant | UNKNOWN for Holdings, Items local "Statistical codes" in .csv file with matching records
[MODBULKOPS-636](https://folio-org.atlassian.net/browse/MODBULKOPS-636) ECS - Upload fails in case "Duplicates across tenants" error is expected
[MODBULKOPS-637](https://folio-org.atlassian.net/browse/MODBULKOPS-637) UUID is empty when Instance with Linked Data source is filtered out
[MODBULKOPS-638](https://folio-org.atlassian.net/browse/MODBULKOPS-638) Filename with errors is incorrect when records are uploaded via Query
[MODBULKOPS-642](https://folio-org.atlassian.net/browse/MODBULKOPS-642) Missing duplicate detection for UUID identifiers
[MODBULKOPS-604](https://folio-org.atlassian.net/browse/MODBULKOPS-604) Unexpectedly long time of upload ~100K Items records in Bulk edit on Sprint testing environment (file with identifiers different from UUIDs)
[MODBULKOPS-643](https://folio-org.atlassian.net/browse/MODBULKOPS-643) "Staff suppress" column is empty on Matching preview when in "Build query" plugin the value is "False" for the record
[MODBULKOPS-646](https://folio-org.atlassian.net/browse/MODBULKOPS-646) Incorrect representation of optimistic locking error in Bulk edit
[MODBULKOPS-609](https://folio-org.atlassian.net/browse/MODBULKOPS-609) Incorrect Preview of records changed when edit FOLIO & MARC Instances via FOLIO flow
[MODBULKOPS-603](https://folio-org.atlassian.net/browse/MODBULKOPS-603) Occasionally Matching preview displays not all uploaded records when upload .csv file with large number of identifiers
[MODBULKOPS-649](https://folio-org.atlassian.net/browse/MODBULKOPS-649) processedRecords discrepancy fixing
[MODBULKOPS-656](https://folio-org.atlassian.net/browse/MODBULKOPS-656) While bulk edit Users in tenant time zone different from UTC Birth date, Date enrolled sometimes are displayed taking into account tenant time zone

### Stories
[MODBULKOPS-189](https://folio-org.atlassian.net/browse/MODBULKOPS-189) Adding Electronic access Columns for Instance Records in Preview and Are you sure? Forms
[MODBULKOPS-190](https://folio-org.atlassian.net/browse/MODBULKOPS-190) Adding Subject Column for Instance Records in Preview and Are you sure? Forms
[MODBULKOPS-491](https://folio-org.atlassian.net/browse/MODBULKOPS-491) Adding Electronic access Columns for Instance Records in Confirmation Form
[MODBULKOPS-494](https://folio-org.atlassian.net/browse/MODBULKOPS-494) Adding Subject Column for Instance Records in Confirmation Form
[MODBULKOPS-191](https://folio-org.atlassian.net/browse/MODBULKOPS-191) Adding Classification Columns for Instance Records in Preview and Are you sure? Forms
[MODBULKOPS-187](https://folio-org.atlassian.net/browse/MODBULKOPS-187) Adding Publication Column for Instance Records in Preview and Are you sure? Forms
[MODBULKOPS-498](https://folio-org.atlassian.net/browse/MODBULKOPS-498) Adding Publication Column for Instance Records in Confirmation Form
[MODBULKOPS-508](https://folio-org.atlassian.net/browse/MODBULKOPS-508) Adding Electronic access Columns for Instance Records in Preview and Are you sure? Forms
[MODBULKOPS-326](https://folio-org.atlassian.net/browse/MODBULKOPS-326) Bulk Edit: Ability to select Instance system control number
[MODBULKOPS-533](https://folio-org.atlassian.net/browse/MODBULKOPS-533) ECS - Filter out local instances when querying in Central tenant
[MODBULKOPS-523](https://folio-org.atlassian.net/browse/MODBULKOPS-523) Notify user if invalid MARC record prevents bulk edit
[MODBULKOPS-299](https://folio-org.atlassian.net/browse/MODBULKOPS-299) Set records for deletion
[MODBULKOPS-442](https://folio-org.atlassian.net/browse/MODBULKOPS-442) Preserve MARC subfields order
[MODBULKOPS-499](https://folio-org.atlassian.net/browse/MODBULKOPS-499) Removing entire value of a subfield
[MODBULKOPS-555](https://folio-org.atlassian.net/browse/MODBULKOPS-555) Instance, Holdings, Item schemas updating - new fields
[MODBULKOPS-201](https://folio-org.atlassian.net/browse/MODBULKOPS-201) Document API calls for scripting bulk edit
[MODBULKOPS-563](https://folio-org.atlassian.net/browse/MODBULKOPS-563) Enhancement to Item Status changes: Lost and paid to withdrawn
[MODBULKOPS-541](https://folio-org.atlassian.net/browse/MODBULKOPS-541) Unify format of Statistical code displayed all inventory types
[MODBULKOPS-496](https://folio-org.atlassian.net/browse/MODBULKOPS-496) Adding Classification Columns for Instance Records in Confirmation Form
[MODBULKOPS-439](https://folio-org.atlassian.net/browse/MODBULKOPS-439) Investigate performance improvements for creating error and warning logs
[MODBULKOPS-525](https://folio-org.atlassian.net/browse/MODBULKOPS-525) Adding Electronic access Columns for Instance Records in Preview and Are you sure? Forms
[MODBULKOPS-536](https://folio-org.atlassian.net/browse/MODBULKOPS-536) Migrate settings from mod-configuration to mod-settings
[MODBULKOPS-561](https://folio-org.atlassian.net/browse/MODBULKOPS-561) Provide ability to enable/disable switch to FQM
[MODBULKOPS-553](https://folio-org.atlassian.net/browse/MODBULKOPS-553) Adjust Item schema in Bulk edit to support functionality implemented in UXPROD-5462
[MODBULKOPS-534](https://folio-org.atlassian.net/browse/MODBULKOPS-534) Make multiple notes separators consistent
[MODBULKOPS-593](https://folio-org.atlassian.net/browse/MODBULKOPS-593) Align Bulk edit update of instance with preceding/succeeding titles with  Inventory update
[MODBULKOPS-540](https://folio-org.atlassian.net/browse/MODBULKOPS-540) ECS - Filter out shadow users when querying in Central tenant
[MODBULKOPS-570](https://folio-org.atlassian.net/browse/MODBULKOPS-570) Changing Version of Lib-fqm-query-processor to resolve 500
[MODBULKOPS-577](https://folio-org.atlassian.net/browse/MODBULKOPS-577) ECS - Filter out shadow users when querying in Central tenant
[MODBULKOPS-633](https://folio-org.atlassian.net/browse/MODBULKOPS-633) Prevent bulk editing DCB type users
[MODBULKOPS-622](https://folio-org.atlassian.net/browse/MODBULKOPS-622) ECS - Apply tenant time zone on bulk edit preview screen
[MODBULKOPS-647](https://folio-org.atlassian.net/browse/MODBULKOPS-647) Spike - Investigate how to display upload results consistently on bulk edit Logs

### Technical tasks
[MODBULKOPS-517](https://folio-org.atlassian.net/browse/MODBULKOPS-517) Profile schema creation
[MODBULKOPS-518](https://folio-org.atlassian.net/browse/MODBULKOPS-518) Profiles API implementation
[MODBULKOPS-512](https://folio-org.atlassian.net/browse/MODBULKOPS-512) Import workflow refactoring - using only one action for updating SRS records
[MODBULKOPS-617](https://folio-org.atlassian.net/browse/MODBULKOPS-617) Switching updating methods: PUT to PATCH (holdings)
[MODBULKOPS-562](https://folio-org.atlassian.net/browse/MODBULKOPS-562) Missing interface dependencies in module descriptor
[MODBULKOPS-333](https://folio-org.atlassian.net/browse/MODBULKOPS-333) Stage 2a - FQM Performance (FQL + identifiers): csv, mrc files converters moving, alignment
[MODBULKOPS-335](https://folio-org.atlassian.net/browse/MODBULKOPS-335) Stage 2b - FQM Performance (identifiers): entities extractors moving
[MODBULKOPS-336](https://folio-org.atlassian.net/browse/MODBULKOPS-336) Stage 3a - FQM Integration (identifiers): switching matched records flow to use mod-bulk-operations capabilities
[MODBULKOPS-337](https://folio-org.atlassian.net/browse/MODBULKOPS-337) Stage 3b - FQM Performance (FQL): switching matched records flow to use mod-bulk-operations capabilities
[MODBULKOPS-514](https://folio-org.atlassian.net/browse/MODBULKOPS-514) Stage 3b - FQM Performance (FQL): FQM data fetcher
[MODBULKOPS-519](https://folio-org.atlassian.net/browse/MODBULKOPS-519) Remove PLATFORM env var, drop Okapi code
[MODBULKOPS-575](https://folio-org.atlassian.net/browse/MODBULKOPS-575) Update "Expiration date" format in .csv file to date only
[MODBULKOPS-304](https://folio-org.atlassian.net/browse/MODBULKOPS-304) Tech Debt: enable check-style plugin
[MODBULKOPS-160](https://folio-org.atlassian.net/browse/MODBULKOPS-160) TechDebt (maintainability): Switch preview generating from CSV to JSON
[MODBULKOPS-567](https://folio-org.atlassian.net/browse/MODBULKOPS-567) Update "Date enrolled" format in .csv file to date only
[MODBULKOPS-602](https://folio-org.atlassian.net/browse/MODBULKOPS-602) Set FQM_QUERY_APPROACH to true by default
[MODBULKOPS-584](https://folio-org.atlassian.net/browse/MODBULKOPS-584) TechDebt (maintainability): Switch preview generating from CSV to JSON - ECS adjustments
[MODBULKOPS-573](https://folio-org.atlassian.net/browse/MODBULKOPS-573) Switching updating methods: PUT to PATCH (instances)
[MODBULKOPS-581](https://folio-org.atlassian.net/browse/MODBULKOPS-581) Upgrade module to SpringBoot4.0 and Spring7.0
[MODBULKOPS-623](https://folio-org.atlassian.net/browse/MODBULKOPS-623) Add S3_SUB_PATH Environment Variable Support
[MODBULKOPS-620](https://folio-org.atlassian.net/browse/MODBULKOPS-620) Query Flow: FQM/Inventory/User schemas alignment
[MODBULKOPS-616](https://folio-org.atlassian.net/browse/MODBULKOPS-616) Switching updating methods: PUT to PATCH (items)
[MODBULKOPS-535](https://folio-org.atlassian.net/browse/MODBULKOPS-535) Upgrade Spring Boot version
[MODBULKOPS-346](https://folio-org.atlassian.net/browse/MODBULKOPS-346) Stage 4 - FQM Performance (UUIDs): switching matched records flow to use FQM instead of retrieving by identifiers

### Tech debts
[MODBULKOPS-500](https://folio-org.atlassian.net/browse/MODBULKOPS-500) Remove unused openssh sshpass (sftp) from Dockerfile
[MODBULKOPS-571](https://folio-org.atlassian.net/browse/MODBULKOPS-571) Tech Debt: Handle EntityTypeService errors gracefully
[MODBULKOPS-418](https://folio-org.atlassian.net/browse/MODBULKOPS-418) TD: Improvement of exception handling

## v2.2.1 - Released 2025/03/27

This release contains tech debt, data import integration story and bugfix for BOM.

### Tech debts
* [MODBULKOPS-500](https://folio-org.atlassian.net/browse/MODBULKOPS-500) Remove unused openssh sshpass (sftp) from Dockerfile

### Stories
* [MODBULKOPS-475](https://folio-org.atlassian.net/browse/MODBULKOPS-475) Data Import completion event and log entries availability synchronization

### Bugs
* [MODBULKOPS-501](https://folio-org.atlassian.net/browse/MODBULKOPS-501) First user UUID record from .csv file with "UTF-8 with BOM" encoding is processed as error in Bulk edit (Identifier > Users > User UUIDs)

## v2.2.0 - Released 2025/03/13

This release contains new functionality for errors handling and MARC operations, bugfixes, dependencies upgrading and migration to Java 21.

### Technical tasks
* [MODBULKOPS-482](https://folio-org.atlassian.net/browse/MODBULKOPS-482) Update to mod-bulk-operations Java 21
* [MODBULKOPS-468](https://folio-org.atlassian.net/browse/MODBULKOPS-468) Improvement of data import errors saving
* [MODBULKOPS-459](https://folio-org.atlassian.net/browse/MODBULKOPS-459) TD: fix unstable testReceiveJobExecutionUpdate unit test
* [MODBULKOPS-456](https://folio-org.atlassian.net/browse/MODBULKOPS-456) Files downloading improvement
* [MODBULKOPS-451](https://folio-org.atlassian.net/browse/MODBULKOPS-451) Rework errors preview
* [MODBULKOPS-433](https://folio-org.atlassian.net/browse/MODBULKOPS-433) Optimisation of memory utilization
* [MODBULKOPS-424](https://folio-org.atlassian.net/browse/MODBULKOPS-424) Check Data import profile creation and  update mod bulk operation data import job profile
* [MODBULKOPS-412](https://folio-org.atlassian.net/browse/MODBULKOPS-412) Add UTF-8 BOM to Exported csv Files
* [MODBULKOPS-406](https://folio-org.atlassian.net/browse/MODBULKOPS-406) Handling Data Import notification
* [MODBULKOPS-398](https://folio-org.atlassian.net/browse/MODBULKOPS-398) TD: Align MARC flow with FOLIO flow
* [MODBULKOPS-379](https://folio-org.atlassian.net/browse/MODBULKOPS-379) TD: Graceful handling S3 errors

### Stories
* [MODEXPW-564](https://folio-org.atlassian.net/browse/MODEXPW-564) Preventing bulk editing corrupted MARC instances  through error handling
* [MODEXPW-547](https://folio-org.atlassian.net/browse/MODEXPW-547) Provide error and warnings for matched records
* [MODBULKOPS-475](https://folio-org.atlassian.net/browse/MODBULKOPS-475) Data Import completion event and log entries availability synchronization
* [MODBULKOPS-449](https://folio-org.atlassian.net/browse/MODBULKOPS-449) Query search failed in Bulk Operations
* [MODBULKOPS-429](https://folio-org.atlassian.net/browse/MODBULKOPS-429) Commit changes - MARC Bib records with administrative data
* [MODBULKOPS-428](https://folio-org.atlassian.net/browse/MODBULKOPS-428) Apply changes locally - MARC Bib records with administrative data
* [MODBULKOPS-390](https://folio-org.atlassian.net/browse/MODBULKOPS-390) Previewing bulk edit changes
* [MODBULKOPS-389](https://folio-org.atlassian.net/browse/MODBULKOPS-389) Supported actions for bulk editing Instances statistical codes
* [MODBULKOPS-377](https://folio-org.atlassian.net/browse/MODBULKOPS-377) Create log with error message from another module
* [MODBULKOPS-325](https://folio-org.atlassian.net/browse/MODBULKOPS-325) Modify Find action on Bulk edit form for Inventory records to match part of the text
* [MODBULKOPS-301](https://folio-org.atlassian.net/browse/MODBULKOPS-301) Handling errors and warnings while committing changes
* [MODBULKOPS-270](https://folio-org.atlassian.net/browse/MODBULKOPS-270) Differentiate between errors and warnings (updating schema)
* [MODBULKOPS-247](https://folio-org.atlassian.net/browse/MODBULKOPS-247) MARC Instance - Previewing Records

### Bugs
* [MODBULKOPS-467](https://folio-org.atlassian.net/browse/MODBULKOPS-467) Errors on Confirmation screen are increasing over time for bulk edit of MARC Instances due to reoccurring event of DI job completion
* [MODBULKOPS-457](https://folio-org.atlassian.net/browse/MODBULKOPS-457) "Something went wrong" error appears and list of Users is empty in Bulk edit app on Eureka environment
* [MODBULKOPS-455](https://folio-org.atlassian.net/browse/MODBULKOPS-455) Modify "Remove" action followed by “Find” action on Bulk edit form for Inventory records to remove part of the text
* [MODBULKOPS-454](https://folio-org.atlassian.net/browse/MODBULKOPS-454) Issue with data-import.splitconfig.get
* [MODBULKOPS-440](https://folio-org.atlassian.net/browse/MODBULKOPS-440) Upgrade vulnerable dependencies for Ramsons
* [MODBULKOPS-434](https://folio-org.atlassian.net/browse/MODBULKOPS-434) For Bulk edit of MARC Instances Confirmation screen fails to display when no changes are required
* [MODBULKOPS-423](https://folio-org.atlassian.net/browse/MODBULKOPS-423) For bulk edit of MARC Instances file with changed records is available for downloading before commit completes
* [MODBULKOPS-421](https://folio-org.atlassian.net/browse/MODBULKOPS-421) Cataloged date omitted from Are you sure? form
* [MODBULKOPS-416](https://folio-org.atlassian.net/browse/MODBULKOPS-416) MARC instances set as deleted cannot be previewed or updated
* [MODBULKOPS-402](https://folio-org.atlassian.net/browse/MODBULKOPS-402) Issues with bulk edit of URL relationship on non-ECS and ECS environments
* [MODBULKOPS-393](https://folio-org.atlassian.net/browse/MODBULKOPS-393) Issues related to display errors from DI on Confirmation screen of bulk edit MARC fields
* [MODBULKOPS-341](https://folio-org.atlassian.net/browse/MODBULKOPS-341) Errors on Confirmation screen when bulk edit Instances on ECS environment and apply changes to Holdings, Items

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
