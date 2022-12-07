package org.folio.bulkops.repository;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.folio.bulkops.config.RepositoryConfig;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.client.S3ClientFactory;
import org.folio.s3.client.S3ClientProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import java.io.FileInputStream;
import java.time.Duration;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest(classes = RemoteFileSystemRepository.class)
@ContextConfiguration(classes = {RepositoryConfig.class})
class RemoteFileSystemRepositoryTest {

  private static final String S3_ACCESS_KEY = "minio-access-key";
  private static final String S3_SECRET_KEY = "minio-secret-key";
  private static final int S3_PORT = 9000;
  private static final String BUCKET = "test-bucket";
  private static final String REGION = "us-west-2";
  private static String minio_endpoint;

  private static final String INITIAL_FILE = "initial.txt";
  private static final String WRONG_FILE = "wrong.txt";
  private static final String INITIAL_FILE_PATH = "src/test/resources/repository/" + INITIAL_FILE;
  private static final String UPDATED_FILE_PATH = "src/test/resources/repository/updated.txt";

  private static RemoteFileSystemRepository remoteFileSystemRepository;

  @BeforeAll
  static void setUp() {
    setUpMinio();
    setUpRepository();
  }

  private static void setUpMinio() {
    var s3 = new GenericContainer<>("minio/minio:latest").withEnv("MINIO_ACCESS_KEY", S3_ACCESS_KEY)
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

  private static void setUpRepository() {
    FolioS3Client folioS3Client = S3ClientFactory.getS3Client(S3ClientProperties.builder()
      .endpoint(minio_endpoint)
      .secretKey(S3_SECRET_KEY)
      .accessKey(S3_ACCESS_KEY)
      .bucket(BUCKET)
      .awsSdk(false)
      .region(REGION)
      .build());
    remoteFileSystemRepository = new RemoteFileSystemRepository(folioS3Client);
  }

  @SneakyThrows
  @Test
  void shouldRetrieveInitialContentAfterGetAndUpdateAfterPut() {
    remoteFileSystemRepository.put(new FileInputStream(INITIAL_FILE_PATH), INITIAL_FILE);
    var content = remoteFileSystemRepository.get(INITIAL_FILE);
    assertEquals("initial content", IOUtils.toString(content, "UTF-8").trim());
    var uploaded = remoteFileSystemRepository.put(new FileInputStream(UPDATED_FILE_PATH), INITIAL_FILE);
    assertEquals(INITIAL_FILE, uploaded);
    content = remoteFileSystemRepository.get(INITIAL_FILE);
    assertEquals("updated content", IOUtils.toString(content, "UTF-8").trim());
  }

  @Test
  void shouldNotRetrieveContentIfFileNameNotFound() {
    assertThrows(Exception.class, () -> remoteFileSystemRepository.get(WRONG_FILE));
  }
}
