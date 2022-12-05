package org.folio.bulkops.repository;

import org.folio.bulkops.config.properties.RemoteFileSystemProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import java.time.Duration;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RemoteFileSystemRepositoryTest {

  private static final String S3_ACCESS_KEY = "minio-access-key";
  private static final String S3_SECRET_KEY = "minio-secret-key";
  private static final String S3_BUCKET = "test-bucket";
  private static final String S3_REGION = "us-west-2";
  private static final int S3_PORT = 9000;
  private static RemoteFileSystemRepository repository;

  @BeforeAll
  static void setUp() {
    var s3 = new GenericContainer<>("minio/minio:latest").withEnv("MINIO_ACCESS_KEY", S3_ACCESS_KEY)
      .withEnv("MINIO_SECRET_KEY", S3_SECRET_KEY)
      .withCommand("server /data")
      .withExposedPorts(S3_PORT)
      .waitingFor(new HttpWaitStrategy().forPath("/minio/health/ready")
        .forPort(S3_PORT)
        .withStartupTimeout(Duration.ofSeconds(10)));
    s3.start();
    var endpoint = format("http://%s:%s", s3.getHost(), s3.getFirstMappedPort());
    repository = new RemoteFileSystemRepository(RemoteFileSystemProperties.builder()
      .endpoint(endpoint)
      .secretKey(S3_SECRET_KEY)
      .accessKey(S3_ACCESS_KEY)
      .bucket(S3_BUCKET)
      .composeWithAwsSdk(false)
      .region(S3_REGION)
      .build());
  }

  @Test
  void shouldRetrieveInitialContentAfterGetAndUpdateAfterPut() {
    repository.put("src/test/resources/repository/initial.txt", "initial.txt");
    var content = repository.get("initial.txt");
    assertEquals("initial content", content.trim());
    var uploaded = repository.put("src/test/resources/repository/updated.txt", "initial.txt");
    assertEquals("initial.txt", uploaded);
    content = repository.get("initial.txt");
    assertEquals("updated content", content.trim());
  }

  @Test
  void shouldNotRetrieveContentIfFileNameNotFound() {
    var content = repository.get("initial_wrong.txt");
    assertNull(content);
  }
}
