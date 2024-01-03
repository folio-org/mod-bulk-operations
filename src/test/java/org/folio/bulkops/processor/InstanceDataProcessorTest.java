package org.folio.bulkops.processor;

import static java.util.Objects.isNull;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.service.ErrorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

class InstanceDataProcessorTest extends BaseTest {
  @Autowired
  DataProcessorFactory factory;
  @MockBean
  ErrorService errorService;

  private DataProcessor<Instance> processor;

  public static final String IDENTIFIER = "123";

  @BeforeEach
  void setUp() {
    if (isNull(processor)) {
      processor = factory.getProcessorFromFactory(Instance.class);
    }
  }

  @Test
  void testSetDiscoverySuppressToTrue() {
    var actual = processor.process(IDENTIFIER, new Instance(), rules(rule(SUPPRESS_FROM_DISCOVERY, SET_TO_TRUE, null)));
    assertNotNull(actual.getUpdated());
    assertTrue(actual.getUpdated().getDiscoverySuppress());
  }

  @Test
  void testSetDiscoverySuppressToFalse() {
    var actual = processor.process(IDENTIFIER, new Instance(), rules(rule(SUPPRESS_FROM_DISCOVERY, SET_TO_FALSE, null)));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.getUpdated().getDiscoverySuppress());
  }

  @Test
  void shouldNotUpdateInstanceWhenActionIsInvalid() {
    var actual = processor.process(IDENTIFIER, new Instance().withDiscoverySuppress(true), rules(rule(SUPPRESS_FROM_DISCOVERY, CLEAR_FIELD, null)));
    assertTrue(actual.getUpdated().getDiscoverySuppress());
    verify(errorService).saveError(any(UUID.class), eq(IDENTIFIER), anyString());
  }

  @Test
  void testClone() {
    var processor = new InstanceDataProcessor();
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .title("Title")
      .discoverySuppress(false)
      .administrativeNotes(List.of("Note1", "Note2"))
      .build();

    var cloned = processor.clone(instance);
    assertTrue(processor.compare(instance, cloned));

    cloned.setAdministrativeNotes(Collections.singletonList("Note3"));
    assertFalse(processor.compare(instance, cloned));
  }
}
