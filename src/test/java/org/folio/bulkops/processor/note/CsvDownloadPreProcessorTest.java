package org.folio.bulkops.processor.note;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.folio.bulkops.domain.bean.UserTenant;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.junit.jupiter.api.Test;

class CsvDownloadPreProcessorTest {

  @Test
  void processTenantInHeadersTest() {
    var downloadPreProcessor = new TestCsvDownloadPreProcessor();
    var headers = new String[] {"Test", "Tenant"};

    var expected = Arrays.stream(new String[] {"Test", "Member"}).toList();
    var actual = downloadPreProcessor.processTenantInHeaders(headers, true, true);
    assertEquals(expected, Arrays.stream(actual).toList());

    expected = Arrays.stream(new String[] {"Test"}).toList();
    actual = downloadPreProcessor.processTenantInHeaders(headers, false, true);
    assertEquals(expected, Arrays.stream(actual).toList());

    headers = new String[] {"Test", "Header"};
    expected = Arrays.stream(headers).toList();
    actual = downloadPreProcessor.processTenantInHeaders(headers, true, false);
    assertEquals(expected, Arrays.stream(actual).toList());

    headers = new String[] {"Test", "Header"};
    expected = Arrays.stream(headers).toList();
    actual = downloadPreProcessor.processTenantInHeaders(headers, false, false);
    assertEquals(expected, Arrays.stream(actual).toList());
  }

  @Test
  void processTenantInRowsTest() {
    Map<String, UserTenant> userTenants = new HashMap<>();
    var userTenant = new UserTenant();
    userTenant.setTenantId("tenantId");
    userTenant.setTenantName("tenantName");
    userTenants.put("tenantId", userTenant);
    var row = new String[] {"value", "tenantId"};
    var downloadPreProcessor = new TestCsvDownloadPreProcessor();
    var expected = Arrays.stream(new String[] {"value", "tenantName"}).toList();
    var actual = downloadPreProcessor.processTenantInRows(row, true, true, userTenants);
    assertEquals(expected, Arrays.stream(actual).toList());

    expected = Arrays.stream(new String[] {"value"}).toList();
    actual = downloadPreProcessor.processTenantInRows(row, false, true, userTenants);
    assertEquals(expected, Arrays.stream(actual).toList());

    row = new String[] {"value1", "value2"};
    expected = Arrays.stream(row).toList();
    actual = downloadPreProcessor.processTenantInRows(row, true, false, userTenants);
    assertEquals(expected, Arrays.stream(actual).toList());

    row = new String[] {"value1", "value2"};
    expected = Arrays.stream(row).toList();
    actual = downloadPreProcessor.processTenantInRows(row, false, false, userTenants);
    assertEquals(expected, Arrays.stream(actual).toList());
  }

  private static class TestCsvDownloadPreProcessor extends CsvDownloadPreProcessor {

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
