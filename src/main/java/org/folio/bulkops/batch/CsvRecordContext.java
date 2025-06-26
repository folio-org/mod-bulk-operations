package org.folio.bulkops.batch;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class CsvRecordContext {

    private static AtomicReference<String> identifier = new AtomicReference<>();

    private static AtomicReference<UUID> bulkOperationId = new AtomicReference<>();

    private CsvRecordContext() {}

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
}
