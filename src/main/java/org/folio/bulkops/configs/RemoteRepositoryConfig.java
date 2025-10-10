package org.folio.bulkops.configs;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.client.S3ClientFactory;
import org.folio.s3.client.S3ClientProperties;
import org.folio.s3.exception.S3ClientException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Log4j2
@ConfigurationProperties("application.remote-files-storage")
@Data
@ConfigurationPropertiesScan
public class RemoteRepositoryConfig {

  private String endpoint;
  private String region;
  private String bucket;
  private String accessKey;
  private String secretKey;
  private boolean awsSdk;

  @Bean
  public FolioS3Client remoteFolioS3Client() {
    log.debug("remote-files-storage: endpoint {}, region {}, bucket {}, accessKey {}, "
            + "secretKey {}, awsSdk {}", endpoint, region, bucket, accessKey, secretKey, awsSdk);
    var client = S3ClientFactory.getS3Client(S3ClientProperties.builder()
            .endpoint(endpoint)
            .secretKey(secretKey)
            .accessKey(accessKey)
            .bucket(bucket)
            .awsSdk(awsSdk)
            .region(region)
            .build());
    try {
      client.createBucketIfNotExists();
    } catch (S3ClientException e) {
      log.error("Error creating bucket: {} during RemoteStorageClient initialization", bucket);
    }
    return client;
  }
}
