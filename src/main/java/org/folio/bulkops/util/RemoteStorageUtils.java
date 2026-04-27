package org.folio.bulkops.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.RemoteFileSystemClient;

/**
 * Utilities for working with remote object storage files.
 *
 * <p>The primary purpose is to avoid holding long-lived TLS streams open against object storage
 * while iterating over records. A long-lived GET is sensitive to idle/duration timeouts of any
 * intermediate proxy or load balancer, which surfaces as {@code Connection reset} during JSON
 * deserialization.
 */
@UtilityClass
@Log4j2
public class RemoteStorageUtils {

  private static final String TEMP_FILE_PREFIX = "bulk-ops-";
  private static final String DEFAULT_TEMP_FILE_SUFFIX = ".tmp";

  /**
   * Downloads a remote object to a local temp file and returns an {@link InputStream} that reads
   * from it. The temp file is deleted when the returned stream is closed (and on JVM exit as a
   * safety net). The temp file's extension is derived from {@code remotePath} so that the local
   * copy preserves the original format (e.g. {@code .json}, {@code .csv}, {@code .mrc}).
   *
   * @param client the remote file system client used to fetch the object
   * @param remotePath the remote path/key of the object to download
   * @return an {@link InputStream} backed by a local temp file; closing it deletes the temp file
   * @throws IOException if the file cannot be downloaded or written locally
   */
  public static InputStream downloadToInputStream(RemoteFileSystemClient client, String remotePath)
      throws IOException {
    var tempFile = Files.createTempFile(TEMP_FILE_PREFIX, resolveTempFileSuffix(remotePath));
    tempFile.toFile().deleteOnExit();
    try (var remote = client.get(remotePath)) {
      Files.copy(remote, tempFile, StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception e) {
      deleteQuietly(tempFile);
      throw e;
    }
    log.info(
        "Downloaded remote file {} to local temp file {} ({} bytes)",
        remotePath,
        tempFile,
        Files.size(tempFile));
    return new SelfDeletingFileInputStream(tempFile);
  }

  private static String resolveTempFileSuffix(String remotePath) {
    if (remotePath == null) {
      return DEFAULT_TEMP_FILE_SUFFIX;
    }
    var slash = Math.max(remotePath.lastIndexOf('/'), remotePath.lastIndexOf('\\'));
    var fileName = slash >= 0 ? remotePath.substring(slash + 1) : remotePath;
    var dot = fileName.lastIndexOf('.');
    if (dot <= 0 || dot == fileName.length() - 1) {
      return DEFAULT_TEMP_FILE_SUFFIX;
    }
    return fileName.substring(dot);
  }

  private static void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException ex) {
      log.warn("Failed to delete temp file {}: {}", path, ex.getMessage());
    }
  }

  /** {@link FileInputStream} that deletes its backing file on close. */
  private static final class SelfDeletingFileInputStream extends FileInputStream {
    private final Path path;

    SelfDeletingFileInputStream(Path path) throws IOException {
      super(path.toFile());
      this.path = path;
    }

    @Override
    public void close() throws IOException {
      try {
        super.close();
      } finally {
        deleteQuietly(path);
      }
    }
  }
}
