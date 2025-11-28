package org.folio.bulkops.processor.marc;

import static java.lang.Character.isDigit;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.folio.bulkops.domain.dto.IdentifierType.HRID;
import static org.folio.bulkops.domain.dto.MarcDataType.SUBFIELD;
import static org.folio.bulkops.domain.dto.MarcDataType.VALUE;
import static org.folio.bulkops.domain.dto.UpdateActionType.ADD_TO_EXISTING;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND;
import static org.folio.bulkops.domain.dto.UpdateActionType.REMOVE_ALL;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE;
import static org.folio.bulkops.util.Constants.SPACE_CHAR;
import static org.folio.bulkops.util.MarcHelper.fetchInstanceUuidOrElseHrid;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.MarcActionDataInner;
import org.folio.bulkops.domain.dto.MarcDataType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.RuleValidationException;
import org.folio.bulkops.processor.MarcDataProcessor;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.util.MarcDateHelper;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.impl.DataFieldImpl;
import org.marc4j.marc.impl.SubfieldImpl;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class MarcInstanceDataProcessor implements MarcDataProcessor {

  private final MarcRulesValidator marcRulesValidator;
  private final ErrorService errorService;

  public void update(
      BulkOperation operation,
      Record marcRecord,
      BulkOperationMarcRuleCollection bulkOperationMarcRuleCollection,
      Date currentDate) {
    var initialRecord = marcRecord.toString();
    bulkOperationMarcRuleCollection
        .getBulkOperationMarcRules()
        .forEach(
            bulkOperationMarcRule -> {
              try {
                marcRulesValidator.validate(bulkOperationMarcRule);
                applyRuleToRecord(bulkOperationMarcRule, marcRecord);
              } catch (RuleValidationException e) {
                log.warn(String.format("Rule validation exception: %s", e.getMessage()));
                errorService.saveError(
                    operation.getId(),
                    getIdentifier(operation, marcRecord),
                    e.getMessage(),
                    ErrorType.ERROR);
              } catch (Exception e) {
                log.error(
                    String.format(
                        "MARC record HRID=%s error: %s",
                        marcRecord.getControlNumber(), e.getMessage()));
                errorService.saveError(
                    operation.getId(),
                    getIdentifier(operation, marcRecord),
                    e.getMessage(),
                    ErrorType.ERROR);
              }
            });
    var updatedRecord = marcRecord.toString();
    if (!StringUtils.equals(initialRecord, updatedRecord)) {
      MarcDateHelper.updateDateTimeControlField(marcRecord, currentDate);
    }
  }

  private String getIdentifier(BulkOperation operation, Record marcRecord) {
    return HRID.equals(operation.getIdentifierType())
        ? marcRecord.getControlNumber()
        : fetchInstanceUuidOrElseHrid(marcRecord);
  }

  private void applyRuleToRecord(BulkOperationMarcRule rule, Record marcRecord)
      throws BulkOperationException {
    var actions = rule.getActions();
    if (FIND.equals(actions.get(0).getName()) && actions.size() == 2) {
      switch (actions.get(1).getName()) {
        case APPEND -> processFindAndAppend(rule, marcRecord);
        case REPLACE_WITH -> processFindAndReplace(rule, marcRecord);
        case REMOVE_FIELD -> processFindAndRemoveField(rule, marcRecord);
        case REMOVE_SUBFIELD -> processFindAndRemoveSubfield(rule, marcRecord);
        default ->
            throw new BulkOperationException(
                String.format(
                    "Action FIND + %s " + "is not supported yet.", actions.get(1).getName()));
      }
    } else if (REMOVE_ALL == actions.get(0).getName()) {
      processRemoveAll(rule, marcRecord);
    } else if (ADD_TO_EXISTING.equals(actions.get(0).getName())) {
      processAddToExisting(rule, marcRecord);
    } else {
      if (rule.getUpdateOption() == UpdateOptionType.SET_RECORDS_FOR_DELETE) {
        if (SET_TO_TRUE.equals(actions.getFirst().getName())) {
          marcRecord.getLeader().setRecordStatus('d');
        } else if (SET_TO_FALSE.equals(actions.getFirst().getName())) {
          marcRecord.getLeader().setRecordStatus('c');
        } else {
          throw new BulkOperationException(
              String.format(
                  "Action %s is not supported for SET_RECORDS_FOR_DELETE option.",
                  actions.getFirst().getName()));
        }
      } else {
        throw new BulkOperationException(
            String.format("Action %s is not supported yet.", actions.getFirst().getName()));
      }
    }
  }

  private void processFindAndAppend(BulkOperationMarcRule rule, Record marcRecord)
      throws BulkOperationException {
    char subfieldCode = rule.getSubfield().charAt(0);
    var findValue = fetchActionDataValue(VALUE, rule.getActions().get(0).getData());
    var appendValue = fetchActionDataValue(VALUE, rule.getActions().get(1).getData());
    var appendSubfieldCode =
        fetchActionDataValue(SUBFIELD, rule.getActions().get(1).getData()).charAt(0);
    findFields(rule, marcRecord).stream()
        .filter(
            df ->
                df.getSubfields(subfieldCode).stream()
                    .anyMatch(sf -> contains(sf.getData(), findValue)))
        .forEach(
            df -> {
              df.addSubfield(new SubfieldImpl(appendSubfieldCode, appendValue));
              df.getSubfields().sort(subfieldComparator);
            });
  }

  private void processFindAndReplace(BulkOperationMarcRule rule, Record marcRecord)
      throws BulkOperationException {
    char subfieldCode = rule.getSubfield().charAt(0);
    var findValue = fetchActionDataValue(VALUE, rule.getActions().get(0).getData());
    var newValue = fetchActionDataValue(VALUE, rule.getActions().get(1).getData());
    findFields(rule, marcRecord)
        .forEach(
            dataField ->
                dataField
                    .getSubfields(subfieldCode)
                    .forEach(
                        subfield -> {
                          if (contains(subfield.getData(), findValue)) {
                            subfield.setData(replace(subfield.getData(), findValue, newValue));
                          }
                        }));
  }

  private void processFindAndRemoveField(BulkOperationMarcRule rule, Record marcRecord)
      throws BulkOperationException {
    char subfieldCode = rule.getSubfield().charAt(0);
    var findValue = fetchActionDataValue(VALUE, rule.getActions().getFirst().getData());
    findFields(rule, marcRecord).stream()
        .filter(
            df ->
                df.getSubfields(subfieldCode).stream()
                    .anyMatch(sf -> contains(sf.getData(), findValue)))
        .forEach(marcRecord::removeVariableField);
  }

  private void processFindAndRemoveSubfield(BulkOperationMarcRule rule, Record marcRecord)
      throws BulkOperationException {
    char subfieldCode = rule.getSubfield().charAt(0);
    var findValue = fetchActionDataValue(VALUE, rule.getActions().getFirst().getData());
    findFields(rule, marcRecord)
        .forEach(
            df ->
                df.getSubfields(subfieldCode).stream()
                    .filter(sf -> contains(sf.getData(), findValue))
                    .forEach(df::removeSubfield));
  }

  private List<DataField> findFields(BulkOperationMarcRule rule, Record marcRecord) {
    char ind1 = fetchIndicatorValue(rule.getInd1());
    char ind2 = fetchIndicatorValue(rule.getInd2());
    return marcRecord.getDataFields().stream()
        .filter(
            dataField ->
                rule.getTag().equals(dataField.getTag())
                    && ind1 == dataField.getIndicator1()
                    && ind2 == dataField.getIndicator2())
        .toList();
  }

  private void processRemoveAll(BulkOperationMarcRule rule, Record marcRecord) {
    var tag = rule.getTag();
    char ind1 = fetchIndicatorValue(rule.getInd1());
    char ind2 = fetchIndicatorValue(rule.getInd2());
    char subfieldCode = rule.getSubfield().charAt(0);
    marcRecord
        .getDataFields()
        .removeIf(
            dataField ->
                dataField.getTag().equals(tag)
                    && dataField.getIndicator1() == ind1
                    && dataField.getIndicator2() == ind2
                    && dataField.getSubfields().stream()
                        .anyMatch(subfield -> subfield.getCode() == subfieldCode));
  }

  private void processAddToExisting(BulkOperationMarcRule rule, Record marcRecord)
      throws BulkOperationException {
    var tag = rule.getTag();
    char ind1 = fetchIndicatorValue(rule.getInd1());
    char ind2 = fetchIndicatorValue(rule.getInd2());
    var newField = new DataFieldImpl(tag, ind1, ind2);
    var subfieldCode = rule.getSubfield().charAt(0);
    var value = fetchActionDataValue(VALUE, rule.getActions().getFirst().getData());
    newField.addSubfield(new SubfieldImpl(subfieldCode, value));
    if (ObjectUtils.isNotEmpty(rule.getSubfields())) {
      for (var subfieldAction : rule.getSubfields()) {
        var action = subfieldAction.getActions().getFirst();
        if (ADD_TO_EXISTING.equals(action.getName())) {
          subfieldCode = subfieldAction.getSubfield().charAt(0);
          value = fetchActionDataValue(VALUE, action.getData());
          newField.addSubfield(new SubfieldImpl(subfieldCode, value));
        }
      }
      newField.getSubfields().sort(subfieldComparator);
    }
    marcRecord.addVariableField(newField);
    marcRecord.getDataFields().sort(Comparator.comparing(DataField::toString));
  }

  private char fetchIndicatorValue(String s) {
    return "\\".equals(s) ? SPACE_CHAR : s.charAt(0);
  }

  private String fetchActionDataValue(MarcDataType dataType, List<MarcActionDataInner> actionData)
      throws BulkOperationException {
    return actionData.stream()
        .filter(data -> dataType.equals(data.getKey()))
        .findFirst()
        .map(MarcActionDataInner::getValue)
        .orElseThrow(
            () -> new BulkOperationException(String.format("Action data %s is absent.", dataType)));
  }

  private final Comparator<Subfield> subfieldComparator =
      (sf1, sf2) -> {
        if (isDigit(sf1.getCode()) ^ isDigit(sf2.getCode())) {
          return isDigit(sf1.getCode()) ? 1 : -1;
        } else {
          return sf1.toString().compareTo(sf2.toString());
        }
      };
}
