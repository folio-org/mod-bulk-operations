package org.folio.bulkops.client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;

import org.folio.s3.client.FolioS3Client;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Lazy
@Component
@RequiredArgsConstructor
public class RemoteFileSystemClient {

  public final FolioS3Client remoteFolioS3Client;

  private final static int defaultCharBufferSize = 8192;

  public String put(InputStream newFile, String fileNameToBeUpdated) {
    return remoteFolioS3Client.write(fileNameToBeUpdated, newFile);
  }

  public String upload(String path, String filename) {
    return remoteFolioS3Client.upload(path, filename);
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

  public Writer writer(String path) {
    return remoteFolioS3Client.getRemoteStorageWriter(path, defaultCharBufferSize);
  }

}
