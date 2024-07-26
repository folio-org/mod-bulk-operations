package org.folio.bulkops.processor;

import static org.folio.bulkops.domain.dto.IdentifierType.HRID;
import static org.folio.bulkops.domain.dto.MarcDataType.SUBFIELD;
import static org.folio.bulkops.domain.dto.MarcDataType.VALUE;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND;
import static org.folio.bulkops.util.Constants.FIELD_999;
import static org.folio.bulkops.util.Constants.INDICATOR_F;
import static org.folio.bulkops.util.Constants.SPACE_CHAR;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.domain.dto.MarcActionDataInner;
import org.folio.bulkops.domain.dto.MarcDataType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.service.ErrorService;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.impl.SubfieldImpl;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class MarcInstanceDataProcessor {
  private final ErrorService errorService;

  public void update(BulkOperation operation, Record marcRecord, BulkOperationMarcRuleCollection bulkOperationMarcRuleCollection) {
    bulkOperationMarcRuleCollection.getBulkOperationMarcRules().forEach(bulkOperationMarcRule -> {
      try {
        applyRuleToRecord(bulkOperationMarcRule, marcRecord);
      } catch (Exception e) {
        log.error(String.format("MARC record HRID=%s error: %s", marcRecord.getControlNumber(), e.getMessage()));
        var identifier = HRID.equals(operation.getIdentifierType()) ?
          marcRecord.getControlNumber() :
          fetchInstanceUuidOrElseHrid(marcRecord);
        errorService.saveError(operation.getId(), identifier, e.getMessage());
      }
    });
  }

  private void applyRuleToRecord(BulkOperationMarcRule rule, Record marcRecord) throws BulkOperationException {
    var actions = rule.getActions();
    if (FIND.equals(actions.get(0).getName()) && actions.size() == 2) {
      switch (actions.get(1).getName()) {
        case APPEND -> processFindAndAppend(rule, marcRecord);
        case REPLACE_WITH -> processFindAndReplace(rule, marcRecord);
        default -> throw new BulkOperationException(String.format("Action FIND + %s is not supported yet.", actions.get(1).getName()));
      }
    } else {
      throw new BulkOperationException(String.format("Action %s is not supported yet.", actions.get(0).getName()));
    }
  }

  private void processFindAndAppend(BulkOperationMarcRule rule, Record marcRecord) throws BulkOperationException {
    char subfieldCode = rule.getSubfield().charAt(0);
    var findValue = fetchActionDataValue(VALUE, rule.getActions().get(0).getData());
    var appendValue = fetchActionDataValue(VALUE, rule.getActions().get(1).getData());
    var appendSubfieldCode = fetchActionDataValue(SUBFIELD, rule.getActions().get(1).getData()).charAt(0);
    findFields(rule, marcRecord).forEach(dataField ->
      dataField.getSubfields().forEach(subfield -> {
        if (subfieldCode == subfield.getCode() && findValue.equals(subfield.getData())) {
          dataField.addSubfield(new SubfieldImpl(appendSubfieldCode, appendValue));
        }
      }));
  }

  private void processFindAndReplace(BulkOperationMarcRule rule, Record marcRecord) throws BulkOperationException {
    char subfieldCode = rule.getSubfield().charAt(0);
    var findValue = fetchActionDataValue(VALUE, rule.getActions().get(0).getData());
    var newValue = fetchActionDataValue(VALUE, rule.getActions().get(1).getData());
    findFields(rule, marcRecord).forEach(dataField ->
      dataField.getSubfields().forEach(subfield -> {
        if (subfieldCode == subfield.getCode() && findValue.equals(subfield.getData())) {
          subfield.setData(newValue);
        }
      }));
  }

  private List<DataField> findFields(BulkOperationMarcRule rule, Record marcRecord) {
    char ind1 = fetchIndicatorValue(rule.getInd1());
    char ind2 = fetchIndicatorValue(rule.getInd2());
    return marcRecord.getDataFields().stream()
      .filter(dataField -> rule.getTag().equals(dataField.getTag()) &&
        ind1 == dataField.getIndicator1() && ind2 == dataField.getIndicator2())
      .toList();
  }

  private char fetchIndicatorValue(String s) {
    return "\\".equals(s) ? SPACE_CHAR : s.charAt(0);
  }

  private String fetchInstanceUuidOrElseHrid(Record marcRecord) {
    return marcRecord.getDataFields().stream()
      .filter(f -> FIELD_999.equals(f.getTag()) && INDICATOR_F == f.getIndicator1() && INDICATOR_F == f.getIndicator2())
      .findFirst()
      .map(f -> f.getSubfield('i'))
      .map(Subfield::getData)
      .orElse(marcRecord.getControlNumber());
  }

  private String fetchActionDataValue(MarcDataType dataType, List<MarcActionDataInner> actionData) throws BulkOperationException {
    return actionData.stream()
      .filter(data -> dataType.equals(data.getKey()))
      .findFirst()
      .map(MarcActionDataInner::getValue)
      .orElseThrow(() -> new BulkOperationException(String.format("Action data %s is absent.", dataType)));
  }
}
