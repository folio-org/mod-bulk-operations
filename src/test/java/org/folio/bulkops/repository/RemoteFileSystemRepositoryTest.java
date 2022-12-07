package org.folio.bulkops.repository;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.folio.bulkops.config.RepositoryConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import java.time.Duration;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RemoteFileSystemRepository.class)
@ContextConfiguration(classes = {RepositoryConfig.class})
class RemoteFileSystemRepositoryTest {

  private static final String S3_ACCESS_KEY = "minio-access-key";
  private static final String S3_SECRET_KEY = "minio-secret-key";
  private static final int S3_PORT = 9000;
  private static String minio_endpoint;

  private static final String INITIAL_FILE = "initial.txt";
  private static final String WRONG_FILE = "wrong.txt";
  private static final String INITIAL_FILE_PATH = "src/test/resources/repository/" + INITIAL_FILE;
  private static final String UPDATED_FILE_PATH = "src/test/resources/repository/updated.txt";

  @Autowired
  private RemoteFileSystemRepository remoteFileSystemRepository;

  @Autowired
  private RepositoryConfig repositoryConfig;

  @BeforeAll
  static void setUp() {
    setUpMinio();
  }

  @BeforeEach
  void setMinioEndpoint() {
    repositoryConfig.setEndpoint(minio_endpoint);
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
  }

  @Test
  void shouldRetrieveInitialContentAfterGetAndUpdateAfterPut() {
    try {
      remoteFileSystemRepository.put(INITIAL_FILE_PATH, INITIAL_FILE);
      var content = remoteFileSystemRepository.get(INITIAL_FILE);
      assertEquals("initial content", content.trim());
      var uploaded = remoteFileSystemRepository.put(UPDATED_FILE_PATH, INITIAL_FILE);
      assertEquals(INITIAL_FILE, uploaded);
      content = remoteFileSystemRepository.get(INITIAL_FILE);
      assertEquals("updated content", content.trim());
    } catch (Exception exc) {
      fail(exc);
    }
  }

  @Test
  void shouldNotRetrieveContentIfFileNameNotFound() {
    assertThrows(Exception.class, () -> remoteFileSystemRepository.get(WRONG_FILE));
  }
}
