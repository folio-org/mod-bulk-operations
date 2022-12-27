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

  public final FolioS3Client folioS3Client;

  public String put(InputStream newFile, String fileNameToBeUpdated) {
    return folioS3Client.write(fileNameToBeUpdated, newFile);
  }

  public InputStream get(String fileName) {
    return folioS3Client.read(fileName);
  }
}
