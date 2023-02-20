package org.folio.bulkops.client;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.exception.ServerErrorException;
import org.folio.s3.client.FolioS3Client;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

@Lazy
@Component
@RequiredArgsConstructor
public class RemoteFileSystemClient {

  public final FolioS3Client remoteFolioS3Client;

  public String put(InputStream newFile, String fileNameToBeUpdated) {
    return remoteFolioS3Client.write(fileNameToBeUpdated, newFile);
  }

  public String upload(String path, String filename) {
    return remoteFolioS3Client.upload(path, filename);
  }

  public String append(InputStream content, String fileNameToAppend) {
    return remoteFolioS3Client.append(fileNameToAppend, content);
  }

  public int getNumOfLines(String file) {
    return (int) new BufferedReader(new InputStreamReader(get(file))).lines().count();
  }

  // Code below should be moved to folio-s3-client
  public InputStream get(String fileName) {
    return remoteFolioS3Client.read(fileName);
  }

  public void remove(String filename) {
    remoteFolioS3Client.remove(filename);
  }

  public Writer writer(String path) {
    return new StringWriter() {
      final Path tmp;
      {
        try {
          tmp = Files.createTempFile(FilenameUtils.getName(path), FilenameUtils.getExtension(path));
        } catch (IOException e) {
          throw new ServerErrorException("Files buffer cannot be created due to error: ", e);
        }
      }

      @Override
      public void write(String data) {
        if (StringUtils.isNotEmpty(data)) {
          try {
            FileUtils.write(tmp.toFile(), data, Charset.defaultCharset(), true);
          } catch (IOException e) {
            FileUtils.deleteQuietly(tmp.toFile());
          }
        } else {
          FileUtils.deleteQuietly(tmp.toFile());
        }
      }

      @Override
      public void close() {
        try {
          if (tmp.toFile().exists()) {
            put(FileUtils.openInputStream(tmp.toFile()), path);
          }
        } catch (Exception e) {
          // Just skip and wait file deletion
        } finally {
          FileUtils.deleteQuietly(tmp.toFile());
        }
      }
    };
  }
}
