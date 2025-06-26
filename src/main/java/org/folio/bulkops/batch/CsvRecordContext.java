package org.folio.bulkops.batch;

import java.util.UUID;

public class CsvRecordContext implements AutoCloseable {

    private static final ThreadLocal<String> identifier = new ThreadLocal<>();

    private static final ThreadLocal<UUID> bulkOperationId = new ThreadLocal<>();

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

    @Override
    public void close() {
        identifier.remove();
        bulkOperationId.remove();
    }
}
