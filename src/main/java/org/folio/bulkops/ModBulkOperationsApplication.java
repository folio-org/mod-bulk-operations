package org.folio.bulkops;

import org.folio.bulkops.configs.RemoteRepositoryConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableFeignClients
@EnableCaching
@EnableAsync
@EnableConfigurationProperties(RemoteRepositoryConfig.class)
public class ModBulkOperationsApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModBulkOperationsApplication.class, args);
  }
}
