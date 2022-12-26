package org.folio.bulkops;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.ConfigurationClient;
import org.folio.bulkops.client.GroupClient;
import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.client.HoldingsSourceClient;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.client.LoanTypeClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.processor.DataProcessorFactory;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.s3.client.S3ClientFactory;
import org.folio.s3.client.S3ClientProperties;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.SocketUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = BaseTest.DockerPostgreDataSourceInitializer.class)
@Testcontainers
@AutoConfigureMockMvc
@Log4j2
public abstract class BaseTest {
  public static PostgreSQLContainer<?> postgresDBContainer = new PostgreSQLContainer<>("postgres:13");

  public static RemoteFileSystemClient client;
  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

  protected static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidXNlcl9pZCI6IjFkM2I1OGNiLTA3YjUtNWZjZC04YTJhLTNjZTA2YTBlYjkwZiIsImlhdCI6MTYxNjQyMDM5MywidGVuYW50IjoiZGlrdSJ9.2nvEYQBbJP1PewEgxixBWLHSX_eELiBEBpjufWiJZRs";
  protected static final String TENANT = "diku";

  public static final String S3_ACCESS_KEY = "minio-access-key";
  public static final String S3_SECRET_KEY = "minio-secret-key";
  public static final int S3_PORT = 9000;
  public static final String BUCKET = "test-bucket";
  public static final String REGION = "us-west-2";
  public static String minio_endpoint;

  private static GenericContainer s3;

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
  public LocationClient locationClient;
  @MockBean
  public HoldingsSourceClient holdingsSourceClient;

  @Autowired
  protected MockMvc mockMvc;
  @Autowired
  private FolioModuleMetadata folioModuleMetadata;

  static {
    postgresDBContainer.start();
  }

  public static class DockerPostgreDataSourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
        "spring.datasource.url=" + postgresDBContainer.getJdbcUrl(),
        "spring.datasource.username=" + postgresDBContainer.getUsername(),
        "spring.datasource.password=" + postgresDBContainer.getPassword());
    }
  }

  @BeforeAll
  static void beforeAll(@Autowired MockMvc mockMvc) {
    if (isNull(s3)) {
      setUpMinio();
    }
    if (isNull(client)) {
      setUpClient();
    }
    setUpTenant(mockMvc);
  }

  @BeforeEach
  void setUp() {
    Map<String, Collection<String>> okapiHeaders = new LinkedHashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of(TENANT));
    okapiHeaders.put(XOkapiHeaders.TOKEN, List.of(TOKEN));
//    okapiHeaders.put(XOkapiHeaders.URL, List.of(wireMockServer.baseUrl()));
    okapiHeaders.put(XOkapiHeaders.USER_ID, List.of(UUID.randomUUID().toString()));
    var defaultFolioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders);
    FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(defaultFolioExecutionContext);
  }

  @AfterEach
  void eachTearDown() {
    FolioExecutionScopeExecutionContextManager.endFolioExecutionContext();
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
//    httpHeaders.add(XOkapiHeaders.URL, wireMockServer.baseUrl());
    httpHeaders.add(XOkapiHeaders.TOKEN, TOKEN);
    httpHeaders.add(XOkapiHeaders.USER_ID, UUID.randomUUID().toString());

    return httpHeaders;
  }

  @SneakyThrows
  public static String asJsonString(Object value) {
    return OBJECT_MAPPER.writeValueAsString(value);
  }

  private static void setUpMinio() {
    s3 = new GenericContainer<>("minio/minio:latest").withEnv("MINIO_ACCESS_KEY", S3_ACCESS_KEY)
      .withEnv("MINIO_SECRET_KEY", S3_SECRET_KEY)
      .withCommand("server /data")
      .withExposedPorts(S3_PORT)
      .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
        new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(S3_PORT), new ExposedPort(S3_PORT)))))
      .waitingFor(new HttpWaitStrategy().forPath("/minio/health/ready")
        .forPort(S3_PORT)
        .withStartupTimeout(Duration.ofSeconds(10))
      );
    s3.start();
    minio_endpoint = format("http://%s:%s", s3.getHost(), s3.getFirstMappedPort());
    log.info("minio container {} on {}", s3.isRunning() ? "is running" : "is not running", minio_endpoint);
  }

  private static void setUpClient() {
    client = new RemoteFileSystemClient(S3ClientFactory.getS3Client(S3ClientProperties.builder()
      .endpoint(minio_endpoint)
      .secretKey(S3_SECRET_KEY)
      .accessKey(S3_ACCESS_KEY)
      .bucket(BUCKET)
      .awsSdk(false)
      .region(REGION)
      .build()));
  }

  public static BulkOperationRuleCollection rules(BulkOperationRule... rules) {
    var uuid = UUID.randomUUID();

    return new BulkOperationRuleCollection()
      .bulkOperationRules(Arrays.stream(rules).map(rule -> rule.bulkOperationId(uuid)).collect(Collectors.toList()))
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
