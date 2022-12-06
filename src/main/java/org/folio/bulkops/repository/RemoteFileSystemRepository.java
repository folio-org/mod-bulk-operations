package org.folio.bulkops.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.s3.client.FolioS3Client;
import org.springframework.stereotype.Repository;

@Log4j2
@Repository
@RequiredArgsConstructor
public class RemoteFileSystemRepository {

  public final FolioS3Client folioS3Client;

  public String put(String pathToNewFile, String fileNameToBeUpdated) {
    return folioS3Client.upload(pathToNewFile, fileNameToBeUpdated);
  }

  public String get(String fileName) {
    try {
      return new String(folioS3Client.read(fileName).readAllBytes());
    } catch (Exception exc) {
      log.error("Failed to read from FolioS3Client: {}", exc.getMessage());
      return null;
    }
  }
}
