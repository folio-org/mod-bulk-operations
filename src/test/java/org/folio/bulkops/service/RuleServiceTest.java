package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.RuleDetails;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationRuleDetails;
import org.folio.bulkops.repository.BulkOperationMarcRuleRepository;
import org.folio.bulkops.repository.BulkOperationRuleDetailsRepository;
import org.folio.bulkops.repository.BulkOperationRuleRepository;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class RuleServiceTest extends BaseTest {
  private static final UUID BULK_OPERATION_ID = UUID.randomUUID();
  private static final String LOCATION_ID = java.util.UUID.randomUUID().toString();
  @Autowired private RuleService ruleService;
  @MockitoBean private BulkOperationRuleRepository ruleRepository;
  @MockitoBean private BulkOperationMarcRuleRepository marcRuleRepository;
  @MockitoBean private BulkOperationRuleDetailsRepository ruleDetailsRepository;

  @Test
  void shouldSaveRules() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var operation = BulkOperation.builder().id(BULK_OPERATION_ID).build();
      when(ruleRepository.save(any(org.folio.bulkops.domain.entity.BulkOperationRule.class)))
          .thenReturn(
              org.folio.bulkops.domain.entity.BulkOperationRule.builder()
                  .id(UUID.randomUUID())
                  .build());

      ruleService.saveRules(operation, rules());

      verify(ruleRepository).deleteAllByBulkOperationId(BULK_OPERATION_ID);
      verify(ruleRepository).save(any(org.folio.bulkops.domain.entity.BulkOperationRule.class));
      verify(ruleDetailsRepository).save(any(BulkOperationRuleDetails.class));
    }
  }

  @Test
  void shouldGetRules() {
    when(ruleRepository.findAllByBulkOperationId(BULK_OPERATION_ID))
        .thenReturn(
            List.of(
                org.folio.bulkops.domain.entity.BulkOperationRule.builder()
                    .bulkOperationId(BULK_OPERATION_ID)
                    .updateOption(UpdateOptionType.PERMANENT_LOCATION)
                    .ruleDetails(
                        List.of(
                            BulkOperationRuleDetails.builder()
                                .updateAction(UpdateActionType.REPLACE_WITH)
                                .updatedValue(LOCATION_ID)
                                .build()))
                    .build()));

    var fetchedRules = ruleService.getRules(BULK_OPERATION_ID);

    assertEquals(rules(), fetchedRules);
  }

  @Test
  void shouldSaveMarcRules() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var operation = BulkOperation.builder().id(BULK_OPERATION_ID).build();
      when(marcRuleRepository.save(
              any(org.folio.bulkops.domain.entity.BulkOperationMarcRule.class)))
          .thenReturn(
              org.folio.bulkops.domain.entity.BulkOperationMarcRule.builder()
                  .id(UUID.randomUUID())
                  .build());

      ruleService.saveMarcRules(operation, marcRules());

      verify(marcRuleRepository, times(0)).deleteAllByBulkOperationId(BULK_OPERATION_ID);
      verify(marcRuleRepository)
          .save(any(org.folio.bulkops.domain.entity.BulkOperationMarcRule.class));
    }
  }

  @Test
  void shouldGetMarcRules() {
    when(marcRuleRepository.findAllByBulkOperationId(BULK_OPERATION_ID))
        .thenReturn(
            List.of(
                org.folio.bulkops.domain.entity.BulkOperationMarcRule.builder()
                    .bulkOperationId(BULK_OPERATION_ID)
                    .tag("100")
                    .ind1(EMPTY)
                    .ind2(EMPTY)
                    .actions(
                        Collections.singletonList(
                            new org.folio.bulkops.domain.dto.MarcAction()
                                .name(UpdateActionType.FIND)
                                .data(
                                    Collections.singletonList(
                                        new org.folio.bulkops.domain.dto.MarcActionDataInner()
                                            .key(org.folio.bulkops.domain.dto.MarcDataType.VALUE)
                                            .value("text")))))
                    .build()));

    var fetchedRules = ruleService.getMarcRules(BULK_OPERATION_ID);

    assertEquals(marcRules(), fetchedRules);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldCheckIfAdministrativeDataRulesArePresent(boolean isPresent) {
    var operation = BulkOperation.builder().id(UUID.randomUUID()).build();
    when(ruleRepository.findFirstByBulkOperationId(operation.getId()))
        .thenReturn(
            isPresent
                ? Optional.of(new org.folio.bulkops.domain.entity.BulkOperationRule())
                : Optional.empty());

    if (isPresent) {
      assertTrue(ruleService.hasAdministrativeUpdates(operation));
    } else {
      assertFalse(ruleService.hasAdministrativeUpdates(operation));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldCheckIfMarcRulesArePresent(boolean isPresent) {
    var operation = BulkOperation.builder().id(UUID.randomUUID()).build();
    when(marcRuleRepository.findFirstByBulkOperationId(operation.getId()))
        .thenReturn(
            isPresent
                ? Optional.of(new org.folio.bulkops.domain.entity.BulkOperationMarcRule())
                : Optional.empty());

    if (isPresent) {
      assertTrue(ruleService.hasMarcUpdates(operation));
    } else {
      assertFalse(ruleService.hasMarcUpdates(operation));
    }
  }

  @Test
  void shouldSaveMarcRuleIfSetToDelete() {
    try (var ignored = new FolioExecutionContextSetter(folioExecutionContext)) {
      var operation = BulkOperation.builder().id(BULK_OPERATION_ID).build();
      var ruleCollection =
          new BulkOperationRuleCollection()
              .bulkOperationRules(
                  List.of(
                      new BulkOperationRule()
                          .bulkOperationId(BULK_OPERATION_ID)
                          .ruleDetails(
                              new RuleDetails()
                                  .option(UpdateOptionType.SET_RECORDS_FOR_DELETE)
                                  .actions(
                                      List.of(new Action().type(UpdateActionType.REPLACE_WITH))))))
              .totalRecords(1);

      when(ruleRepository.save(any(org.folio.bulkops.domain.entity.BulkOperationRule.class)))
          .thenReturn(
              org.folio.bulkops.domain.entity.BulkOperationRule.builder()
                  .id(UUID.randomUUID())
                  .build());

      ruleService.saveRules(operation, ruleCollection);

      verify(marcRuleRepository)
          .save(any(org.folio.bulkops.domain.entity.BulkOperationMarcRule.class));
    }
  }

  @Test
  void shouldNotSaveMarcRuleIfNotSetToDelete() {
    try (var ignored = new FolioExecutionContextSetter(folioExecutionContext)) {
      var operation = BulkOperation.builder().id(BULK_OPERATION_ID).build();
      var ruleCollection =
          new BulkOperationRuleCollection()
              .bulkOperationRules(
                  List.of(
                      new BulkOperationRule()
                          .bulkOperationId(BULK_OPERATION_ID)
                          .ruleDetails(
                              new RuleDetails()
                                  .option(UpdateOptionType.PERMANENT_LOCATION)
                                  .actions(
                                      List.of(new Action().type(UpdateActionType.REPLACE_WITH))))))
              .totalRecords(1);

      when(ruleRepository.save(any(org.folio.bulkops.domain.entity.BulkOperationRule.class)))
          .thenReturn(
              org.folio.bulkops.domain.entity.BulkOperationRule.builder()
                  .id(UUID.randomUUID())
                  .build());

      ruleService.saveRules(operation, ruleCollection);

      verify(marcRuleRepository, times(0))
          .save(any(org.folio.bulkops.domain.entity.BulkOperationMarcRule.class));
    }
  }

  private BulkOperationRuleCollection rules() {
    return new BulkOperationRuleCollection()
        .bulkOperationRules(
            List.of(
                new BulkOperationRule()
                    .bulkOperationId(BULK_OPERATION_ID)
                    .ruleDetails(
                        new RuleDetails()
                            .option(UpdateOptionType.PERMANENT_LOCATION)
                            .tenants(null)
                            .actions(
                                List.of(
                                    new Action()
                                        .parameters(null)
                                        .tenants(null)
                                        .updatedTenants(null)
                                        .type(UpdateActionType.REPLACE_WITH)
                                        .updated(LOCATION_ID))))))
        .totalRecords(1);
  }

  private BulkOperationMarcRuleCollection marcRules() {
    return new BulkOperationMarcRuleCollection()
        .bulkOperationMarcRules(
            Collections.singletonList(
                new BulkOperationMarcRule()
                    .bulkOperationId(BULK_OPERATION_ID)
                    .tag("100")
                    .ind1(EMPTY)
                    .ind2(EMPTY)
                    .actions(
                        Collections.singletonList(
                            new org.folio.bulkops.domain.dto.MarcAction()
                                .name(UpdateActionType.FIND)
                                .data(
                                    Collections.singletonList(
                                        new org.folio.bulkops.domain.dto.MarcActionDataInner()
                                            .key(org.folio.bulkops.domain.dto.MarcDataType.VALUE)
                                            .value("text")))))))
        .totalRecords(1);
  }
}
