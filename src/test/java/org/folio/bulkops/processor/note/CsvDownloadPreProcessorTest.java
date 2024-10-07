package org.folio.bulkops.processor.note;

import org.folio.bulkops.domain.entity.BulkOperation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvDownloadPreProcessorTest {

  @Test
  void processTenantInHeadersTest() {
    var downloadPreProcessor = new TestCsvDownloadPreProcessor();
    var headers = new String[]{"Test", "Tenant"};

    var expected = Arrays.stream(new String[]{"Test", "Member"}).toList();
    var actual = downloadPreProcessor.processTenantInHeaders(headers, true, true);
    assertEquals(expected, Arrays.stream(actual).toList());

    expected = Arrays.stream(new String[]{"Test"}).toList();
    actual = downloadPreProcessor.processTenantInHeaders(headers, false, true);
    assertEquals(expected, Arrays.stream(actual).toList());

    headers = new String[]{"Test", "Header"};
    expected = Arrays.stream(headers).toList();
    actual = downloadPreProcessor.processTenantInHeaders(headers, true, false);
    assertEquals(expected, Arrays.stream(actual).toList());
  }

  @Test
  void processTenantInRowsTest() {
    var downloadPreProcessor = new TestCsvDownloadPreProcessor();
    var row = new String[]{"value", "tenantId"};

    var expected = Arrays.stream(row).toList();
    var actual = downloadPreProcessor.processTenantInRows(row, true, true);
    assertEquals(expected, Arrays.stream(actual).toList());

    expected = Arrays.stream( new String[]{"value"}).toList();
    actual = downloadPreProcessor.processTenantInRows(row, false, true);
    assertEquals(expected, Arrays.stream(actual).toList());

    row = new String[]{"value1", "value2"};
    expected = Arrays.stream(row).toList();
    actual = downloadPreProcessor.processTenantInRows(row, false, false);
    assertEquals(expected, Arrays.stream(actual).toList());
  }

  private class TestCsvDownloadPreProcessor extends CsvDownloadPreProcessor {

    @Override
    protected List<String> getNoteTypeNames(BulkOperation bulkOperation) {
      return null;
    }

    @Override
    protected int getNotePosition() {
      return 0;
    }
  }
}
