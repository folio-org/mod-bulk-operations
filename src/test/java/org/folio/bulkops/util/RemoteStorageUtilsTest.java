package org.folio.bulkops.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RemoteStorageUtilsTest {

  @Test
  void downloadToInputStream_returnsStreamWithRemoteContent() throws IOException {
    var payload = "{\"id\":\"1\"}\n{\"id\":\"2\"}\n";
    var client = mock(RemoteFileSystemClient.class);
    when(client.get(anyString()))
        .thenReturn(new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8)));

    try (InputStream in = RemoteStorageUtils.downloadToInputStream(client, "remote/path.json")) {
      var read = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(read).isEqualTo(payload);
    }
  }

  @Test
  void downloadToInputStream_deletesTempFileWhenStreamIsClosed() throws Exception {
    var client = mock(RemoteFileSystemClient.class);
    when(client.get(anyString()))
        .thenReturn(new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)));

    InputStream in = RemoteStorageUtils.downloadToInputStream(client, "remote/path.json");
    Path backingPath = extractBackingPath(in);

    assertThat(backingPath).exists();
    in.close();
    assertThat(backingPath).doesNotExist();
  }

  @Test
  void downloadToInputStream_deletesTempFileWhenDownloadFails() throws Exception {
    var client = mock(RemoteFileSystemClient.class);
    when(client.get(anyString())).thenReturn(new FailingInputStream());

    Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
    long beforeMatching = countBulkOpsTempFiles(tempDir);

    assertThatThrownBy(() -> RemoteStorageUtils.downloadToInputStream(client, "remote/path.json"))
        .isInstanceOf(IOException.class);

    long afterMatching = countBulkOpsTempFiles(tempDir);
    // No leaked temp file from this invocation.
    assertThat(afterMatching).isLessThanOrEqualTo(beforeMatching);
  }

  @Test
  void downloadToInputStream_supportsEmptyRemoteContent() throws IOException {
    var client = mock(RemoteFileSystemClient.class);
    when(client.get(anyString())).thenReturn(new ByteArrayInputStream(new byte[0]));

    try (InputStream in = RemoteStorageUtils.downloadToInputStream(client, "remote/empty.json")) {
      assertThat(in.readAllBytes()).isEmpty();
    }
  }

  @ParameterizedTest
  @CsvSource({
    "remote/file.json,            .json",
    "remote/file.csv,             .csv",
    "remote/file.mrc,             .mrc",
    "remote/path/data.tar.gz,     .gz",
    "no-extension,                .tmp",
    "trailing-dot.,               .tmp",
    "'',                          .tmp"
  })
  void downloadToInputStream_preservesExtensionFromRemotePath(
      String remotePath, String expectedSuffix) throws Exception {
    var client = mock(RemoteFileSystemClient.class);
    when(client.get(anyString()))
        .thenReturn(new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));

    try (InputStream in = RemoteStorageUtils.downloadToInputStream(client, remotePath)) {
      Path backingPath = extractBackingPath(in);
      assertThat(backingPath.getFileName().toString())
          .startsWith("bulk-ops-")
          .endsWith(expectedSuffix);
    }
  }

  @Test
  void downloadToInputStream_usesDefaultSuffixForNullRemotePath() throws Exception {
    var client = mock(RemoteFileSystemClient.class);
    when(client.get(any()))
        .thenReturn(new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));

    try (InputStream in = RemoteStorageUtils.downloadToInputStream(client, null)) {
      Path backingPath = extractBackingPath(in);
      assertThat(backingPath.getFileName().toString()).endsWith(".tmp");
    }
  }

  private static Path extractBackingPath(InputStream in) throws ReflectiveOperationException {
    Field field = in.getClass().getDeclaredField("path");
    field.setAccessible(true);
    return (Path) field.get(in);
  }

  private static long countBulkOpsTempFiles(Path dir) throws IOException {
    try (var stream = Files.list(dir)) {
      return stream.filter(p -> p.getFileName().toString().startsWith("bulk-ops-")).count();
    }
  }

  private static final class FailingInputStream extends InputStream {
    @Override
    public int read() throws IOException {
      throw new IOException("simulated network failure");
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      throw new IOException("simulated network failure");
    }
  }
}
