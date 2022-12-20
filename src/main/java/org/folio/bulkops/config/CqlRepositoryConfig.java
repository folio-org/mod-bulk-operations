package org.folio.bulkops.config;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.cql.JpaCqlRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManager;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class CqlRepositoryConfig {
  private final EntityManager entityManager;

  @Bean
  public JpaCqlRepository<BulkOperationExecutionContent, UUID> executionContentCqlRepository() {
    return new JpaCqlRepositoryImpl<>(BulkOperationExecutionContent.class, entityManager);
  }
}
