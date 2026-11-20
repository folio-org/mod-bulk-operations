package org.folio.bulkops.configs.kafka;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.folio.bulkops.BaseTest;
import org.folio.spring.kafka.filtering.filter.EnabledTenantMessageFilterStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.test.context.TestPropertySource;

/**
 * Starts the whole context with the Kafka tenant filter enabled. In this mode folio-spring-kafka
 * registers additional beans (e.g. an extra ConsumerFactory for entitlement events and the
 * entitlement HTTP client), so the enabled path must be covered by a context start-up test.
 */
@TestPropertySource(properties = "folio.kafka.tenant-filter.enabled=true")
class KafkaTenantFilterEnabledTest extends BaseTest {

  @Autowired
  @Qualifier("tenantAwareMessageFilter")
  private RecordFilterStrategy<?, ?> tenantAwareMessageFilter;

  @Autowired
  @Qualifier("kafkaListenerContainerFactoryDi")
  private KafkaListenerContainerFactory<?> kafkaListenerContainerFactoryDi;

  @Test
  void contextStartsWithEnabledTenantFilter() {
    assertInstanceOf(EnabledTenantMessageFilterStrategy.class, tenantAwareMessageFilter);
  }

  @Test
  void diListenerContainerFactoryIsAvailableWithEnabledTenantFilter() {
    assertNotNull(kafkaListenerContainerFactoryDi);
  }
}
