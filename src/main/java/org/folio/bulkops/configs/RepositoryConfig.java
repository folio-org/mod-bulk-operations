package org.folio.bulkops.configs;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.client.S3ClientFactory;
import org.folio.s3.client.S3ClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Lazy
@Configuration
@Log4j2
@ConfigurationProperties("application.remote-files-storage")
@Data
@ConfigurationPropertiesScan
public class RepositoryConfig {

  private String endpoint;
  private String region;
  private String bucket;
  private String accessKey;
  private String secretKey;
  private boolean awsSdk;

  @Bean
  public FolioS3Client folioS3Client() {
    return buildFolioS3Client();
  }

  private FolioS3Client buildFolioS3Client() {
    log.debug("remote-files-storage: endpoint {}, region {}, bucket {}, accessKey {}, secretKey {}, awsSdk {}",
      endpoint, region, bucket, accessKey, secretKey, awsSdk);
    return S3ClientFactory.getS3Client(S3ClientProperties.builder()
      .endpoint(endpoint)
      .secretKey(secretKey)
      .accessKey(accessKey)
      .bucket(bucket)
      .awsSdk(awsSdk)
      .region(region)
      .build());
  }
}
