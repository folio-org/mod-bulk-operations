package org.folio.bulkops.client;

import lombok.RequiredArgsConstructor;
import org.folio.s3.client.FolioS3Client;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Lazy
@Component
@RequiredArgsConstructor
public class LocalFileSystemClient {

  public final FolioS3Client localFolioS3Client;

  public String put(InputStream newFile, String fileNameToBeUpdated) {
    return localFolioS3Client.write(fileNameToBeUpdated, newFile);
  }

  public InputStream get(String fileName) {
    return localFolioS3Client.read(fileName);
  }
}
