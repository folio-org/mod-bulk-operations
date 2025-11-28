package org.folio.bulkops.service;

import static org.folio.bulkops.util.Constants.LEADER_TAG;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.MarcAction;
import org.folio.bulkops.domain.dto.RuleDetails;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.mapper.MarcRulesMapper;
import org.folio.bulkops.repository.BulkOperationMarcRuleRepository;
import org.folio.bulkops.repository.BulkOperationRuleDetailsRepository;
import org.folio.bulkops.repository.BulkOperationRuleRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RuleService {
  private final BulkOperationRuleRepository ruleRepository;
  private final BulkOperationRuleDetailsRepository ruleDetailsRepository;
  private final BulkOperationMarcRuleRepository marcRuleRepository;
  private final MarcRulesMapper marcRulesMapper;
  private final ApplicationContext applicationContext;

  @Transactional
  public BulkOperationRuleCollection saveRules(
      BulkOperation bulkOperation, BulkOperationRuleCollection ruleCollection) {
    ruleRepository.deleteAllByBulkOperationId(bulkOperation.getId());
    marcRuleRepository.deleteAllByBulkOperationId(bulkOperation.getId());

    ruleCollection
        .getBulkOperationRules()
        .forEach(
            bulkOperationRule -> {
              var rule =
                  ruleRepository.save(
                      org.folio.bulkops.domain.entity.BulkOperationRule.builder()
                          .bulkOperationId(bulkOperationRule.getBulkOperationId())
                          .updateOption(bulkOperationRule.getRuleDetails().getOption())
                          .build());
              bulkOperationRule
                  .getRuleDetails()
                  .getActions()
                  .forEach(
                      action ->
                          ruleDetailsRepository.save(
                              org.folio.bulkops.domain.entity.BulkOperationRuleDetails.builder()
                                  .ruleId(rule.getId())
                                  .updateAction(action.getType())
                                  .initialValue(action.getInitial())
                                  .updatedValue(action.getUpdated())
                                  .parameters(action.getParameters())
                                  .ruleTenants(bulkOperationRule.getRuleDetails().getTenants())
                                  .actionTenants(action.getTenants())
                                  .updatedTenants(action.getUpdatedTenants())
                                  .build()));
            });
    saveMarcRuleIfSetToDelete(bulkOperation, ruleCollection);
    return ruleCollection;
  }

  private void saveMarcRuleIfSetToDelete(
      BulkOperation bulkOperation, BulkOperationRuleCollection ruleCollection) {
    ruleCollection
        .getBulkOperationRules()
        .forEach(
            rule -> {
              if (rule.getRuleDetails().getOption() == UpdateOptionType.SET_RECORDS_FOR_DELETE) {
                rule.getRuleDetails()
                    .getActions()
                    .forEach(
                        action -> {
                          var marcRules = new BulkOperationMarcRuleCollection();
                          var marcRule =
                              new org.folio.bulkops.domain.dto.BulkOperationMarcRule()
                                  .bulkOperationId(rule.getBulkOperationId())
                                  .actions(List.of(new MarcAction().name(action.getType())))
                                  .tag(LEADER_TAG)
                                  .updateOption(UpdateOptionType.SET_RECORDS_FOR_DELETE);
                          marcRules.addBulkOperationMarcRulesItem(marcRule);
                          marcRules.totalRecords(1);
                          RuleService selfProxy = applicationContext.getBean(RuleService.class);
                          // Proxy is used, transactional behavior works.
                          selfProxy.saveMarcRules(bulkOperation, marcRules);
                        });
              }
            });
  }

  public BulkOperationRuleCollection getRules(UUID bulkOperationId) {
    var rules =
        ruleRepository.findAllByBulkOperationId(bulkOperationId).stream()
            .map(this::mapBulkOperationRuleToDto)
            .toList();
    return new BulkOperationRuleCollection().bulkOperationRules(rules).totalRecords(rules.size());
  }

  private BulkOperationRule mapBulkOperationRuleToDto(
      org.folio.bulkops.domain.entity.BulkOperationRule entity) {
    return new BulkOperationRule()
        .bulkOperationId(entity.getBulkOperationId())
        .ruleDetails(
            new RuleDetails()
                .option(entity.getUpdateOption())
                .actions(
                    entity.getRuleDetails().stream()
                        .map(
                            details ->
                                new Action()
                                    .type(details.getUpdateAction())
                                    .initial(details.getInitialValue())
                                    .updated(details.getUpdatedValue())
                                    .parameters(details.getParameters())
                                    .tenants(details.getActionTenants())
                                    .updatedTenants(details.getUpdatedTenants()))
                        .toList())
                .tenants(entity.getRuleDetails().getFirst().getRuleTenants()));
  }

  @Transactional
  public BulkOperationMarcRuleCollection saveMarcRules(
      BulkOperation bulkOperation, BulkOperationMarcRuleCollection ruleCollection) {

    ruleCollection
        .getBulkOperationMarcRules()
        .forEach(marcRule -> marcRuleRepository.save(marcRulesMapper.mapToEntity(marcRule)));

    return ruleCollection;
  }

  public BulkOperationMarcRuleCollection getMarcRules(UUID bulkOperationId) {
    var rules =
        marcRuleRepository.findAllByBulkOperationId(bulkOperationId).stream()
            .map(marcRulesMapper::mapToDto)
            .toList();
    return new BulkOperationMarcRuleCollection()
        .bulkOperationMarcRules(rules)
        .totalRecords(rules.size());
  }

  public boolean hasAdministrativeUpdates(BulkOperation bulkOperation) {
    return ruleRepository.findFirstByBulkOperationId(bulkOperation.getId()).isPresent();
  }

  public boolean hasMarcUpdates(BulkOperation bulkOperation) {
    return marcRuleRepository.findFirstByBulkOperationId(bulkOperation.getId()).isPresent();
  }
}
