package org.folio.bulkops.repository;

import lombok.RequiredArgsConstructor;
import org.folio.s3.client.FolioS3Client;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RemoteFileSystemRepository {

  public final FolioS3Client folioS3Client;

  public String put(String pathToNewFile, String fileNameToBeUpdated) {
    return folioS3Client.upload(pathToNewFile, fileNameToBeUpdated);
  }

  public String get(String fileName) throws Exception {
    return new String(folioS3Client.read(fileName).readAllBytes());
  }
}
