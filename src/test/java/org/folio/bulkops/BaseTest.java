package org.folio.bulkops;

import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.SneakyThrows;
import org.folio.bulkops.config.properties.RemoteFileSystemProperties;
import org.folio.bulkops.repository.RemoteFileSystemRepository;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = BaseTest.DockerPostgreDataSourceInitializer.class)
@Testcontainers
@AutoConfigureMockMvc
public abstract class BaseTest {
  public static PostgreSQLContainer<?> postgresDBContainer = new PostgreSQLContainer<>("postgres:13");
  public static WireMockServer wireMockServer;
  public static final int WIRE_MOCK_PORT = SocketUtils.findAvailableTcpPort();
  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

  public static RemoteFileSystemRepository repository;

  protected static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidXNlcl9pZCI6IjFkM2I1OGNiLTA3YjUtNWZjZC04YTJhLTNjZTA2YTBlYjkwZiIsImlhdCI6MTYxNjQyMDM5MywidGVuYW50IjoiZGlrdSJ9.2nvEYQBbJP1PewEgxixBWLHSX_eELiBEBpjufWiJZRs";
  protected static final String TENANT = "diku";

  private static final String S3_ACCESS_KEY = "minio-access-key";
  private static final String S3_SECRET_KEY = "minio-secret-key";
  private static final String S3_BUCKET = "test-bucket";
  private static final String S3_REGION = "us-west-2";
  private static final int S3_PORT = 9000;

  private static String minio_endpoint;

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  private FolioModuleMetadata folioModuleMetadata;

  static {
    postgresDBContainer.start();
  }

  public static class DockerPostgreDataSourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
        "spring.datasource.url=" + postgresDBContainer.getJdbcUrl(),
        "spring.datasource.username=" + postgresDBContainer.getUsername(),
        "spring.datasource.password=" + postgresDBContainer.getPassword());
    }
  }

  @BeforeAll
  static void beforeAll(@Autowired MockMvc mockMvc) {
    wireMockServer = new WireMockServer(WIRE_MOCK_PORT);
    wireMockServer.start();

    setUpTenant(mockMvc);
    setUpMinio();
    setUpRemoteRepository();
  }

  private static void setUpMinio() {
    var s3 = new GenericContainer<>("minio/minio:latest").withEnv("MINIO_ACCESS_KEY", S3_ACCESS_KEY)
      .withEnv("MINIO_SECRET_KEY", S3_SECRET_KEY)
      .withCommand("server /data")
      .withExposedPorts(S3_PORT)
      .waitingFor(new HttpWaitStrategy().forPath("/minio/health/ready")
        .forPort(S3_PORT)
        .withStartupTimeout(Duration.ofSeconds(10)));
    s3.start();
    minio_endpoint = format("http://%s:%s", s3.getHost(), s3.getFirstMappedPort());
  }

  private static void setUpRemoteRepository() {
    repository = new RemoteFileSystemRepository(RemoteFileSystemProperties.builder()
      .endpoint(minio_endpoint)
      .secretKey(S3_SECRET_KEY)
      .accessKey(S3_ACCESS_KEY)
      .bucket(S3_BUCKET)
      .composeWithAwsSdk(false)
      .region(S3_REGION)
      .build());
  }

  @BeforeEach
  void setUp() {
    Map<String, Collection<String>> okapiHeaders = new LinkedHashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of(TENANT));
    okapiHeaders.put(XOkapiHeaders.TOKEN, List.of(TOKEN));
    okapiHeaders.put(XOkapiHeaders.URL, List.of(wireMockServer.baseUrl()));
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
    mockMvc.perform(post("/_/tenant").content(asJsonString(new TenantAttributes().moduleTo("mod-data-export-worker")))
      .headers(defaultHeaders())
      .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());
  }

  public static HttpHeaders defaultHeaders() {
    final HttpHeaders httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.put(XOkapiHeaders.TENANT, List.of(TENANT));
    httpHeaders.add(XOkapiHeaders.URL, wireMockServer.baseUrl());
    httpHeaders.add(XOkapiHeaders.TOKEN, TOKEN);
    httpHeaders.add(XOkapiHeaders.USER_ID, UUID.randomUUID().toString());

    return httpHeaders;
  }

  @SneakyThrows
  public static String asJsonString(Object value) {
    return OBJECT_MAPPER.writeValueAsString(value);
  }
}