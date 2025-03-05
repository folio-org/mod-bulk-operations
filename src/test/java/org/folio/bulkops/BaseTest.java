package org.folio.bulkops;

import static java.lang.String.format;
import static org.folio.bulkops.processor.folio.UserDataProcessor.DATE_TIME_FORMAT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.AddressTypeClient;
import org.folio.bulkops.client.CallNumberTypeClient;
import org.folio.bulkops.client.ConfigurationClient;
import org.folio.bulkops.client.CustomFieldsClient;
import org.folio.bulkops.client.DamagedStatusClient;
import org.folio.bulkops.client.DepartmentClient;
import org.folio.bulkops.client.ElectronicAccessRelationshipClient;
import org.folio.bulkops.client.GroupClient;
import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.client.HoldingsNoteTypeClient;
import org.folio.bulkops.client.HoldingsSourceClient;
import org.folio.bulkops.client.HoldingsTypeClient;
import org.folio.bulkops.client.IllPolicyClient;
import org.folio.bulkops.client.InstanceFormatsClient;
import org.folio.bulkops.client.InstanceNoteTypesClient;
import org.folio.bulkops.client.InstanceStatusesClient;
import org.folio.bulkops.client.InstanceClient;
import org.folio.bulkops.client.InstanceTypesClient;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.client.ItemNoteTypeClient;
import org.folio.bulkops.client.LoanTypeClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.client.MaterialTypeClient;
import org.folio.bulkops.client.ModesOfIssuanceClient;
import org.folio.bulkops.client.NatureOfContentTermsClient;
import org.folio.bulkops.client.OkapiClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.client.ServicePointClient;
import org.folio.bulkops.client.StatisticalCodeClient;
import org.folio.bulkops.client.StatisticalCodeTypeClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.s3.client.S3ClientFactory;
import org.folio.s3.client.S3ClientProperties;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = BaseTest.Initializer.class)
@Testcontainers
@AutoConfigureMockMvc
@Log4j2
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
@EmbeddedKafka(topics = { "folio.Default.diku.DI_JOB_COMPLETED" })
@EnableKafka
public abstract class BaseTest {
  public static final String S3_ACCESS_KEY = "minio-access-key";
  public static final String S3_SECRET_KEY = "minio-secret-key";
  public static final int S3_PORT = 9000;
  public static final String BUCKET = "test-bucket";
  public static final String REGION = "us-west-2";
  private static final String MINIO_ENDPOINT;
  private static final String OKAPI_URL = "http://okapi:9130";

  public static final PostgreSQLContainer<?> postgresDBContainer;
  private static final GenericContainer<?> s3;
  public static final RemoteFileSystemClient client;
  static {
    postgresDBContainer = new PostgreSQLContainer<>("postgres:12");
    postgresDBContainer.start();
    s3 = new GenericContainer<>("minio/minio:latest")
        .withEnv("MINIO_ACCESS_KEY", S3_ACCESS_KEY)
        .withEnv("MINIO_SECRET_KEY", S3_SECRET_KEY)
        .withCommand("server /data")
        .withExposedPorts(S3_PORT)
        .waitingFor(new HttpWaitStrategy().forPath("/minio/health/ready")
          .forPort(S3_PORT)
          .withStartupTimeout(Duration.ofSeconds(10))
        );
    s3.start();
    MINIO_ENDPOINT = format("http://%s:%s", s3.getHost(), s3.getFirstMappedPort());
    client =
        new RemoteFileSystemClient(S3ClientFactory.getS3Client(S3ClientProperties.builder()
            .endpoint(MINIO_ENDPOINT)
            .secretKey(S3_SECRET_KEY)
            .accessKey(S3_ACCESS_KEY)
            .bucket(BUCKET)
            .awsSdk(false)
            .region(REGION)
            .build()));
  }

  public static LocalDateTimeDeserializer localDateTimeDeserializer = new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .registerModule(new JavaTimeModule().addDeserializer(LocalDateTime.class, localDateTimeDeserializer))
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

  protected static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidXNlcl9pZCI6IjFkM2I1OGNiLTA3YjUtNWZjZC04YTJhLTNjZTA2YTBlYjkwZiIsImlhdCI6MTYxNjQyMDM5MywidGVuYW50IjoiZGlrdSJ9.2nvEYQBbJP1PewEgxixBWLHSX_eELiBEBpjufWiJZRs";
  protected static final String TENANT = "diku";

  @MockitoBean
  public HoldingsClient holdingsClient;
  @MockitoBean
  public ItemClient itemClient;
  @MockitoBean
  public UserClient userClient;
  @MockitoBean
  public LoanTypeClient loanTypeClient;
  @MockitoBean
  public ConfigurationClient configurationClient;
  @MockitoBean
  public GroupClient groupClient;
  @MockitoBean
  public DepartmentClient departmentClient;
  @MockitoBean
  public AddressTypeClient addressTypeClient;
  @MockitoBean
  public CustomFieldsClient customFieldsClient;
  @MockitoBean
  public OkapiClient okapiClient;
  @MockitoBean
  public LocationClient locationClient;
  @MockitoBean
  public HoldingsSourceClient holdingsSourceClient;
  @MockitoBean
  public CallNumberTypeClient callNumberTypeClient;
  @MockitoBean
  public HoldingsTypeClient holdingsTypeClient;
  @MockitoBean
  public HoldingsNoteTypeClient holdingsNoteTypeClient;
  @MockitoBean
  public IllPolicyClient illPolicyClient;
  @MockitoBean
  public StatisticalCodeClient statisticalCodeClient;
  @MockitoBean
  public StatisticalCodeTypeClient statisticalCodeTypeClient;
  @MockitoBean
  public DamagedStatusClient damagedStatusClient;
  @MockitoBean
  public ItemNoteTypeClient itemNoteTypeClient;
  @MockitoBean
  public ServicePointClient servicePointClient;
  @MockitoBean
  public MaterialTypeClient materialTypeClient;
  @MockitoBean
  public ElectronicAccessRelationshipClient relationshipClient;
  @MockitoBean
  public InstanceStatusesClient instanceStatusesClient;
  @MockitoBean
  public ModesOfIssuanceClient modesOfIssuanceClient;
  @MockitoBean
  public InstanceTypesClient instanceTypesClient;
  @MockitoBean
  public NatureOfContentTermsClient natureOfContentTermsClient;
  @MockitoBean
  public InstanceFormatsClient instanceFormatsClient;
  @MockitoBean
  public InstanceClient instanceClient;
  @MockitoBean
  public InstanceNoteTypesClient instanceNoteTypesClient;
  @MockitoBean
  public PrepareSystemUserService prepareSystemUserService;

  @Autowired
  protected MockMvc mockMvc;
  @Autowired
  private FolioModuleMetadata folioModuleMetadata;
  @Autowired
  public ObjectMapper objectMapper;

  public final Map<String, Object> okapiHeaders = new HashMap<>();

  public FolioExecutionContext folioExecutionContext;

  public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
        "spring.datasource.url=" + postgresDBContainer.getJdbcUrl(),
        "spring.datasource.username=" + postgresDBContainer.getUsername(),
        "spring.datasource.password=" + postgresDBContainer.getPassword(),
        "application.remote-files-storage.endpoint=" + MINIO_ENDPOINT,
        "application.remote-files-storage.region=" + REGION,
        "application.remote-files-storage.bucket=" + BUCKET,
        "application.remote-files-storage.accessKey=" + S3_ACCESS_KEY,
        "application.remote-files-storage.secretKey=" + S3_SECRET_KEY,
        "application.remote-files-storage.awsSdk=false");
    }
  }

  @BeforeAll
  static void beforeAll(@Autowired MockMvc mockMvc) {
    setUpTenant(mockMvc);
  }

  @BeforeEach
  void setUp() {
    okapiHeaders.clear();
    okapiHeaders.put(XOkapiHeaders.TENANT, TENANT);
    okapiHeaders.put(XOkapiHeaders.TOKEN, TOKEN);
    okapiHeaders.put(XOkapiHeaders.USER_ID, UUID.randomUUID().toString());

    var localHeaders =
      okapiHeaders.entrySet()
        .stream()
        .filter(e -> e.getKey().startsWith(XOkapiHeaders.OKAPI_HEADERS_PREFIX))
        .collect(Collectors.toMap(Map.Entry::getKey, e -> (Collection<String>)List.of(String.valueOf(e.getValue()))));

    folioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, localHeaders);
  }

  @AfterEach
  void eachTearDown() {
  }

  @SneakyThrows
  protected static void setUpTenant(MockMvc mockMvc) {
    mockMvc.perform(post("/_/tenant")
      .content(asJsonString(new TenantAttributes().moduleTo("mod-bulk-operations")))
      .headers(defaultHeaders())
      .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());
  }

  public static HttpHeaders defaultHeaders() {
    final HttpHeaders httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.put(XOkapiHeaders.TENANT, List.of(TENANT));
    httpHeaders.add(XOkapiHeaders.TOKEN, TOKEN);
    httpHeaders.add(XOkapiHeaders.URL, OKAPI_URL);
    httpHeaders.add(XOkapiHeaders.USER_ID, UUID.randomUUID().toString());

    return httpHeaders;
  }

  @SneakyThrows
  public static String asJsonString(Object value) {
    return OBJECT_MAPPER.writeValueAsString(value);
  }

  public static BulkOperationRuleCollection rules(BulkOperationRule... rules) {
    var uuid = UUID.randomUUID();

    return new BulkOperationRuleCollection()
      .bulkOperationRules(Arrays.stream(rules).map(rule -> rule.bulkOperationId(uuid)).toList())
      .totalRecords(rules.length);
  }

  public static BulkOperationRule rule(UpdateOptionType option, UpdateActionType action, String initial, String updated) {
    return new BulkOperationRule()
      .ruleDetails(new BulkOperationRuleRuleDetails()
        .option(option)
        .actions(Collections.singletonList(new Action()
          .type(action)
            .initial(initial)
          .updated(updated))));
  }

  public static BulkOperationRule rule(UpdateOptionType option, UpdateActionType action, String updated) {
    return rule(option, action, null, updated);
  }

  public static BulkOperationRule rule(UpdateOptionType option, UpdateActionType action, String updated,
                                       List<String> actionsTenants, List<String> ruleTenants) {
    var rule = rule(option, action, null, updated);
    rule.getRuleDetails().setTenants(ruleTenants);
    rule.getRuleDetails().getActions().forEach(act -> act.setTenants(actionsTenants));
    return rule;
  }

  public BulkOperation buildBulkOperation(String fileName, org.folio.bulkops.domain.dto.EntityType entityType, org.folio.bulkops.domain.dto.BulkOperationStep step) {
    return switch (step) {
      case UPLOAD -> BulkOperation.builder()
        .entityType(entityType)
        .linkToMatchedRecordsCsvFile(fileName)
        .build();
      case EDIT -> BulkOperation.builder()
        .entityType(entityType)
        .linkToModifiedRecordsCsvFile(fileName)
        .build();
      case COMMIT -> BulkOperation.builder()
        .entityType(entityType)
        .linkToCommittedRecordsCsvFile(fileName)
        .build();
    };
  }
}
