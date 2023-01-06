# mod-bulk-operations self evaluation criteria

- [x] Upon acceptance, code author(s) agree to have source code canonically in folio-org github
- [x] Copyright assigned to OLF
- [x] Uses Apache 2.0 license
- [x] Third party dependencies use an Apache 2.0 compatible license
- [x] Module’s repository includes a compliant Module Descriptor
- [x] Modules must declare all consumed interfaces in the Module Descriptor “requires” and “optional” sections
- [x] Environment vars are documented in the ModuleDescriptor
- [x] Back-end modules must define endpoints consumable by other modules in the Module Descriptor “provides” section
- [x] All API endpoints are documented in RAML or **OpenAPI**
- [x] All API endpoints protected with appropriate permissions
- [x] No excessive permissions granted to the module
- [x] Code of Conduct statement in repository
- [x] Installation documentation included
- [x] Contribution guide is included in repo
- [ ] Module provides ~~reference~~ and/or **sample data** **Note**: we don't plan to support sample or reference data
- [x] Personal data form is completed, accurate, and provided as PERSONAL_DATA_DISCLOSURE.md file
- [x] Sensitive information is not checked into git repository
- [x] Module is written in a language and framework that FOLIO development teams are familiar with
      (Spring Way)
- [x] Back-end modules are based on Maven/JDK ~~11~~ **17, per
      [recent discussions](https://folio-project.slack.com/archives/C58TABALV/p1658913892197899?thread_ts=1658334995.769609&cid=C58TABALV)**
      and provide a Dockerfile
- [ ] Integration (API) tests written in Karate if applicable **(will be implemented in 157+ sprints)**
- [x] Back-end unit tests at 80% coverage
- [x] Data is segregated by tenant at the storage layer
- [x] Back-end modules don’t access data in DB schemas other than their own and public
- [x] Tenant data is segregated at the transit layer
- [x] Back-end modules respond with a tenant’s content based on x-okapi-tenant header
- [x] Standard GET /admin/health endpoint returning a 200 response -_note: read more at
      [https://wiki.folio.org/display/DD/Back+End+Module+Health+Check+Protocol](https://wiki.folio.org/display/DD/Back+End+Module+Health+Check+Protocol)_
- [x] HA compliant
- [x] Module only uses FOLIO interfaces already provided by previously accepted modules _e.g. a UI
      module cannot be accepted that relies on an interface only provided by a back end module that
      hasn’t been accepted yet_
  - For the converse, mod-circulation relied upon the previous `calendar 4.0` interface. This has
    been upgraded to `5.0` in this module, so PR folio-org/mod-circulation#1167 has been created to
    adapt the current `mod-circulation` to work with the new `mod-calendar`.
- [x] Module only uses existing infrastructure / platform technologies (PostgreSQL)
- [ ] ~~Integration with any third party system (outside of the FOLIO environment) tolerates the
      absence of configuration / presence of the system gracefully.~~ N/A
- [ ] ~~Front-end modules: builds are Node 16/Yarn 1~~ N/A
- [ ] ~~Front-end unit tests written in Jest/RTL at 80% coverage~~ N/A
- [ ] ~~Front-end End-to-end tests written in Cypress, if applicable~~ N/A
- [ ] ~~Front-end modules have i18n support via react-intl and an en.json file with English texts~~
      N/A
- [ ] ~~Front-end modules have WCAG 2.1 AA compliance as measured by a current major version of axe
      DevTools Chrome Extension~~ N/A
- [ ] ~~Front-end modules use the current version of Stripes~~ N/A
- [ ] ~~Front-end modules follow relevant existing UI layouts, patterns and norms -_note: read more
      about current practices at
      [https://ux.folio.org/docs/all-guidelines/](https://ux.folio.org/docs/all-guidelines/)_~~ N/A
- [ ] ~~Front-end modules must work in the latest version of Chrome (the supported runtime
      environment)~~ N/A
- [x] sonarqube hasn't identified any issues
  - There's a few false positives that sonar is reporting:
    - Duplication across entity classes `NormalOpening` and `ExceptionRange`. The only duplication
      is that two variables have the same names and annotations, however, it's triggered sonarqube
      to report a duplicated block as a code smell.
    - Sonar reports that `.query` on the JDBC object used in `CustomTenantService` can return null.
      [This is false](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcOperations.html#query-java.lang.String-org.springframework.jdbc.core.RowMapper-).
    - Similarly, it reports that `.queryForObject` on the same JDBC object in `RMBOpeningMapper` is
      deprecated. That is
      [also false](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcOperations.html#query-java.lang.String-org.springframework.jdbc.core.RowMapper-).
  - These should be marked as false positives accordingly in sonarcloud
