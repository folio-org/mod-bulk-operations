package org.folio.bulkops.configs;

import java.util.UUID;

import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.domain.entity.Profile;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.cql.JpaCqlRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CqlRepositoryConfig {
  private final EntityManager entityManager;
  @Bean
  public JpaCqlRepository<BulkOperationExecutionContent, UUID> executionContentCqlRepository() {
    return new JpaCqlRepositoryImpl<>(BulkOperationExecutionContent.class, entityManager);
  }

  @Bean
  public JpaCqlRepository<BulkOperation, UUID> bulkOperationCqlRepository() {
    return new JpaCqlRepositoryImpl<>(BulkOperation.class, entityManager);
  }

  @Bean
  public JpaCqlRepository<Profile, UUID> profileUUIDJpaCqlRepository() {
    return new JpaCqlRepositoryImpl<>(Profile.class, entityManager);
  }
}
