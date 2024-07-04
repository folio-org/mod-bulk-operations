package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.domain.entity.BulkOperationRuleDetails;
import org.folio.bulkops.repository.BulkOperationMarcRuleRepository;
import org.folio.bulkops.repository.BulkOperationRuleDetailsRepository;
import org.folio.bulkops.repository.BulkOperationRuleRepository;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class RuleServiceTest extends BaseTest {
  private final static UUID BULK_OPERATION_ID = UUID.randomUUID();
  private final static String LOCATION_ID = UUID.randomUUID().toString();
  @Autowired
  private RuleService ruleService;
  @MockBean
  private BulkOperationRuleRepository ruleRepository;
  @MockBean
  private BulkOperationMarcRuleRepository marcRuleRepository;
  @MockBean
  private BulkOperationRuleDetailsRepository ruleDetailsRepository;

  @Test
  void shouldSaveRules() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      when(ruleRepository.save(any(org.folio.bulkops.domain.entity.BulkOperationRule.class)))
        .thenReturn(org.folio.bulkops.domain.entity.BulkOperationRule.builder().id(UUID.randomUUID()).build());

      ruleService.saveRules(rules());

      verify(ruleRepository).deleteAllByBulkOperationId(BULK_OPERATION_ID);
      verify(ruleRepository).save(any(org.folio.bulkops.domain.entity.BulkOperationRule.class));
      verify(ruleDetailsRepository).save(any(BulkOperationRuleDetails.class));
    }
  }

  @Test
  void shouldGetRules() {
    when(ruleRepository.findAllByBulkOperationId(BULK_OPERATION_ID))
      .thenReturn(List.of(org.folio.bulkops.domain.entity.BulkOperationRule.builder()
        .bulkOperationId(BULK_OPERATION_ID)
        .updateOption(UpdateOptionType.PERMANENT_LOCATION)
        .ruleDetails(List.of(BulkOperationRuleDetails.builder()
          .updateAction(UpdateActionType.REPLACE_WITH)
          .updatedValue(LOCATION_ID)
          .build()))
        .build()));

    var fetchedRules = ruleService.getRules(BULK_OPERATION_ID);

    assertEquals(rules(), fetchedRules);
  }

  @Test
  void shouldSaveMarcRules() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      when(marcRuleRepository.save(any(org.folio.bulkops.domain.entity.BulkOperationMarcRule.class)))
        .thenReturn(org.folio.bulkops.domain.entity.BulkOperationMarcRule.builder().id(UUID.randomUUID()).build());

      ruleService.saveMarcRules(marcRules());

      verify(marcRuleRepository).deleteAllByBulkOperationId(BULK_OPERATION_ID);
      verify(marcRuleRepository).save(any(org.folio.bulkops.domain.entity.BulkOperationMarcRule.class));
    }
  }

  @Test
  void shouldGetMarcRules() {
    when(marcRuleRepository.findAllByBulkOperationId(BULK_OPERATION_ID))
      .thenReturn(List.of(org.folio.bulkops.domain.entity.BulkOperationMarcRule.builder()
        .bulkOperationId(BULK_OPERATION_ID)
        .tag("100")
        .ind1(EMPTY)
        .ind2(EMPTY)
        .actions(Collections.singletonList(new org.folio.bulkops.domain.dto.MarcAction()
          .name(UpdateActionType.FIND)
          .data(Collections.singletonList(new org.folio.bulkops.domain.dto.MarcActionDataInner()
            .key(org.folio.bulkops.domain.dto.MarcDataType.VALUE)
            .value("text")))))
        .build()));

    var fetchedRules = ruleService.getMarcRules(BULK_OPERATION_ID);

    assertEquals(marcRules(), fetchedRules);
  }

  private BulkOperationRuleCollection rules() {
    return new BulkOperationRuleCollection()
      .bulkOperationRules(List.of(new BulkOperationRule()
        .bulkOperationId(BULK_OPERATION_ID)
        .ruleDetails(new BulkOperationRuleRuleDetails()
          .option(UpdateOptionType.PERMANENT_LOCATION)
          .actions(List.of(new Action()
            .type(UpdateActionType.REPLACE_WITH)
            .updated(LOCATION_ID))))))
      .totalRecords(1);
  }

  private BulkOperationMarcRuleCollection marcRules() {
    return new BulkOperationMarcRuleCollection()
      .bulkOperationMarcRules(Collections.singletonList(new BulkOperationMarcRule()
        .bulkOperationId(BULK_OPERATION_ID)
        .tag("100")
        .ind1(EMPTY)
        .ind2(EMPTY)
        .actions(Collections.singletonList(new org.folio.bulkops.domain.dto.MarcAction()
          .name(UpdateActionType.FIND)
          .data(Collections.singletonList(new org.folio.bulkops.domain.dto.MarcActionDataInner()
            .key(org.folio.bulkops.domain.dto.MarcDataType.VALUE)
            .value("text")))))))
      .totalRecords(1);
  }
}
