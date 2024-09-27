package org.folio.bulkops.configs;

import feign.codec.Encoder;
import feign.form.FormEncoder;
import org.springframework.context.annotation.Bean;

public class DataImportFeignConfig {
  @Bean
  public Encoder feignEncoder() {
    return new FormEncoder();
  }
}
