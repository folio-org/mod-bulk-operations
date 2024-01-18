package org.folio.bulkops;

import static java.lang.String.format;
import static org.folio.bulkops.processor.UserDataProcessor.DATE_TIME_FORMAT;
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
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.s3.client.S3ClientFactory;
import org.folio.s3.client.S3ClientProperties;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = BaseTest.Initializer.class)
@Testcontainers
@AutoConfigureMockMvc
@Log4j2
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseTest {
  public static final String S3_ACCESS_KEY = "minio-access-key";
  public static final String S3_SECRET_KEY = "minio-secret-key";
  public static final int S3_PORT = 9000;
  public static final String BUCKET = "test-bucket";
  public static final String REGION = "us-west-2";
  private static final String MINIO_ENDPOINT;

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

  @MockBean
  public HoldingsClient holdingsClient;
  @MockBean
  public ItemClient itemClient;
  @MockBean
  public UserClient userClient;
  @MockBean
  public LoanTypeClient loanTypeClient;
  @MockBean
  public ConfigurationClient configurationClient;
  @MockBean
  public GroupClient groupClient;
  @MockBean
  public DepartmentClient departmentClient;
  @MockBean
  public AddressTypeClient addressTypeClient;
  @MockBean
  public CustomFieldsClient customFieldsClient;
  @MockBean
  public OkapiClient okapiClient;
  @MockBean
  public LocationClient locationClient;
  @MockBean
  public HoldingsSourceClient holdingsSourceClient;
  @MockBean
  public CallNumberTypeClient callNumberTypeClient;
  @MockBean
  public HoldingsTypeClient holdingsTypeClient;
  @MockBean
  public HoldingsNoteTypeClient holdingsNoteTypeClient;
  @MockBean
  public IllPolicyClient illPolicyClient;
  @MockBean
  public StatisticalCodeClient statisticalCodeClient;
  @MockBean
  public DamagedStatusClient damagedStatusClient;
  @MockBean
  public ItemNoteTypeClient itemNoteTypeClient;
  @MockBean
  public ServicePointClient servicePointClient;
  @MockBean
  public MaterialTypeClient materialTypeClient;
  @MockBean
  public ElectronicAccessRelationshipClient relationshipClient;
  @MockBean
  public InstanceStatusesClient instanceStatusesClient;
  @MockBean
  public ModesOfIssuanceClient modesOfIssuanceClient;
  @MockBean
  public InstanceTypesClient instanceTypesClient;
  @MockBean
  public NatureOfContentTermsClient natureOfContentTermsClient;
  @MockBean
  public InstanceFormatsClient instanceFormatsClient;
  @MockBean
  public InstanceClient instanceClient;

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
}
