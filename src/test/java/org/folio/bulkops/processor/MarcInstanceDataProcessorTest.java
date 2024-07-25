package org.folio.bulkops.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.MarcAction;
import org.folio.bulkops.domain.dto.MarcActionDataInner;
import org.folio.bulkops.domain.dto.MarcDataType;
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
    var dataField = new DataFieldImpl("500", '1', '2');
    dataField.addSubfield(new SubfieldImpl('a', "text a"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("500", '1', '2');
    dataField.addSubfield(new SubfieldImpl('a', "Text a"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("500", '2', '1');
    dataField.addSubfield(new SubfieldImpl('a', "Text a"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("500", '1', '2');
    dataField.addSubfield(new SubfieldImpl('b', "Text a"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("510", '1', '2');
    dataField.addSubfield(new SubfieldImpl('a', "Text a"));
    marcRecord.addVariableField(dataField);
    var findAndAppendRule = new BulkOperationMarcRule()
      .id(UUID.randomUUID())
      .bulkOperationId(bulkOperationId)
      .tag("500")
      .ind1("1")
      .ind2("2")
      .subfield("a")
      .actions(List.of(
        new MarcAction()
          .name(UpdateActionType.FIND)
          .data(Collections.singletonList(new MarcActionDataInner()
            .key(MarcDataType.VALUE)
            .value("text a"))),
        new MarcAction()
          .name(UpdateActionType.APPEND)
          .data(List.of(
            new MarcActionDataInner()
              .key(MarcDataType.VALUE)
              .value("text b"),
            new MarcActionDataInner()
              .key(MarcDataType.SUBFIELD)
              .value("b")))));
    var rules = new BulkOperationMarcRuleCollection()
      .bulkOperationMarcRules(Collections.singletonList(findAndAppendRule))
      .totalRecords(1);

    processor.update(operation, marcRecord, rules);

    var dataFields = marcRecord.getDataFields();
    assertThat(dataFields).hasSize(5);

    var subfields = dataFields.get(0).getSubfields();
    assertThat(subfields).hasSize(2);
    assertThat(subfields.get(0).getCode()).isEqualTo('a');
    assertThat(subfields.get(0).getData()).isEqualTo("text a");
    assertThat(subfields.get(1).getCode()).isEqualTo('b');
    assertThat(subfields.get(1).getData()).isEqualTo("text b");
    subfields = dataFields.get(1).getSubfields();
    assertThat(subfields).hasSize(1);
    subfields = dataFields.get(2).getSubfields();
    assertThat(subfields).hasSize(1);
    subfields = dataFields.get(3).getSubfields();
    assertThat(subfields).hasSize(1);
    subfields = dataFields.get(4).getSubfields();
    assertThat(subfields).hasSize(1);
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
      .id(UUID.randomUUID())
      .bulkOperationId(bulkOperationId)
      .tag("500")
      .ind1("1")
      .ind2("2")
      .subfield("a")
      .actions(List.of(
        new MarcAction()
          .name(UpdateActionType.FIND)
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
    FIND                  | MARK_AS_STAFF_ONLY
    FIND_AND_REMOVE_THESE | FIND_AND_REMOVE_THESE
    """, delimiter = '|')
  void shouldNotApplyUnsupportedAction(UpdateActionType updateActionType, String errorMessageArg) {
    var bulkOperationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(bulkOperationId)
      .identifierType(IdentifierType.HRID)
      .build();
    var hrid = "inst000001";
    var controlNumberField = new ControlFieldImpl("001", hrid);
    var marcRecord = new RecordImpl();
    marcRecord.addVariableField(controlNumberField);
    var findAndAppendRule = new BulkOperationMarcRule()
      .id(UUID.randomUUID())
      .bulkOperationId(bulkOperationId)
      .tag("500")
      .ind1("1")
      .ind2("2")
      .subfield("a")
      .actions(List.of(
        new MarcAction()
          .name(updateActionType),
        new MarcAction()
          .name(UpdateActionType.MARK_AS_STAFF_ONLY)));
    var rules = new BulkOperationMarcRuleCollection()
      .bulkOperationMarcRules(Collections.singletonList(findAndAppendRule))
      .totalRecords(1);

    processor.update(operation, marcRecord, rules);

    var errorMessage = String.format("Action %s is not supported yet.", errorMessageArg);
    verify(errorService).saveError(bulkOperationId, hrid, errorMessage);
  }
}
