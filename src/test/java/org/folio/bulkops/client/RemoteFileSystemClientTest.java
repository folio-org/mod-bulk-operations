package org.folio.bulkops.client;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.config.RepositoryConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Log4j2
class RemoteFileSystemClientTest extends BaseTest {

  private static final String INITIAL_FILE = "initial.txt";
  private static final String WRONG_FILE = "wrong.txt";

  @Autowired
  private RemoteFileSystemClient remoteFileSystemClient;

  @Autowired
  private RepositoryConfig repositoryConfig;

  @SneakyThrows
  @Test
  void shouldRetrieveInitialContentAfterGetAndUpdateAfterPut() {
    client.put(IOUtils.toInputStream("initial content", StandardCharsets.UTF_8), INITIAL_FILE);
    var content = client.get(INITIAL_FILE);
    assertEquals("initial content", IOUtils.toString(content, StandardCharsets.UTF_8).trim());
    var uploaded = client.put(IOUtils.toInputStream("updated content", StandardCharsets.UTF_8), INITIAL_FILE);
    assertEquals(INITIAL_FILE, uploaded);
    content = client.get(INITIAL_FILE);
    assertEquals("updated content", IOUtils.toString(content, StandardCharsets.UTF_8).trim());
  }

  @Test
  void shouldThrowExceptionIfFileNameNotFound() {
    assertThrows(Exception.class, () -> client.get(WRONG_FILE));
  }

  @Test
  void shouldInitializeFolioS3Client() {
    assertNotNull(remoteFileSystemClient);
    assertNotNull(repositoryConfig);
    assertEquals(BUCKET, repositoryConfig.getBucket());
    assertEquals(REGION, repositoryConfig.getRegion());
    assertEquals(S3_ACCESS_KEY, repositoryConfig.getAccessKey());
    assertEquals(S3_SECRET_KEY, repositoryConfig.getSecretKey());

    // Check only port cause host could be different depending on environment.
    assertTrue(repositoryConfig.getEndpoint().contains(String.valueOf(S3_PORT)));
  }
}
