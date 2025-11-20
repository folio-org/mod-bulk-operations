package org.folio.bulkops.batch;

import java.util.UUID;

public class CsvRecordContext implements AutoCloseable {

  private static final ThreadLocal<String> identifier = new ThreadLocal<>();

  private static final ThreadLocal<UUID> bulkOperationId = new ThreadLocal<>();

  private static final ThreadLocal<String> tenantId = new ThreadLocal<>();

  public static String getIdentifier() {
    return identifier.get();
  }

  public static void setIdentifier(String id) {
    identifier.set(id);
  }

  public static UUID getBulkOperationId() {
    return bulkOperationId.get();
  }

  public static void setBulkOperationId(UUID id) {
    bulkOperationId.set(id);
  }

  public static String getTenantId() {
    return tenantId.get();
  }

  public static void setTenantId(String id) {
    identifier.set(id);
  }

  @Override
  public void close() {
    identifier.remove();
    bulkOperationId.remove();
    tenantId.remove();
  }
}
