package org.folio.bulkops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_ELECTRONIC_ACCESS;
import static org.folio.bulkops.util.Constants.DATE_TIME_CONTROL_FIELD;

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

    assertThat(rowData.getFirst()).isEqualTo("Related resource;url;text;materials;public note");
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

    assertThat(rowData.getFirst()).isEqualTo("Related resource;-;text;materials;public note");
  }
}
