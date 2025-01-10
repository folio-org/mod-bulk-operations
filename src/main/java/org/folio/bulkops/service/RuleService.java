package org.folio.bulkops.service;

import java.util.UUID;

import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.mapper.MarcRulesMapper;
import org.folio.bulkops.repository.BulkOperationMarcRuleRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.repository.BulkOperationRuleDetailsRepository;
import org.folio.bulkops.repository.BulkOperationRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RuleService {
  private final BulkOperationRuleRepository ruleRepository;
  private final BulkOperationRuleDetailsRepository ruleDetailsRepository;
  private final BulkOperationRepository bulkOperationRepository;
  private final BulkOperationMarcRuleRepository marcRuleRepository;
  private final MarcRulesMapper marcRulesMapper;

  @Transactional
  public BulkOperationRuleCollection saveRules(BulkOperation bulkOperation, BulkOperationRuleCollection ruleCollection) {
    ruleRepository.deleteAllByBulkOperationId(bulkOperation.getId());
    marcRuleRepository.deleteAllByBulkOperationId(bulkOperation.getId());

    ruleCollection.getBulkOperationRules().forEach(bulkOperationRule -> {
      var rule = ruleRepository.save(org.folio.bulkops.domain.entity.BulkOperationRule.builder()
        .bulkOperationId(bulkOperationRule.getBulkOperationId())
        .updateOption(bulkOperationRule.getRuleDetails().getOption())
        .build());
      bulkOperationRule.getRuleDetails().getActions()
        .forEach(action -> ruleDetailsRepository.save(org.folio.bulkops.domain.entity.BulkOperationRuleDetails.builder()
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
    return ruleCollection;
  }

  public BulkOperationRuleCollection getRules(UUID bulkOperationId) {
    var rules = ruleRepository.findAllByBulkOperationId(bulkOperationId).stream()
      .map(this::mapBulkOperationRuleToDto)
      .toList();
    if (rules.isEmpty()) {
      throw new NotFoundException("Bulk operation rules were not found by bulk operation id=" + bulkOperationId);
    }
    return new BulkOperationRuleCollection().bulkOperationRules(rules).totalRecords(rules.size());
  }

  private BulkOperationRule mapBulkOperationRuleToDto(org.folio.bulkops.domain.entity.BulkOperationRule entity) {
    return new BulkOperationRule()
      .bulkOperationId(entity.getBulkOperationId())
      .ruleDetails(new BulkOperationRuleRuleDetails()
        .option(entity.getUpdateOption())
        .actions(entity.getRuleDetails().stream()
          .map(details -> new Action()
            .type(details.getUpdateAction())
            .initial(details.getInitialValue())
            .updated(details.getUpdatedValue())
            .parameters(details.getParameters())
            .tenants(details.getActionTenants())
            .updatedTenants(details.getUpdatedTenants()))
          .toList())
        .tenants(entity.getRuleDetails().get(0).getRuleTenants()));
  }

  @Transactional
  public BulkOperationMarcRuleCollection saveMarcRules(BulkOperation bulkOperation, BulkOperationMarcRuleCollection ruleCollection) {
    marcRuleRepository.deleteAllByBulkOperationId(bulkOperation.getId());

    ruleCollection.getBulkOperationMarcRules()
      .forEach(marcRule -> marcRuleRepository.save(marcRulesMapper.mapToEntity(marcRule)));

    return ruleCollection;
  }

  public BulkOperationMarcRuleCollection getMarcRules(UUID bulkOperationId) {
    var rules = marcRuleRepository.findAllByBulkOperationId(bulkOperationId).stream()
      .map(marcRulesMapper::mapToDto)
      .toList();
    if (rules.isEmpty()) {
      throw new NotFoundException("Bulk operation MARC rules were not found by bulk operation id=" + bulkOperationId);
    }
    return new BulkOperationMarcRuleCollection().bulkOperationMarcRules(rules).totalRecords(rules.size());
  }
}
