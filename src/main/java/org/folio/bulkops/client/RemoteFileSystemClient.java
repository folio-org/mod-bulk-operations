package org.folio.bulkops.client;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.folio.s3.client.FolioS3Client;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

@Lazy
@Component
@RequiredArgsConstructor
public class RemoteFileSystemClient {

  public final FolioS3Client remoteFolioS3Client;

  public String put(InputStream newFile, String fileNameToBeUpdated) {
    return remoteFolioS3Client.write(fileNameToBeUpdated, newFile);
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

  public OutputStream newOutputStream(String path) {

    final int BUFFER_SIZE = 50000000;

    return new OutputStream() {

      byte[] buffer = new byte[0];

      @Override
      public synchronized void write(int b) {
        buffer = ArrayUtils.add(buffer, (byte) b);
        if (buffer.length >= BUFFER_SIZE) {
          try (var input = new ByteArrayInputStream(buffer)) {
            append(input, path);
          } catch (IOException e) {
            // Just skip writing and clean buffer
          } finally {
            buffer = new byte[0];
          }
        }
      }

      @Override
      public void close() {
        try(var input = new ByteArrayInputStream(buffer)) {
          append(input, path);
        } catch (IOException e) {
          // Just skip writing and clean buffer
        } finally {
          buffer = new byte[0];
        }
      }
    };
  }

  public BufferedWriter writer(String path) {
    return new BufferedWriter(new OutputStreamWriter(newOutputStream(path)));
  }
}
