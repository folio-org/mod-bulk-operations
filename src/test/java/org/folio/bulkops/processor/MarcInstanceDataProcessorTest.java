package org.folio.bulkops.processor;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.dto.UpdateActionType.ADDITIONAL_SUBFIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.ADD_TO_EXISTING;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND;
import static org.mockito.Mockito.verify;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.MarcAction;
import org.folio.bulkops.domain.dto.MarcActionDataInner;
import org.folio.bulkops.domain.dto.MarcDataType;
import org.folio.bulkops.domain.dto.MarcSubfieldAction;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.service.ErrorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.marc4j.marc.impl.ControlFieldImpl;
import org.marc4j.marc.impl.DataFieldImpl;
import org.marc4j.marc.impl.RecordImpl;
import org.marc4j.marc.impl.SubfieldImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

class MarcInstanceDataProcessorTest extends BaseTest {
  @MockBean
  private ErrorService errorService;
  @Autowired
  private MarcInstanceDataProcessor processor;

  @Test
  void shouldApplyFindAndAppendRule() {
    var bulkOperationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(bulkOperationId)
      .identifierType(IdentifierType.ID)
      .build();
    var marcRecord = new RecordImpl();
    var dataField = new DataFieldImpl("500", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('b', "text b"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("500", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('b', "Text b"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("500", ' ', '1');
    dataField.addSubfield(new SubfieldImpl('b', "Text b"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("500", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "Text b"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("510", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('b', "Text b"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("500", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('b', "text b"));
    marcRecord.addVariableField(dataField);
    var findAndAppendRule = new BulkOperationMarcRule()
      .bulkOperationId(bulkOperationId)
      .tag("500")
      .ind1("1")
      .ind2("\\")
      .subfield("b")
      .actions(List.of(
        new MarcAction()
          .name(FIND)
          .data(Collections.singletonList(new MarcActionDataInner()
            .key(MarcDataType.VALUE)
            .value("text b"))),
        new MarcAction()
          .name(UpdateActionType.APPEND)
          .data(List.of(
            new MarcActionDataInner()
              .key(MarcDataType.VALUE)
              .value("text a"),
            new MarcActionDataInner()
              .key(MarcDataType.SUBFIELD)
              .value("a")))));
    var rules = new BulkOperationMarcRuleCollection()
      .bulkOperationMarcRules(Collections.singletonList(findAndAppendRule))
      .totalRecords(1);

    processor.update(operation, marcRecord, rules);

    var dataFields = marcRecord.getDataFields();
    assertThat(dataFields).hasSize(6);

    assertThat(dataFields.get(0)).hasToString("500 1 $atext a$btext b");
    assertThat(dataFields.get(1).getSubfields()).hasSize(1);
    assertThat(dataFields.get(2).getSubfields()).hasSize(1);
    assertThat(dataFields.get(3).getSubfields()).hasSize(1);
    assertThat(dataFields.get(4).getSubfields()).hasSize(1);
    assertThat(dataFields.get(5)).hasToString("500 1 $atext a$btext b");
  }

  @Test
  void shouldApplyFindAndRemoveField() {
    var bulkOperationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(bulkOperationId)
      .identifierType(IdentifierType.ID)
      .build();
    var marcRecord = new RecordImpl();
    var dataField = new DataFieldImpl("500", '1', '1');
    dataField.addSubfield(new SubfieldImpl('a', "text a"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("500", '1', '1');
    dataField.addSubfield(new SubfieldImpl('a', "Text a"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("510", '1', '1');
    dataField.addSubfield(new SubfieldImpl('a', "text a"));
    marcRecord.addVariableField(dataField);
    var findAndAppendRule = new BulkOperationMarcRule()
      .bulkOperationId(bulkOperationId)
      .tag("500")
      .ind1("1")
      .ind2("1")
      .subfield("a")
      .actions(List.of(
        new MarcAction()
          .name(FIND)
          .data(Collections.singletonList(new MarcActionDataInner()
            .key(MarcDataType.VALUE)
            .value("text a"))),
        new MarcAction()
          .name(UpdateActionType.REMOVE_FIELD)
          .data(Collections.emptyList())));
    var rules = new BulkOperationMarcRuleCollection()
      .bulkOperationMarcRules(Collections.singletonList(findAndAppendRule))
      .totalRecords(1);

    processor.update(operation, marcRecord, rules);

    var dataFields = marcRecord.getDataFields();
    assertThat(dataFields).hasSize(2);

    assertThat(dataFields.get(0)).hasToString("500 11$aText a");
    assertThat(dataFields.get(1)).hasToString("510 11$atext a");
  }

  @Test
  void shouldApplyFindAndRemoveSubfield() {
    var bulkOperationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(bulkOperationId)
      .identifierType(IdentifierType.ID)
      .build();
    var marcRecord = new RecordImpl();
    var dataField = new DataFieldImpl("500", '1', '1');
    dataField.addSubfield(new SubfieldImpl('a', "text a"));
    dataField.addSubfield(new SubfieldImpl('b', "text b"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("500", '1', '1');
    dataField.addSubfield(new SubfieldImpl('a', "Text a"));
    dataField.addSubfield(new SubfieldImpl('b', "Text b"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("510", '1', '1');
    dataField.addSubfield(new SubfieldImpl('a', "text a"));
    dataField.addSubfield(new SubfieldImpl('b', "text b"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("500", '1', '1');
    dataField.addSubfield(new SubfieldImpl('a', "text a"));
    dataField.addSubfield(new SubfieldImpl('b', "text b"));
    marcRecord.addVariableField(dataField);
    var findAndAppendRule = new BulkOperationMarcRule()
      .bulkOperationId(bulkOperationId)
      .tag("500")
      .ind1("1")
      .ind2("1")
      .subfield("a")
      .actions(List.of(
        new MarcAction()
          .name(FIND)
          .data(Collections.singletonList(new MarcActionDataInner()
            .key(MarcDataType.VALUE)
            .value("text a"))),
        new MarcAction()
          .name(UpdateActionType.REMOVE_SUBFIELD)
          .data(Collections.emptyList())));
    var rules = new BulkOperationMarcRuleCollection()
      .bulkOperationMarcRules(Collections.singletonList(findAndAppendRule))
      .totalRecords(1);

    processor.update(operation, marcRecord, rules);

    var dataFields = marcRecord.getDataFields();
    assertThat(dataFields).hasSize(4);

    assertThat(dataFields.get(0)).hasToString("500 11$btext b");
    assertThat(dataFields.get(1)).hasToString("500 11$aText a$bText b");
    assertThat(dataFields.get(2)).hasToString("510 11$atext a$btext b");
    assertThat(dataFields.get(3)).hasToString("500 11$btext b");
  }

  @Test
  void shouldApplyFindAndReplaceRule() {
    var bulkOperationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(bulkOperationId)
      .identifierType(IdentifierType.ID)
      .build();
    var marcRecord = new RecordImpl();
    var dataField = new DataFieldImpl("500", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "old value"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("500", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "Old value"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("500", ' ', '1');
    dataField.addSubfield(new SubfieldImpl('a', "old value"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("500", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('b', "old value"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("510", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "old value"));
    marcRecord.addVariableField(dataField);
    var findAndReplaceRule = new BulkOperationMarcRule()
      .bulkOperationId(bulkOperationId)
      .tag("500")
      .ind1("1")
      .ind2("\\")
      .subfield("a")
      .actions(List.of(
        new MarcAction()
          .name(FIND)
          .data(Collections.singletonList(new MarcActionDataInner()
            .key(MarcDataType.VALUE)
            .value("old value"))),
        new MarcAction()
          .name(UpdateActionType.REPLACE_WITH)
          .data(List.of(
            new MarcActionDataInner()
              .key(MarcDataType.VALUE)
              .value("new value")))));
    var rules = new BulkOperationMarcRuleCollection()
      .bulkOperationMarcRules(Collections.singletonList(findAndReplaceRule))
      .totalRecords(1);

    processor.update(operation, marcRecord, rules);

    var dataFields = marcRecord.getDataFields();
    assertThat(dataFields).hasSize(5);

    dataFields.forEach(df -> assertThat(df.getSubfields()).hasSize(1));

    var subfield = dataFields.get(0).getSubfields().get(0);
    assertThat(subfield.getCode()).isEqualTo('a');
    assertThat(subfield.getData()).isEqualTo("new value");
    subfield = dataFields.get(1).getSubfields().get(0);
    assertThat(subfield.getCode()).isEqualTo('a');
    assertThat(subfield.getData()).isEqualTo("Old value");
    subfield = dataFields.get(2).getSubfields().get(0);
    assertThat(subfield.getCode()).isEqualTo('a');
    assertThat(subfield.getData()).isEqualTo("old value");
    subfield = dataFields.get(3).getSubfields().get(0);
    assertThat(subfield.getCode()).isEqualTo('b');
    assertThat(subfield.getData()).isEqualTo("old value");
    subfield = dataFields.get(4).getSubfields().get(0);
    assertThat(subfield.getCode()).isEqualTo('a');
    assertThat(subfield.getData()).isEqualTo("old value");

  }

  @Test
  void shouldApplyRemoveAllRule() {
    var bulkOperationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(bulkOperationId)
      .identifierType(IdentifierType.ID)
      .build();
    var marcRecord = new RecordImpl();
    var dataField = new DataFieldImpl("500", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "text a"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("500", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "Text a"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("500", ' ', '1');
    dataField.addSubfield(new SubfieldImpl('a', "Text a"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("500", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('b', "Text b"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("500", '1', '1');
    dataField.addSubfield(new SubfieldImpl('b', "Text b"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("510", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "Text a"));
    marcRecord.addVariableField(dataField);
    var findAndAppendRule = new BulkOperationMarcRule()
      .bulkOperationId(bulkOperationId)
      .tag("500")
      .ind1("1")
      .ind2("\\")
      .subfield("a")
      .actions(List.of(
        new MarcAction()
          .name(UpdateActionType.REMOVE_ALL)
          .data(Collections.singletonList(new MarcActionDataInner()
            .key(MarcDataType.VALUE)
            .value("text a")))));
    var rules = new BulkOperationMarcRuleCollection()
      .bulkOperationMarcRules(Collections.singletonList(findAndAppendRule))
      .totalRecords(1);

    processor.update(operation, marcRecord, rules);

    var dataFields = marcRecord.getDataFields();
    assertThat(dataFields).hasSize(4);

    var subfields = dataFields.get(0).getSubfields();
    assertThat(subfields).hasSize(1);
    assertThat(subfields.get(0).getCode()).isEqualTo('a');
    assertThat(subfields.get(0).getData()).isEqualTo("Text a");
    subfields = dataFields.get(1).getSubfields();
    assertThat(subfields).hasSize(1);
    assertThat(subfields.get(0).getCode()).isEqualTo('b');
    assertThat(subfields.get(0).getData()).isEqualTo("Text b");
    subfields = dataFields.get(2).getSubfields();
    assertThat(subfields).hasSize(1);
    assertThat(subfields.get(0).getCode()).isEqualTo('b');
    assertThat(subfields.get(0).getData()).isEqualTo("Text b");
    subfields = dataFields.get(3).getSubfields();
    assertThat(subfields).hasSize(1);
    assertThat(subfields.get(0).getCode()).isEqualTo('a');
    assertThat(subfields.get(0).getData()).isEqualTo("Text a");
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    null   | text b | b    | Action data VALUE is absent.    | ID   | true
    text a | null   | b    | Action data VALUE is absent.    | ID   | false
    text a | text b | null | Action data SUBFIELD is absent. | HRID | false
    """, delimiter = '|', nullValues = "null")
  void shouldSaveErrorsOnBadRule(String findValue, String appendValue, String subfieldValue, String errorMessage,
    IdentifierType identifierType, boolean isInstanceId) {
    var bulkOperationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(bulkOperationId)
      .identifierType(identifierType)
      .build();
    var hrid = "inst000001";
    var controlNumberField = new ControlFieldImpl("001", hrid);
    var dataField500 = new DataFieldImpl("500", '1', '2');
    dataField500.addSubfield(new SubfieldImpl('a', "text a"));
    var instanceId = UUID.randomUUID().toString();
    var dataField999 = new DataFieldImpl("999", 'f', 'f');
    dataField999.addSubfield(new SubfieldImpl('i', instanceId));
    var marcRecord = new RecordImpl();
    marcRecord.addVariableField(controlNumberField);
    marcRecord.addVariableField(dataField500);
    if (isInstanceId) {
      marcRecord.addVariableField(dataField999);
    }
    var findAndAppendRule = new BulkOperationMarcRule()
      .bulkOperationId(bulkOperationId)
      .tag("500")
      .ind1("1")
      .ind2("2")
      .subfield("a")
      .actions(List.of(
        new MarcAction()
          .name(FIND)
          .data(Collections.singletonList(new MarcActionDataInner()
            .key(MarcDataType.VALUE)
            .value(findValue))),
        new MarcAction()
          .name(UpdateActionType.APPEND)
          .data(List.of(
            new MarcActionDataInner()
              .key(MarcDataType.VALUE)
              .value(appendValue),
            new MarcActionDataInner()
              .key(MarcDataType.SUBFIELD)
              .value(subfieldValue)))));
    var rules = new BulkOperationMarcRuleCollection()
      .bulkOperationMarcRules(Collections.singletonList(findAndAppendRule))
      .totalRecords(1);

    processor.update(operation, marcRecord, rules);

    var identifier = isInstanceId ? instanceId : hrid;
    verify(errorService).saveError(bulkOperationId, identifier, errorMessage);
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    FIND                  | MARK_AS_STAFF_ONLY    | MARK_AS_STAFF_ONLY
    FIND                  | null                  | FIND
    FIND_AND_REMOVE_THESE | null                  | FIND_AND_REMOVE_THESE
    """, delimiter = '|', nullValues = "null")
  void shouldNotApplyUnsupportedAction(UpdateActionType updateActionType1, UpdateActionType updateActionType2, String errorMessageArg) {
    var bulkOperationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(bulkOperationId)
      .identifierType(IdentifierType.HRID)
      .build();
    var hrid = "inst000001";
    var controlNumberField = new ControlFieldImpl("001", hrid);
    var marcRecord = new RecordImpl();
    marcRecord.addVariableField(controlNumberField);

    var actions = new ArrayList<MarcAction>();
    actions.add(new MarcAction().name(updateActionType1));
    if (updateActionType2 != null) {
      actions.add(new MarcAction().name(updateActionType2));
    }

    var findAndAppendRule = new BulkOperationMarcRule()
      .bulkOperationId(bulkOperationId)
      .tag("500")
      .ind1("1")
      .ind2("2")
      .subfield("a")
      .actions(actions);
    var rules = new BulkOperationMarcRuleCollection()
      .bulkOperationMarcRules(Collections.singletonList(findAndAppendRule))
      .totalRecords(1);

    processor.update(operation, marcRecord, rules);

    var errorMessageTemplate = FIND.equals(updateActionType1) && nonNull(updateActionType2) ?
      "Action FIND + %s is not supported yet." :
      "Action %s is not supported yet.";
    var errorMessage = String.format(errorMessageTemplate, errorMessageArg);
    verify(errorService).saveError(bulkOperationId, hrid, errorMessage);
  }

  @Test
  void shouldApplyAddToExistingRule() {
    var bulkOperationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(bulkOperationId)
      .identifierType(IdentifierType.ID)
      .build();
    var marcRecord = new RecordImpl();
    var dataField = new DataFieldImpl("500", '1', '1');
    dataField.addSubfield(new SubfieldImpl('a', "text a"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("550", '1', '1');
    dataField.addSubfield(new SubfieldImpl('a', "text a"));
    marcRecord.addVariableField(dataField);
    var findAndAppendRule = new BulkOperationMarcRule()
      .bulkOperationId(bulkOperationId)
      .tag("510")
      .ind1("1")
      .ind2("1")
      .subfield("b")
      .actions(List.of(
        new MarcAction()
          .name(ADD_TO_EXISTING)
          .data(Collections.singletonList(new MarcActionDataInner()
            .key(MarcDataType.VALUE)
            .value("text b"))),
        new MarcAction()
          .name(ADDITIONAL_SUBFIELD)
          .data(Collections.emptyList())))
      .subfields(List.of(new MarcSubfieldAction()
          .subfield("1")
          .actions(List.of(new MarcAction()
              .name(ADD_TO_EXISTING)
              .data(Collections.singletonList(new MarcActionDataInner()
                .key(MarcDataType.VALUE)
                .value("text 1"))),
            new MarcAction()
              .name(ADDITIONAL_SUBFIELD)
              .data(Collections.emptyList()))),
        new MarcSubfieldAction()
          .subfield("a")
          .actions(List.of(new MarcAction()
              .name(ADD_TO_EXISTING)
              .data(Collections.singletonList(new MarcActionDataInner()
                .key(MarcDataType.VALUE)
                .value("text a"))),
            new MarcAction()
              .name(ADDITIONAL_SUBFIELD)
              .data(Collections.emptyList())))));
    var rules = new BulkOperationMarcRuleCollection()
      .bulkOperationMarcRules(Collections.singletonList(findAndAppendRule))
      .totalRecords(1);

    processor.update(operation, marcRecord, rules);

    var dataFields = marcRecord.getDataFields();
    assertThat(dataFields).hasSize(3);

    assertThat(dataFields.get(0).getTag()).isEqualTo("500");
    assertThat(dataFields.get(1).getTag()).isEqualTo("510");
    assertThat(dataFields.get(1)).hasToString("510 11$atext a$btext b$1text 1");
    assertThat(dataFields.get(2).getTag()).isEqualTo("550");
  }
}
