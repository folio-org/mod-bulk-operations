package org.folio.bulkops.processor.marc;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.dto.UpdateActionType.ADDITIONAL_SUBFIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.ADD_TO_EXISTING;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND;
import static org.folio.bulkops.util.Constants.DATE_TIME_CONTROL_FIELD;
import static org.folio.bulkops.util.Constants.HYPHEN;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.MarcAction;
import org.folio.bulkops.domain.dto.MarcActionDataInner;
import org.folio.bulkops.domain.dto.MarcDataType;
import org.folio.bulkops.domain.dto.MarcSubfieldAction;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.service.SubjectReferenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.marc4j.marc.impl.ControlFieldImpl;
import org.marc4j.marc.impl.DataFieldImpl;
import org.marc4j.marc.impl.LeaderImpl;
import org.marc4j.marc.impl.RecordImpl;
import org.marc4j.marc.impl.SubfieldImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class MarcInstanceDataProcessorTest extends BaseTest {
  @MockitoBean private ErrorService errorService;
  @MockitoBean private SubjectReferenceService subjectReferenceService;
  @Autowired private MarcInstanceDataProcessor processor;

  @Test
  @SneakyThrows
  void shouldApplyFindAndAppendRule() {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));

    var controlField = new ControlFieldImpl(DATE_TIME_CONTROL_FIELD, "20240101100202.4");
    marcRecord.addVariableField(controlField);

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
    var bulkOperationId = UUID.randomUUID();
    var findAndAppendRule =
        new BulkOperationMarcRule()
            .bulkOperationId(bulkOperationId)
            .tag("500")
            .ind1("1")
            .ind2("\\")
            .subfield("b")
            .actions(
                List.of(
                    new MarcAction()
                        .name(FIND)
                        .data(
                            Collections.singletonList(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value("text"))),
                    new MarcAction()
                        .name(UpdateActionType.APPEND)
                        .data(
                            List.of(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value("text a"),
                                new MarcActionDataInner().key(MarcDataType.SUBFIELD).value("a")))));
    var rules =
        new BulkOperationMarcRuleCollection()
            .bulkOperationMarcRules(Collections.singletonList(findAndAppendRule))
            .totalRecords(1);
    var pattern = "yyyy/MM/dd HH:mm:ss.SSS";
    var simpleDateFormat = new SimpleDateFormat(pattern);
    var date = simpleDateFormat.parse("2024/01/01 11:12:12.454");
    var operation =
        BulkOperation.builder().id(bulkOperationId).identifierType(IdentifierType.ID).build();
    processor.update(operation, marcRecord, rules, date);

    var dateTimeControlFieldOpt =
        marcRecord.getControlFields().stream()
            .filter(f -> DATE_TIME_CONTROL_FIELD.equals(f.getTag()))
            .findFirst();
    assertTrue(dateTimeControlFieldOpt.isPresent());
    var marcUpdateDateTime = dateTimeControlFieldOpt.get().getData();
    assertThat(marcUpdateDateTime).isEqualTo("20240101111212.4");

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
  @SneakyThrows
  void shouldApplyFindAndRemoveField() {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));

    var controlField = new ControlFieldImpl(DATE_TIME_CONTROL_FIELD, "20240101100202.4");
    marcRecord.addVariableField(controlField);

    var dataField = new DataFieldImpl("500", '1', '1');
    dataField.addSubfield(new SubfieldImpl('a', "text a"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("500", '1', '1');
    dataField.addSubfield(new SubfieldImpl('a', "Text a"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("510", '1', '1');
    dataField.addSubfield(new SubfieldImpl('a', "text a"));
    marcRecord.addVariableField(dataField);
    var pattern = "yyyy/MM/dd HH:mm:ss.SSS";
    var simpleDateFormat = new SimpleDateFormat(pattern);
    var date = simpleDateFormat.parse("2024/01/01 11:12:12.454");
    var bulkOperationId = UUID.randomUUID();
    var operation =
        BulkOperation.builder().id(bulkOperationId).identifierType(IdentifierType.ID).build();
    var findAndAppendRule =
        new BulkOperationMarcRule()
            .bulkOperationId(bulkOperationId)
            .tag("500")
            .ind1("1")
            .ind2("1")
            .subfield("a")
            .actions(
                List.of(
                    new MarcAction()
                        .name(FIND)
                        .data(
                            Collections.singletonList(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value("text"))),
                    new MarcAction()
                        .name(UpdateActionType.REMOVE_FIELD)
                        .data(Collections.emptyList())));
    var rules =
        new BulkOperationMarcRuleCollection()
            .bulkOperationMarcRules(Collections.singletonList(findAndAppendRule))
            .totalRecords(1);

    processor.update(operation, marcRecord, rules, date);

    var dateTimeControlFieldOpt =
        marcRecord.getControlFields().stream()
            .filter(f -> DATE_TIME_CONTROL_FIELD.equals(f.getTag()))
            .findFirst();
    assertTrue(dateTimeControlFieldOpt.isPresent());
    var marcUpdateDateTime = dateTimeControlFieldOpt.get().getData();
    assertThat(marcUpdateDateTime).isEqualTo("20240101111212.4");

    var dataFields = marcRecord.getDataFields();
    assertThat(dataFields).hasSize(2);

    assertThat(dataFields.get(0)).hasToString("500 11$aText a");
    assertThat(dataFields.get(1)).hasToString("510 11$atext a");
  }

  @Test
  @SneakyThrows
  void shouldApplyFindAndRemoveSubfield() {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));

    var controlField = new ControlFieldImpl(DATE_TIME_CONTROL_FIELD, "20240101100202.4");
    marcRecord.addVariableField(controlField);

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
    var pattern = "yyyy/MM/dd HH:mm:ss.SSS";
    var simpleDateFormat = new SimpleDateFormat(pattern);
    var date = simpleDateFormat.parse("2024/01/01 11:12:12.454");
    var bulkOperationId = UUID.randomUUID();
    var operation =
        BulkOperation.builder().id(bulkOperationId).identifierType(IdentifierType.ID).build();
    var findAndAppendRule =
        new BulkOperationMarcRule()
            .bulkOperationId(bulkOperationId)
            .tag("500")
            .ind1("1")
            .ind2("1")
            .subfield("a")
            .actions(
                List.of(
                    new MarcAction()
                        .name(FIND)
                        .data(
                            Collections.singletonList(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value("text"))),
                    new MarcAction()
                        .name(UpdateActionType.REMOVE_SUBFIELD)
                        .data(Collections.emptyList())));
    var rules =
        new BulkOperationMarcRuleCollection()
            .bulkOperationMarcRules(Collections.singletonList(findAndAppendRule))
            .totalRecords(1);

    processor.update(operation, marcRecord, rules, date);

    var dateTimeControlFieldOpt =
        marcRecord.getControlFields().stream()
            .filter(f -> DATE_TIME_CONTROL_FIELD.equals(f.getTag()))
            .findFirst();
    assertTrue(dateTimeControlFieldOpt.isPresent());
    var marcUpdateDateTime = dateTimeControlFieldOpt.get().getData();
    assertThat(marcUpdateDateTime).isEqualTo("20240101111212.4");

    var dataFields = marcRecord.getDataFields();
    assertThat(dataFields).hasSize(4);

    assertThat(dataFields.get(0)).hasToString("500 11$btext b");
    assertThat(dataFields.get(1)).hasToString("500 11$aText a$bText b");
    assertThat(dataFields.get(2)).hasToString("510 11$atext a$btext b");
    assertThat(dataFields.get(3)).hasToString("500 11$btext b");
  }

  @Test
  @SneakyThrows
  void shouldApplyFindAndReplaceRule() {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));

    var controlField = new ControlFieldImpl(DATE_TIME_CONTROL_FIELD, "20240101100202.4");
    marcRecord.addVariableField(controlField);

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
    var pattern = "yyyy/MM/dd HH:mm:ss.SSS";
    var simpleDateFormat = new SimpleDateFormat(pattern);
    var date = simpleDateFormat.parse("2024/01/01 11:12:12.454");
    var bulkOperationId = UUID.randomUUID();
    var operation =
        BulkOperation.builder().id(bulkOperationId).identifierType(IdentifierType.ID).build();
    var findAndReplaceRule =
        new BulkOperationMarcRule()
            .bulkOperationId(bulkOperationId)
            .tag("500")
            .ind1("1")
            .ind2("\\")
            .subfield("a")
            .actions(
                List.of(
                    new MarcAction()
                        .name(FIND)
                        .data(
                            Collections.singletonList(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value("old"))),
                    new MarcAction()
                        .name(UpdateActionType.REPLACE_WITH)
                        .data(
                            List.of(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value("new")))));
    var rules =
        new BulkOperationMarcRuleCollection()
            .bulkOperationMarcRules(Collections.singletonList(findAndReplaceRule))
            .totalRecords(1);

    processor.update(operation, marcRecord, rules, date);

    var dateTimeControlFieldOpt =
        marcRecord.getControlFields().stream()
            .filter(f -> DATE_TIME_CONTROL_FIELD.equals(f.getTag()))
            .findFirst();
    assertTrue(dateTimeControlFieldOpt.isPresent());
    var marcUpdateDateTime = dateTimeControlFieldOpt.get().getData();
    assertThat(marcUpdateDateTime).isEqualTo("20240101111212.4");

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
  @SneakyThrows
  void shouldApplyRemoveAllRule() {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));

    var controlField = new ControlFieldImpl(DATE_TIME_CONTROL_FIELD, "20240101100202.4");
    marcRecord.addVariableField(controlField);

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
    var bulkOperationId = UUID.randomUUID();
    var operation =
        BulkOperation.builder().id(bulkOperationId).identifierType(IdentifierType.ID).build();
    var findAndAppendRule =
        new BulkOperationMarcRule()
            .bulkOperationId(bulkOperationId)
            .tag("500")
            .ind1("1")
            .ind2("\\")
            .subfield("a")
            .actions(
                List.of(
                    new MarcAction()
                        .name(UpdateActionType.REMOVE_ALL)
                        .data(
                            Collections.singletonList(
                                new MarcActionDataInner()
                                    .key(MarcDataType.VALUE)
                                    .value("text a")))));
    var rules =
        new BulkOperationMarcRuleCollection()
            .bulkOperationMarcRules(Collections.singletonList(findAndAppendRule))
            .totalRecords(1);
    var pattern = "yyyy/MM/dd HH:mm:ss.SSS";
    var simpleDateFormat = new SimpleDateFormat(pattern);
    var date = simpleDateFormat.parse("2024/01/01 11:12:12.454");
    processor.update(operation, marcRecord, rules, date);

    var dateTimeControlFieldOpt =
        marcRecord.getControlFields().stream()
            .filter(f -> DATE_TIME_CONTROL_FIELD.equals(f.getTag()))
            .findFirst();
    assertTrue(dateTimeControlFieldOpt.isPresent());
    var marcUpdateDateTime = dateTimeControlFieldOpt.get().getData();
    assertThat(marcUpdateDateTime).isEqualTo("20240101111212.4");

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
  @CsvSource(
      textBlock =
          """
          null   | text b | b    | Action data VALUE is absent.    | ID   | true
          text a | null   | b    | Action data VALUE is absent.    | ID   | false
          text a | text b | null | Action data SUBFIELD is absent. | HRID | false
          """,
      delimiter = '|',
      nullValues = "null")
  void shouldSaveErrorsOnBadRule(
      String findValue,
      String appendValue,
      String subfieldValue,
      String errorMessage,
      IdentifierType identifierType,
      boolean isInstanceId) {
    var dataField500 = new DataFieldImpl("500", '1', '2');
    dataField500.addSubfield(new SubfieldImpl('a', "text a"));
    var instanceId = UUID.randomUUID().toString();
    var dataField999 = new DataFieldImpl("999", 'f', 'f');
    dataField999.addSubfield(new SubfieldImpl('i', instanceId));
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));
    var hrid = "inst000001";
    var controlNumberField = new ControlFieldImpl("001", hrid);
    marcRecord.addVariableField(controlNumberField);
    marcRecord.addVariableField(dataField500);
    if (isInstanceId) {
      marcRecord.addVariableField(dataField999);
    }
    var bulkOperationId = UUID.randomUUID();
    var findAndAppendRule =
        new BulkOperationMarcRule()
            .bulkOperationId(bulkOperationId)
            .tag("500")
            .ind1("1")
            .ind2("2")
            .subfield("a")
            .actions(
                List.of(
                    new MarcAction()
                        .name(FIND)
                        .data(
                            Collections.singletonList(
                                new MarcActionDataInner()
                                    .key(MarcDataType.VALUE)
                                    .value(findValue))),
                    new MarcAction()
                        .name(UpdateActionType.APPEND)
                        .data(
                            List.of(
                                new MarcActionDataInner()
                                    .key(MarcDataType.VALUE)
                                    .value(appendValue),
                                new MarcActionDataInner()
                                    .key(MarcDataType.SUBFIELD)
                                    .value(subfieldValue)))));
    var rules =
        new BulkOperationMarcRuleCollection()
            .bulkOperationMarcRules(Collections.singletonList(findAndAppendRule))
            .totalRecords(1);
    var operation =
        BulkOperation.builder().id(bulkOperationId).identifierType(identifierType).build();
    processor.update(operation, marcRecord, rules, new Date());

    var identifier = isInstanceId ? instanceId : hrid;
    verify(errorService).saveError(bulkOperationId, identifier, errorMessage, ErrorType.ERROR);
  }

  @ParameterizedTest
  @CsvSource(
      textBlock =
          """
          FIND                  | MARK_AS_STAFF_ONLY    | MARK_AS_STAFF_ONLY
          FIND                  | null                  | FIND
          FIND_AND_REMOVE_THESE | null                  | FIND_AND_REMOVE_THESE
          """,
      delimiter = '|',
      nullValues = "null")
  void shouldNotApplyUnsupportedAction(
      UpdateActionType updateActionType1,
      UpdateActionType updateActionType2,
      String errorMessageArg) {
    var hrid = "inst000001";
    var controlNumberField = new ControlFieldImpl("001", hrid);
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));
    marcRecord.addVariableField(controlNumberField);

    var actions = new ArrayList<MarcAction>();
    actions.add(new MarcAction().name(updateActionType1));
    if (updateActionType2 != null) {
      actions.add(new MarcAction().name(updateActionType2));
    }
    var bulkOperationId = UUID.randomUUID();
    var findAndAppendRule =
        new BulkOperationMarcRule()
            .bulkOperationId(bulkOperationId)
            .tag("500")
            .ind1("1")
            .ind2("2")
            .subfield("a")
            .actions(actions);
    var rules =
        new BulkOperationMarcRuleCollection()
            .bulkOperationMarcRules(Collections.singletonList(findAndAppendRule))
            .totalRecords(1);
    var operation =
        BulkOperation.builder().id(bulkOperationId).identifierType(IdentifierType.HRID).build();

    processor.update(operation, marcRecord, rules, new Date());

    var errorMessageTemplate =
        FIND.equals(updateActionType1) && nonNull(updateActionType2)
            ? "Action FIND + %s is not supported yet."
            : "Action %s is not supported yet.";
    var errorMessage = String.format(errorMessageTemplate, errorMessageArg);
    verify(errorService).saveError(bulkOperationId, hrid, errorMessage, ErrorType.ERROR);
  }

  @Test
  @SneakyThrows
  void shouldApplyAddToExistingRule() {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));

    var controlField = new ControlFieldImpl(DATE_TIME_CONTROL_FIELD, "20240101100202.4");
    marcRecord.addVariableField(controlField);

    var dataField = new DataFieldImpl("500", '1', '1');
    dataField.addSubfield(new SubfieldImpl('a', "text a"));
    marcRecord.addVariableField(dataField);
    dataField = new DataFieldImpl("550", '1', '1');
    dataField.addSubfield(new SubfieldImpl('a', "text a"));
    marcRecord.addVariableField(dataField);
    var bulkOperationId = UUID.randomUUID();
    var findAndAppendRule =
        new BulkOperationMarcRule()
            .bulkOperationId(bulkOperationId)
            .tag("510")
            .ind1("1")
            .ind2("1")
            .subfield("b")
            .actions(
                List.of(
                    new MarcAction()
                        .name(ADD_TO_EXISTING)
                        .data(
                            Collections.singletonList(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value("text b"))),
                    new MarcAction().name(ADDITIONAL_SUBFIELD).data(Collections.emptyList())))
            .subfields(
                List.of(
                    new MarcSubfieldAction()
                        .subfield("1")
                        .actions(
                            List.of(
                                new MarcAction()
                                    .name(ADD_TO_EXISTING)
                                    .data(
                                        Collections.singletonList(
                                            new MarcActionDataInner()
                                                .key(MarcDataType.VALUE)
                                                .value("text 1"))),
                                new MarcAction()
                                    .name(ADDITIONAL_SUBFIELD)
                                    .data(Collections.emptyList()))),
                    new MarcSubfieldAction()
                        .subfield("a")
                        .actions(
                            List.of(
                                new MarcAction()
                                    .name(ADD_TO_EXISTING)
                                    .data(
                                        Collections.singletonList(
                                            new MarcActionDataInner()
                                                .key(MarcDataType.VALUE)
                                                .value("text a"))),
                                new MarcAction()
                                    .name(ADDITIONAL_SUBFIELD)
                                    .data(Collections.emptyList())))));
    var rules =
        new BulkOperationMarcRuleCollection()
            .bulkOperationMarcRules(Collections.singletonList(findAndAppendRule))
            .totalRecords(1);
    var pattern = "yyyy/MM/dd HH:mm:ss.SSS";
    var simpleDateFormat = new SimpleDateFormat(pattern);
    var date = simpleDateFormat.parse("2024/01/01 11:12:12.454");
    var operation =
        BulkOperation.builder().id(bulkOperationId).identifierType(IdentifierType.ID).build();
    processor.update(operation, marcRecord, rules, date);
    var dateTimeControlFieldOpt =
        marcRecord.getControlFields().stream()
            .filter(f -> DATE_TIME_CONTROL_FIELD.equals(f.getTag()))
            .findFirst();
    assertTrue(dateTimeControlFieldOpt.isPresent());
    var marcUpdateDateTime = dateTimeControlFieldOpt.get().getData();
    assertThat(marcUpdateDateTime).isEqualTo("20240101111212.4");

    var dataFields = marcRecord.getDataFields();
    assertThat(dataFields).hasSize(3);

    assertThat(dataFields.get(0).getTag()).isEqualTo("500");
    assertThat(dataFields.get(1).getTag()).isEqualTo("510");
    assertThat(dataFields.get(1)).hasToString("510 11$atext a$btext b$1text 1");
    assertThat(dataFields.get(2).getTag()).isEqualTo("550");
  }

  @Test
  @SneakyThrows
  void shouldNotChange005FieldIfRulesAreNotApplied() {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));

    var controlField = new ControlFieldImpl(DATE_TIME_CONTROL_FIELD, "20240101100202.4");
    marcRecord.addVariableField(controlField);

    var dataField = new DataFieldImpl("500", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('b', "text b"));
    marcRecord.addVariableField(dataField);
    var bulkOperationId = UUID.randomUUID();
    var findAndAppendRule =
        new BulkOperationMarcRule()
            .bulkOperationId(bulkOperationId)
            .tag("900")
            .ind1("1")
            .ind2("\\")
            .subfield("b")
            .actions(
                List.of(
                    new MarcAction()
                        .name(FIND)
                        .data(
                            Collections.singletonList(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value("text b"))),
                    new MarcAction()
                        .name(UpdateActionType.APPEND)
                        .data(
                            List.of(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value("text a"),
                                new MarcActionDataInner().key(MarcDataType.SUBFIELD).value("a")))));
    var rules =
        new BulkOperationMarcRuleCollection()
            .bulkOperationMarcRules(Collections.singletonList(findAndAppendRule))
            .totalRecords(1);
    var operation =
        BulkOperation.builder().id(bulkOperationId).identifierType(IdentifierType.ID).build();
    processor.update(operation, marcRecord, rules, new Date());

    var dateTimeControlFieldOpt =
        marcRecord.getControlFields().stream()
            .filter(f -> DATE_TIME_CONTROL_FIELD.equals(f.getTag()))
            .findFirst();
    assertTrue(dateTimeControlFieldOpt.isPresent());
    var marcUpdateDateTime = dateTimeControlFieldOpt.get().getData();
    assertThat(marcUpdateDateTime).isEqualTo("20240101100202.4");
  }

  @Test
  @SneakyThrows
  void shouldSaveErrorOnRuleValidationException() {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));

    var controlField = new ControlFieldImpl(DATE_TIME_CONTROL_FIELD, "20240101100202.4");
    marcRecord.addVariableField(controlField);

    var dataField = new DataFieldImpl("500", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('b', "text b"));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("999", 'f', 'f');
    var identifier = "identifier";
    dataField.addSubfield(new SubfieldImpl('i', identifier));
    marcRecord.addVariableField(dataField);
    var bulkOperationId = UUID.randomUUID();
    var findAndAppendRule =
        new BulkOperationMarcRule()
            .bulkOperationId(bulkOperationId)
            .tag("001")
            .ind1("1")
            .ind2("\\")
            .subfield("b")
            .actions(
                List.of(
                    new MarcAction()
                        .name(FIND)
                        .data(
                            Collections.singletonList(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value("text b"))),
                    new MarcAction()
                        .name(UpdateActionType.APPEND)
                        .data(
                            List.of(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value("text a"),
                                new MarcActionDataInner().key(MarcDataType.SUBFIELD).value("a")))));
    var rules =
        new BulkOperationMarcRuleCollection()
            .bulkOperationMarcRules(Collections.singletonList(findAndAppendRule))
            .totalRecords(1);
    var pattern = "yyyy/MM/dd HH:mm:ss.SSS";
    var simpleDateFormat = new SimpleDateFormat(pattern);
    var date = simpleDateFormat.parse("2024/01/01 11:12:12.454");
    var operation =
        BulkOperation.builder().id(bulkOperationId).identifierType(IdentifierType.ID).build();
    processor.update(operation, marcRecord, rules, date);

    var expectedErrorMessage = "Bulk edit of 001 field is not supported";
    verify(errorService)
        .saveError(bulkOperationId, identifier, expectedErrorMessage, ErrorType.ERROR);
  }

  @Test
  void shouldSetRecordStatusToDeletedWhenSetToTrue() throws Exception {
    var rule =
        new BulkOperationMarcRule()
            .updateOption(org.folio.bulkops.domain.dto.UpdateOptionType.SET_RECORDS_FOR_DELETE)
            .actions(List.of(new MarcAction().name(UpdateActionType.SET_TO_TRUE)));
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));
    var method =
        processor
            .getClass()
            .getDeclaredMethod(
                "applyRuleToRecord", BulkOperationMarcRule.class, org.marc4j.marc.Record.class);
    method.setAccessible(true);
    method.invoke(processor, rule, marcRecord);
    assertThat(marcRecord.getLeader().getRecordStatus()).isEqualTo('d');
  }

  @Test
  void shouldSetRecordStatusToCorrectedWhenSetToFalse() throws Exception {
    var rule =
        new BulkOperationMarcRule()
            .updateOption(org.folio.bulkops.domain.dto.UpdateOptionType.SET_RECORDS_FOR_DELETE)
            .actions(List.of(new MarcAction().name(UpdateActionType.SET_TO_FALSE)));
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));
    var method =
        processor
            .getClass()
            .getDeclaredMethod(
                "applyRuleToRecord", BulkOperationMarcRule.class, org.marc4j.marc.Record.class);
    method.setAccessible(true);
    method.invoke(processor, rule, marcRecord);
    assertThat(marcRecord.getLeader().getRecordStatus()).isEqualTo('c');
  }

  @Test
  void shouldThrowExceptionForUnsupportedSetRecordsForDeleteAction() throws Exception {
    var rule =
        new BulkOperationMarcRule()
            .updateOption(org.folio.bulkops.domain.dto.UpdateOptionType.SET_RECORDS_FOR_DELETE)
            .actions(List.of(new MarcAction().name(UpdateActionType.APPEND)));
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));
    var method =
        processor
            .getClass()
            .getDeclaredMethod(
                "applyRuleToRecord", BulkOperationMarcRule.class, org.marc4j.marc.Record.class);
    method.setAccessible(true);
    var exception =
        assertThrows(
            InvocationTargetException.class, () -> method.invoke(processor, rule, marcRecord));
    assertThat(exception.getCause()).isInstanceOf(BulkOperationException.class);
    assertThat(exception.getCause().getMessage())
        .contains("is not supported for SET_RECORDS_FOR_DELETE option.");
  }

  @Test
  void processAddToExisting_shouldHaveChangesInMrcIfSubjectTagAndSubfieldNotLetterOr2()
      throws Exception {
    var tag = "650";
    var ind1 = "1";
    var ind2 = "0";
    var subfield = "7"; // not a letter and not '2'
    var value = "test";
    var rule =
        new BulkOperationMarcRule()
            .tag(tag)
            .ind1(ind1)
            .ind2(ind2)
            .subfield(subfield)
            .actions(
                List.of(
                    new MarcAction()
                        .name(UpdateActionType.ADD_TO_EXISTING)
                        .data(
                            List.of(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value(value)))));
    var marcRecord = new RecordImpl();

    // Use reflection to call private method
    var method =
        processor
            .getClass()
            .getDeclaredMethod(
                "processAddToExisting", BulkOperationMarcRule.class, org.marc4j.marc.Record.class);
    method.setAccessible(true);
    method.invoke(processor, rule, marcRecord);

    // Should have changes
    assertThat(marcRecord.getDataFields().getFirst().toString()).isEqualTo("650 10$7test");
  }

  @Test
  void processAddToExisting_shouldHaveMarcInAreYouSureIfSubjectTagInd2is7Subfield2AndSrcNotExists()
      throws Exception {
    var tag = "650";
    var ind1 = "1";
    var ind2 = "7";
    var subfield = "2";
    var value = "not-exist";
    var rule =
        new BulkOperationMarcRule()
            .tag(tag)
            .ind1(ind1)
            .ind2(ind2)
            .subfield(subfield)
            .actions(
                List.of(
                    new MarcAction()
                        .name(UpdateActionType.ADD_TO_EXISTING)
                        .data(
                            List.of(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value(value)))));
    var marcRecord = new RecordImpl();

    when(subjectReferenceService.getSubjectSourceNameByCode(value)).thenReturn(HYPHEN);

    var method =
        processor
            .getClass()
            .getDeclaredMethod(
                "processAddToExisting", BulkOperationMarcRule.class, org.marc4j.marc.Record.class);
    method.setAccessible(true);
    method.invoke(processor, rule, marcRecord);

    assertThat(marcRecord.getDataFields().getFirst().getSubfields().getFirst().getData())
        .isEqualTo("not-exist");
  }

  @Test
  void processAddToExisting_shouldAddFieldIfSubjectTagAndSubfieldIsLetter() throws Exception {
    var tag = "650";
    var ind1 = "1";
    var ind2 = "0";
    var subfield = "a";
    var value = "subject";
    var rule =
        new BulkOperationMarcRule()
            .tag(tag)
            .ind1(ind1)
            .ind2(ind2)
            .subfield(subfield)
            .actions(
                List.of(
                    new MarcAction()
                        .name(UpdateActionType.ADD_TO_EXISTING)
                        .data(
                            List.of(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value(value)))));
    var marcRecord = new RecordImpl();

    var method =
        processor
            .getClass()
            .getDeclaredMethod(
                "processAddToExisting", BulkOperationMarcRule.class, org.marc4j.marc.Record.class);
    method.setAccessible(true);
    method.invoke(processor, rule, marcRecord);

    assertThat(marcRecord.getDataFields()).hasSize(1);
    var df = marcRecord.getDataFields().getFirst();
    assertThat(df.getTag()).isEqualTo(tag);
    assertThat(df.getSubfields().getFirst().getCode()).isEqualTo('a');
    assertThat(df.getSubfields().getFirst().getData()).isEqualTo(value);
  }

  @Test
  void processAddToExisting_shouldAddFieldIfNotSubjectTag() throws Exception {
    var tag = "500";
    var ind1 = "1";
    var ind2 = "1";
    var subfield = "a";
    var value = "notSubject";
    var rule =
        new BulkOperationMarcRule()
            .tag(tag)
            .ind1(ind1)
            .ind2(ind2)
            .subfield(subfield)
            .actions(
                List.of(
                    new MarcAction()
                        .name(UpdateActionType.ADD_TO_EXISTING)
                        .data(
                            List.of(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value(value)))));
    var marcRecord = new RecordImpl();

    var method =
        processor
            .getClass()
            .getDeclaredMethod(
                "processAddToExisting", BulkOperationMarcRule.class, org.marc4j.marc.Record.class);
    method.setAccessible(true);
    method.invoke(processor, rule, marcRecord);

    assertThat(marcRecord.getDataFields()).hasSize(1);
    var df = marcRecord.getDataFields().getFirst();
    assertThat(df.getTag()).isEqualTo(tag);
    assertThat(df.getSubfields().getFirst().getCode()).isEqualTo('a');
    assertThat(df.getSubfields().getFirst().getData()).isEqualTo(value);
  }

  @Test
  @SneakyThrows
  void shouldRemoveSubfieldWhenFindAndReplaceResultsInEmptyValue() {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));

    var controlField = new ControlFieldImpl(DATE_TIME_CONTROL_FIELD, "20240101100202.4");
    marcRecord.addVariableField(controlField);

    // Field with two subfields - only one will be emptied
    var dataField = new DataFieldImpl("500", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "test"));
    dataField.addSubfield(new SubfieldImpl('b', "keep this"));
    marcRecord.addVariableField(dataField);

    // Field with one subfield that will be emptied - entire field should be removed
    dataField = new DataFieldImpl("500", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "test"));
    marcRecord.addVariableField(dataField);

    // Field that won't match
    dataField = new DataFieldImpl("500", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "different"));
    marcRecord.addVariableField(dataField);

    var pattern = "yyyy/MM/dd HH:mm:ss.SSS";
    var simpleDateFormat = new SimpleDateFormat(pattern);
    var date = simpleDateFormat.parse("2024/01/01 11:12:12.454");
    var bulkOperationId = UUID.randomUUID();
    var operation =
        BulkOperation.builder().id(bulkOperationId).identifierType(IdentifierType.ID).build();
    var findAndReplaceRule =
        new BulkOperationMarcRule()
            .bulkOperationId(bulkOperationId)
            .tag("500")
            .ind1("1")
            .ind2("\\")
            .subfield("a")
            .actions(
                List.of(
                    new MarcAction()
                        .name(FIND)
                        .data(
                            Collections.singletonList(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value("test"))),
                    new MarcAction()
                        .name(UpdateActionType.REPLACE_WITH)
                        .data(
                            List.of(new MarcActionDataInner().key(MarcDataType.VALUE).value("")))));
    var rules =
        new BulkOperationMarcRuleCollection()
            .bulkOperationMarcRules(Collections.singletonList(findAndReplaceRule))
            .totalRecords(1);

    processor.update(operation, marcRecord, rules, date);

    var dataFields = marcRecord.getDataFields();
    // Should have 2 fields: one with only subfield b, and one with "different"
    assertThat(dataFields).hasSize(2);

    // First field should only have subfield 'b'
    assertThat(dataFields.get(0)).hasToString("500 1 $bkeep this");

    // Second field should be unchanged
    assertThat(dataFields.get(1)).hasToString("500 1 $adifferent");
  }

  @Test
  @SneakyThrows
  void shouldRemoveFieldWhenFindAndRemoveSubfieldLeavesNoSubfields() {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));

    var controlField = new ControlFieldImpl(DATE_TIME_CONTROL_FIELD, "20240101100202.4");
    marcRecord.addVariableField(controlField);

    // Field with only one subfield that matches - entire field should be removed
    var dataField = new DataFieldImpl("500", '1', '1');
    dataField.addSubfield(new SubfieldImpl('a', "text a"));
    marcRecord.addVariableField(dataField);

    // Field with two subfields where only one matches - field should remain with one subfield
    dataField = new DataFieldImpl("500", '1', '1');
    dataField.addSubfield(new SubfieldImpl('a', "text a"));
    dataField.addSubfield(new SubfieldImpl('b', "text b"));
    marcRecord.addVariableField(dataField);

    // Field that doesn't match
    dataField = new DataFieldImpl("500", '1', '1');
    dataField.addSubfield(new SubfieldImpl('a', "different"));
    marcRecord.addVariableField(dataField);

    var pattern = "yyyy/MM/dd HH:mm:ss.SSS";
    var simpleDateFormat = new SimpleDateFormat(pattern);
    var date = simpleDateFormat.parse("2024/01/01 11:12:12.454");
    var bulkOperationId = UUID.randomUUID();
    var operation =
        BulkOperation.builder().id(bulkOperationId).identifierType(IdentifierType.ID).build();
    var findAndRemoveRule =
        new BulkOperationMarcRule()
            .bulkOperationId(bulkOperationId)
            .tag("500")
            .ind1("1")
            .ind2("1")
            .subfield("a")
            .actions(
                List.of(
                    new MarcAction()
                        .name(FIND)
                        .data(
                            Collections.singletonList(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value("text"))),
                    new MarcAction()
                        .name(UpdateActionType.REMOVE_SUBFIELD)
                        .data(Collections.emptyList())));
    var rules =
        new BulkOperationMarcRuleCollection()
            .bulkOperationMarcRules(Collections.singletonList(findAndRemoveRule))
            .totalRecords(1);

    processor.update(operation, marcRecord, rules, date);

    var dataFields = marcRecord.getDataFields();
    // Should have 2 fields remaining
    assertThat(dataFields).hasSize(2);

    // First field should only have subfield 'b' (subfield 'a' was removed)
    assertThat(dataFields.get(0)).hasToString("500 11$btext b");

    // Second field should be unchanged
    assertThat(dataFields.get(1)).hasToString("500 11$adifferent");
  }

  @Test
  @SneakyThrows
  void shouldHandlePartialReplacementCorrectly() {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));

    var controlField = new ControlFieldImpl(DATE_TIME_CONTROL_FIELD, "20240101100202.4");
    marcRecord.addVariableField(controlField);

    // Field where only part of the value will be replaced - subfield should remain
    var dataField = new DataFieldImpl("500", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "test value"));
    marcRecord.addVariableField(dataField);

    var pattern = "yyyy/MM/dd HH:mm:ss.SSS";
    var simpleDateFormat = new SimpleDateFormat(pattern);
    var date = simpleDateFormat.parse("2024/01/01 11:12:12.454");
    var bulkOperationId = UUID.randomUUID();
    var operation =
        BulkOperation.builder().id(bulkOperationId).identifierType(IdentifierType.ID).build();
    var findAndReplaceRule =
        new BulkOperationMarcRule()
            .bulkOperationId(bulkOperationId)
            .tag("500")
            .ind1("1")
            .ind2("\\")
            .subfield("a")
            .actions(
                List.of(
                    new MarcAction()
                        .name(FIND)
                        .data(
                            Collections.singletonList(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value("test"))),
                    new MarcAction()
                        .name(UpdateActionType.REPLACE_WITH)
                        .data(
                            List.of(
                                new MarcActionDataInner().key(MarcDataType.VALUE).value("new")))));
    var rules =
        new BulkOperationMarcRuleCollection()
            .bulkOperationMarcRules(Collections.singletonList(findAndReplaceRule))
            .totalRecords(1);

    processor.update(operation, marcRecord, rules, date);

    var dataFields = marcRecord.getDataFields();
    // Field should remain with updated value
    assertThat(dataFields).hasSize(1);
    assertThat(dataFields.get(0)).hasToString("500 1 $anew value");
  }
}
