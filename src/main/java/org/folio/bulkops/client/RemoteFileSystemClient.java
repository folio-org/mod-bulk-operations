package org.folio.bulkops.client;

import java.io.InputStream;

import org.folio.s3.client.FolioS3Client;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

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

  public InputStream get(String fileName) {
    return remoteFolioS3Client.read(fileName);
  }
}
