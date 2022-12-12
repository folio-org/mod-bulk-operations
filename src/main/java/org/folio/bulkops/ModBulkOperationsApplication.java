package org.folio.bulkops;

import org.folio.bulkops.client.DataExportClient;
import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.config.RepositoryConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(clients = {
  DataExportClient.class,
  HoldingsClient.class,
  ItemClient.class,
  UserClient.class
})
@EnableConfigurationProperties(RepositoryConfig.class)
public class ModBulkOperationsApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModBulkOperationsApplication.class, args);
  }
}
