package org.folio.bulkops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_CLASSIFICATION;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_ELECTRONIC_ACCESS;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_PUBLICATION;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_SUBJECT;
import static org.folio.bulkops.util.Constants.DATE_TIME_CONTROL_FIELD;
import static org.folio.bulkops.util.Constants.NON_PRINTING_DELIMITER;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.SubjectSource;
import org.folio.bulkops.domain.bean.SubjectSourceCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.marc4j.marc.impl.ControlFieldImpl;
import org.marc4j.marc.impl.DataFieldImpl;
import org.marc4j.marc.impl.LeaderImpl;
import org.marc4j.marc.impl.RecordImpl;
import org.marc4j.marc.impl.SubfieldImpl;
import org.springframework.beans.factory.annotation.Autowired;

class MarcToUnifiedTableRowMapperTest extends BaseTest {
  @Autowired
  private MarcToUnifiedTableRowMapper marcToUnifiedTableRowMapper;

  @Test
  void processRecordWithElectronicAccessTest() {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));

    var controlField = new ControlFieldImpl(DATE_TIME_CONTROL_FIELD, "20240101100202.4");
    marcRecord.addVariableField(controlField);

    var dataField = new DataFieldImpl("856", '4', '2');
    dataField.addSubfield(new SubfieldImpl('u', "url"));
    dataField.addSubfield(new SubfieldImpl('y', "text"));
    dataField.addSubfield(new SubfieldImpl('3', "material"));
    dataField.addSubfield(new SubfieldImpl('z', "public note"));
    marcRecord.addVariableField(dataField);
    var rowData = marcToUnifiedTableRowMapper.processRecord(marcRecord,
            List.of(INSTANCE_ELECTRONIC_ACCESS), true);

    assertThat(rowData.getFirst()).isEqualTo(
      "URL relationship;URI;Link text;Material specified;URL public note\n" +
      "Related resource;url;text;material;public note");
  }

  @Test
  void processRecordWithElectronicAccessAndRepeatableFieldsTest() {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));

    var controlField = new ControlFieldImpl(DATE_TIME_CONTROL_FIELD, "20240101100202.4");
    marcRecord.addVariableField(controlField);

    var dataField = new DataFieldImpl("856", '4', '2');
    dataField.addSubfield(new SubfieldImpl('u', "url"));
    dataField.addSubfield(new SubfieldImpl('y', "text"));
    dataField.addSubfield(new SubfieldImpl('y', "text2"));
    dataField.addSubfield(new SubfieldImpl('3', "material"));
    dataField.addSubfield(new SubfieldImpl('z', "public note"));
    dataField.addSubfield(new SubfieldImpl('z', "public note2"));
    marcRecord.addVariableField(dataField);
    var rowData = marcToUnifiedTableRowMapper.processRecord(marcRecord,
            List.of(INSTANCE_ELECTRONIC_ACCESS), true);

    assertThat(rowData.getFirst()).isEqualTo(
      "URL relationship;URI;Link text;Material specified;URL public note\n" +
      "Related resource;url;text text2;material;public note public note2");

    rowData = marcToUnifiedTableRowMapper.processRecord(marcRecord,
            List.of(INSTANCE_ELECTRONIC_ACCESS), false);

    assertThat(rowData.getFirst()).isEqualTo(
      "Related resource%s;url%s;text text2%s;material%s;public note public note2"
      .formatted(NON_PRINTING_DELIMITER, NON_PRINTING_DELIMITER, 
                 NON_PRINTING_DELIMITER, NON_PRINTING_DELIMITER));
  }

  @Test
  void processRecordWithElectronicAccessIfSubfieldIsEmptyTest() {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));

    var controlField = new ControlFieldImpl(DATE_TIME_CONTROL_FIELD, "20240101100202.4");
    marcRecord.addVariableField(controlField);

    var dataField = new DataFieldImpl("856", '4', '2');
    dataField.addSubfield(new SubfieldImpl('u', ""));
    dataField.addSubfield(new SubfieldImpl('y', "text"));
    dataField.addSubfield(new SubfieldImpl('3', "material"));
    dataField.addSubfield(new SubfieldImpl('z', "public note"));
    marcRecord.addVariableField(dataField);
    var rowData = marcToUnifiedTableRowMapper.processRecord(marcRecord,
            List.of(INSTANCE_ELECTRONIC_ACCESS), true);

    assertThat(rowData.getFirst()).isEqualTo((
      "URL relationship;URI;Link text;Material specified;URL public note\n" +
      "Related resource;-;text;material;public note"));

    rowData = marcToUnifiedTableRowMapper.processRecord(marcRecord,
            List.of(INSTANCE_ELECTRONIC_ACCESS), false);

    assertThat(rowData.getFirst()).isEqualTo((
      "Related resource\u001f;-\u001f;text\u001f;material\u001f;public note"));
  }

  @Test
  void processRecordWithSubjectIfSubfieldIsEmptyTest() {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));

    var controlField = new ControlFieldImpl(DATE_TIME_CONTROL_FIELD, "20240101100202.4");
    marcRecord.addVariableField(controlField);

    var dataField = new DataFieldImpl("600", '1', '2');
    dataField.addSubfield(new SubfieldImpl('a', ""));
    dataField.addSubfield(new SubfieldImpl('b', "text"));
    dataField.addSubfield(new SubfieldImpl('c', "subject c"));
    dataField.addSubfield(new SubfieldImpl('d', "subject d"));
    marcRecord.addVariableField(dataField);
    var rowData = marcToUnifiedTableRowMapper.processRecord(marcRecord, List.of(INSTANCE_SUBJECT),
            true);

    assertThat(rowData.getFirst()).isEqualTo(("Subject headings;Subject source;Subject type\n"
            + "text subject c subject d;Medical Subject Headings;Personal name"));

    rowData = marcToUnifiedTableRowMapper.processRecord(marcRecord, List.of(INSTANCE_SUBJECT),
            false);

    assertThat(rowData.getFirst()).isEqualTo((
            "text subject c subject d\u001f;Medical Subject Headings\u001f;Personal name"));
  }

  @Test
  void processRecordWithSubjectAndAllTypesTest() {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));

    var controlField = new ControlFieldImpl(DATE_TIME_CONTROL_FIELD, "20240101100202.4");
    marcRecord.addVariableField(controlField);

    var dataField = new DataFieldImpl("600", '1', '2');
    dataField.addSubfield(new SubfieldImpl('a', "a 600"));
    dataField.addSubfield(new SubfieldImpl('b', "b text"));
    dataField.addSubfield(new SubfieldImpl('c', "subject c"));
    dataField.addSubfield(new SubfieldImpl('d', "subject d"));
    dataField.addSubfield(new SubfieldImpl('d', ""));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("610", '1', '3');
    dataField.addSubfield(new SubfieldImpl('a', ""));
    dataField.addSubfield(new SubfieldImpl('b', "text"));
    dataField.addSubfield(new SubfieldImpl('c', "subject c"));
    dataField.addSubfield(new SubfieldImpl('c', "subject c 2"));
    dataField.addSubfield(new SubfieldImpl('d', "subject d"));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("611", '3', '2');
    dataField.addSubfield(new SubfieldImpl('a', "611 a"));
    dataField.addSubfield(new SubfieldImpl('b', "text"));
    dataField.addSubfield(new SubfieldImpl('c', "subject c"));
    dataField.addSubfield(new SubfieldImpl('d', "subject d"));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("630", '1', '5');
    dataField.addSubfield(new SubfieldImpl('a', ""));
    dataField.addSubfield(new SubfieldImpl('b', "text"));
    dataField.addSubfield(new SubfieldImpl('c', "subject c"));
    dataField.addSubfield(new SubfieldImpl('d', "subject d"));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("647", '1', '2');
    dataField.addSubfield(new SubfieldImpl('a', ""));
    dataField.addSubfield(new SubfieldImpl('b', "text"));
    dataField.addSubfield(new SubfieldImpl('c', "subject c"));
    dataField.addSubfield(new SubfieldImpl('d', "subject d"));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("648", '2', '7');
    dataField.addSubfield(new SubfieldImpl('a', ""));
    dataField.addSubfield(new SubfieldImpl('2', "text"));
    dataField.addSubfield(new SubfieldImpl('c', "subject c"));
    dataField.addSubfield(new SubfieldImpl('d', "subject d"));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("650", '1', '9');
    dataField.addSubfield(new SubfieldImpl('a', ""));
    dataField.addSubfield(new SubfieldImpl('b', "text"));
    dataField.addSubfield(new SubfieldImpl('c', "subject c"));
    dataField.addSubfield(new SubfieldImpl('d', "subject d"));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("651", ' ', '2');
    dataField.addSubfield(new SubfieldImpl('a', ""));
    dataField.addSubfield(new SubfieldImpl('b', "text"));
    dataField.addSubfield(new SubfieldImpl('c', "subject c"));
    dataField.addSubfield(new SubfieldImpl('d', "subject d"));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("651", '0', '1');
    dataField.addSubfield(new SubfieldImpl('a', "a"));
    dataField.addSubfield(new SubfieldImpl('b', "text"));
    dataField.addSubfield(new SubfieldImpl('c', "subject c"));
    dataField.addSubfield(new SubfieldImpl('d', "subject d"));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("655", '1', ' ');
    dataField.addSubfield(new SubfieldImpl('a', ""));
    dataField.addSubfield(new SubfieldImpl('b', "text"));
    dataField.addSubfield(new SubfieldImpl('c', "subject c"));
    dataField.addSubfield(new SubfieldImpl('d', "subject d"));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("648", '3', '7');
    dataField.addSubfield(new SubfieldImpl('a', ""));
    dataField.addSubfield(new SubfieldImpl('2', "codeFound"));
    dataField.addSubfield(new SubfieldImpl('c', "subject c"));
    dataField.addSubfield(new SubfieldImpl('d', "subject d"));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("648", '3', '7');
    dataField.addSubfield(new SubfieldImpl('1', "subject 1"));
    dataField.addSubfield(new SubfieldImpl('2', "subject 2"));
    dataField.addSubfield(new SubfieldImpl('c', "subject c"));
    dataField.addSubfield(new SubfieldImpl('d', "subject d"));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("650", '/', '7');
    dataField.addSubfield(new SubfieldImpl('a', "text1"));
    dataField.addSubfield(new SubfieldImpl('2', "not found"));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("650", '/', '7');
    dataField.addSubfield(new SubfieldImpl('a', "text2"));
    dataField.addSubfield(new SubfieldImpl('2', "found"));
    marcRecord.addVariableField(dataField);

    when(subjectSourcesClient.getByQuery("code==codeFound"))
            .thenReturn(new SubjectSourceCollection()
            .withSubjectSources(List.of(new SubjectSource()
                    .withId(UUID.randomUUID().toString()).withName("source1"))));
    when(subjectSourcesClient.getByQuery("code==found")).thenReturn(new SubjectSourceCollection()
            .withSubjectSources(List.of(new SubjectSource()
                    .withId(UUID.randomUUID().toString()).withName("found"))));
    when(subjectSourcesClient.getByQuery("code==not found"))
            .thenReturn(new SubjectSourceCollection().withSubjectSources(List.of()));
    when(subjectSourcesClient.getByQuery("code==text"))
            .thenReturn(new SubjectSourceCollection().withSubjectSources(List.of()));
    when(subjectSourcesClient.getByQuery("code==subject 2"))
            .thenReturn(new SubjectSourceCollection().withSubjectSources(List.of()));

    var rowData = marcToUnifiedTableRowMapper.processRecord(marcRecord, List.of(INSTANCE_SUBJECT),
            true);

    var expectedRowData = "Subject headings;Subject source;Subject type\n"
            + "a 600 b text subject c subject d;Medical Subject Headings;Personal name | "
            + "text subject c subject c 2 subject d;National Agriculture Library subject "
            + "authority file;Corporate name | "
            + "611 a text subject c subject d;Medical Subject Headings;Meeting name | "
            + "text subject c subject d;Canadian Subject Headings;Uniform title | "
            + "text subject c subject d;Medical Subject Headings;Named event | "
            + "subject c subject d;-;Chronological term | "
            + "text subject c subject d;-;Topical term | "
            + "text subject c subject d;Medical Subject Headings;Geographic name | "
            + "a text subject c subject d;Library of Congress Children’s and Young Adults' "
            + "Subject Headings;Geographic name | "
            + "text subject c subject d;-;Genre/Form |"
            + " subject c subject d;source1;Chronological term |"
            + " subject c subject d;-;Chronological term |"
            + " text1;-;Topical term |"
            + " text2;found;Topical term";

    assertThat(rowData.getFirst()).isEqualTo(expectedRowData);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void processRecordWithClassificationTest(boolean forCsv) {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));

    var controlField = new ControlFieldImpl(DATE_TIME_CONTROL_FIELD, "20240101100202.4");
    marcRecord.addVariableField(controlField);

    var dataField = new DataFieldImpl("050", ' ', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "a 050"));
    dataField.addSubfield(new SubfieldImpl('b', "b 050"));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("060", ' ', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "a 060"));
    dataField.addSubfield(new SubfieldImpl('b', "b 060"));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("080", ' ', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "a 080"));
    dataField.addSubfield(new SubfieldImpl('b', "b 080"));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("082", ' ', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "a 082-1/"));
    dataField.addSubfield(new SubfieldImpl('a', "a 082-2/"));
    dataField.addSubfield(new SubfieldImpl('b', "b 082"));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("086", ' ', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "a 086"));
    dataField.addSubfield(new SubfieldImpl('z', "z 086"));
    marcRecord.addVariableField(dataField);

    dataField = new DataFieldImpl("090", ' ', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "a 090"));
    dataField.addSubfield(new SubfieldImpl('b', "b 090"));
    marcRecord.addVariableField(dataField);

    var rowData = marcToUnifiedTableRowMapper.processRecord(marcRecord,
            List.of(INSTANCE_CLASSIFICATION), forCsv);

    var expectedCsvRowData = "Classification identifier type;Classification\n"
            + "LC;a 050 b 050 | NLM;a 060 b 060 | UDC;a 080 b 080 | Dewey;a 082-1 | "
            + "Dewey;a 082-2 b 082 | GDC;a 086 | GDC;z 086 | LC;a 090 b 090";
    var expectedPreviewData = "LC\u001F;a 050 b 050\u001F|NLM\u001F;a 060 b 060\u001F|"
            + "UDC\u001F;a 080 b 080\u001F|Dewey\u001F;a 082-1\u001F|Dewey\u001F;"
            + "a 082-2 b 082\u001F|GDC\u001F;a 086\u001F|GDC\u001F;z 086\u001F|LC\u001F;"
            + "a 090 b 090";

    assertThat(rowData.getFirst()).isEqualTo(forCsv ? expectedCsvRowData : expectedPreviewData);
  }

  @Test
  void processRecordWithSinglePublicationTest() {
    var marcRecord = new RecordImpl();
    marcRecord.setLeader(new LeaderImpl("04295nam a22004573a 4500"));

    var controlField = new ControlFieldImpl(DATE_TIME_CONTROL_FIELD, "20240101100202.4");
    marcRecord.addVariableField(controlField);

    var dataField = new DataFieldImpl("260", ' ', ' ');
    dataField.addSubfield(new SubfieldImpl('a', "London"));
    dataField.addSubfield(new SubfieldImpl('b', "Penguin Books"));
    dataField.addSubfield(new SubfieldImpl('c', "2023"));
    marcRecord.addVariableField(dataField);

    var rowData = marcToUnifiedTableRowMapper.processRecord(marcRecord,
            List.of(INSTANCE_PUBLICATION), true);

    assertThat(rowData.getFirst()).isEqualTo(
            "Publisher;Publisher role;Place of publication;Publication date\n"
                    + "Penguin Books;-;London;2023");
  }
}
