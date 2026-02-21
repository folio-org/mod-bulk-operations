package org.folio.bulkops;

import org.folio.bulkops.configs.RemoteRepositoryConfig;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RemoteRepositoryConfig.class)
@EnableBatchProcessing
public class ModBulkOperationsApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModBulkOperationsApplication.class, args);
  }
}
