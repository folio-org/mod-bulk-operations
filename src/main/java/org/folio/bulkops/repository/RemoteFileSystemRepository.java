package org.folio.bulkops.repository;

import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.config.properties.RemoteFileSystemProperties;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.client.S3ClientFactory;
import org.folio.s3.client.S3ClientProperties;

@Log4j2
public class RemoteFileSystemRepository {

  private final FolioS3Client folioS3Client;

  public RemoteFileSystemRepository(RemoteFileSystemProperties properties) {
    folioS3Client = buildClient(properties);
  }

  private FolioS3Client buildClient(RemoteFileSystemProperties properties) {
    return S3ClientFactory.getS3Client(S3ClientProperties.builder()
      .endpoint(properties.getEndpoint())
      .secretKey(properties.getSecretKey())
      .accessKey(properties.getAccessKey())
      .bucket(properties.getBucket())
      .awsSdk(properties.isComposeWithAwsSdk())
      .region(properties.getRegion())
      .build());
  }

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
