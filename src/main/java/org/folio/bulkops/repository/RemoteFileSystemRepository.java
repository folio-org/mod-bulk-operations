package org.folio.bulkops.repository;

import lombok.RequiredArgsConstructor;
import org.folio.s3.client.FolioS3Client;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;

@Lazy
@Repository
@RequiredArgsConstructor
public class RemoteFileSystemRepository {

  public final FolioS3Client folioS3Client;

  public String put(InputStream newFile, String fileNameToBeUpdated) {
    return folioS3Client.write(fileNameToBeUpdated, newFile);
  }

  public InputStream get(String fileName) throws IOException {
    return folioS3Client.read(fileName);
  }
}
