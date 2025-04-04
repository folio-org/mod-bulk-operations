package org.folio.bulkops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_ELECTRONIC_ACCESS;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_SUBJECT;
import static org.folio.bulkops.util.Constants.DATE_TIME_CONTROL_FIELD;
import static org.folio.bulkops.util.Constants.NON_PRINTING_DELIMITER;

import java.util.List;

import org.folio.bulkops.BaseTest;
import org.junit.jupiter.api.Test;
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
    dataField.addSubfield(new SubfieldImpl('3', "materials"));
    dataField.addSubfield(new SubfieldImpl('z', "public note"));
    marcRecord.addVariableField(dataField);
    var rowData = marcToUnifiedTableRowMapper.processRecord(marcRecord, List.of(INSTANCE_ELECTRONIC_ACCESS), true);

    assertThat(rowData.getFirst()).isEqualTo("Related resource%s;url%s;text%s;materials%s;public note"
      .formatted(NON_PRINTING_DELIMITER, NON_PRINTING_DELIMITER, NON_PRINTING_DELIMITER, NON_PRINTING_DELIMITER));
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
    dataField.addSubfield(new SubfieldImpl('3', "materials"));
    dataField.addSubfield(new SubfieldImpl('z', "public note"));
    dataField.addSubfield(new SubfieldImpl('z', "public note2"));
    marcRecord.addVariableField(dataField);
    var rowData = marcToUnifiedTableRowMapper.processRecord(marcRecord, List.of(INSTANCE_ELECTRONIC_ACCESS), true);

    assertThat(rowData.getFirst()).isEqualTo("Related resource%s;url%s;text text2%s;materials%s;public note public note2"
      .formatted(NON_PRINTING_DELIMITER, NON_PRINTING_DELIMITER, NON_PRINTING_DELIMITER, NON_PRINTING_DELIMITER));
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
    dataField.addSubfield(new SubfieldImpl('3', "materials"));
    dataField.addSubfield(new SubfieldImpl('z', "public note"));
    marcRecord.addVariableField(dataField);
    var rowData = marcToUnifiedTableRowMapper.processRecord(marcRecord, List.of(INSTANCE_ELECTRONIC_ACCESS), true);

    assertThat(rowData.getFirst()).isEqualTo("Related resource%s;-%s;text%s;materials%s;public note"
      .formatted(NON_PRINTING_DELIMITER, NON_PRINTING_DELIMITER, NON_PRINTING_DELIMITER, NON_PRINTING_DELIMITER));
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
    var rowData = marcToUnifiedTableRowMapper.processRecord(marcRecord, List.of(INSTANCE_SUBJECT), true);

    assertThat(rowData.getFirst()).isEqualTo("- text subject c subject d%s;Medical Subject Headings%s;Personal name"
      .formatted(NON_PRINTING_DELIMITER, NON_PRINTING_DELIMITER, NON_PRINTING_DELIMITER, NON_PRINTING_DELIMITER));
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

    var rowData = marcToUnifiedTableRowMapper.processRecord(marcRecord, List.of(INSTANCE_SUBJECT), true);

    var expectedRowData = "a 600 b text subject c subject d\u001f;Medical Subject Headings\u001f;Personal name\u001f|" +
      "text subject c subject c 2 subject d\u001f;National Agriculture Library subject authority file\u001f;Corporate name\u001f|" +
      "611 a text subject c subject d\u001f;Medical Subject Headings\u001f;Meeting name\u001f|" +
      "text subject c subject d\u001f;Canadian Subject Headings\u001f;Uniform title\u001f|" +
      "text subject c subject d\u001f;Medical Subject Headings\u001f;Named event\u001f|" +
      "text subject c subject d\u001f;-\u001f;Chronological term\u001f|" +
      "text subject c subject d\u001f;-\u001f;Topical term\u001f|" +
      "text subject c subject d\u001f;Medical Subject Headings\u001f;Geographic name\u001f|" +
      "a text subject c subject d\u001f;Library of Congress Children’s and Young Adults' Subject Headings\u001f;Geographic name\u001f|" +
      "text subject c subject d\u001f;-\u001f;Genre/Form";

    assertThat(rowData.getFirst()).isEqualTo(expectedRowData);
  }
}
