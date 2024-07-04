package org.folio.bulkops.client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;

import org.folio.bulkops.service.MarcRemoteStorageWriter;
import org.folio.s3.client.FolioS3Client;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RemoteFileSystemClient {

  private static final int DEFAULT_CHAR_BUFFER_SIZE = 16384;

  public final FolioS3Client remoteFolioS3Client;

  public String put(InputStream newFile, String fileNameToBeUpdated) {
    return remoteFolioS3Client.write(fileNameToBeUpdated, newFile);
  }

  public int getNumOfLines(String file) {
    return (int) new BufferedReader(new InputStreamReader(get(file))).lines()
      .count();
  }

  public InputStream get(String fileName) {
    return remoteFolioS3Client.read(fileName);
  }

  public void remove(String filename) {
    remoteFolioS3Client.remove(filename);
  }

  public void remove(String... paths) {
    remoteFolioS3Client.remove(paths);
  }

  public Writer writer(String path) {
    return remoteFolioS3Client.getRemoteStorageWriter(path, DEFAULT_CHAR_BUFFER_SIZE);
  }

  public MarcRemoteStorageWriter marcWriter(String path) {
    return new MarcRemoteStorageWriter(path, DEFAULT_CHAR_BUFFER_SIZE, remoteFolioS3Client);
  }
}
