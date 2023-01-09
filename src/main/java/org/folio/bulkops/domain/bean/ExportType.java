package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


public enum ExportType {

  CIRCULATION_LOG("CIRCULATION_LOG"),

  BURSAR_FEES_FINES("BURSAR_FEES_FINES"),

  BATCH_VOUCHER_EXPORT("BATCH_VOUCHER_EXPORT"),

  EDIFACT_ORDERS_EXPORT("EDIFACT_ORDERS_EXPORT"),

  ORDERS_EXPORT("ORDERS_EXPORT"),

  INVOICE_EXPORT("INVOICE_EXPORT"),

  BULK_EDIT_IDENTIFIERS("BULK_EDIT_IDENTIFIERS"),

  BULK_EDIT_QUERY("BULK_EDIT_QUERY"),

  BULK_EDIT_UPDATE("BULK_EDIT_UPDATE"),

  E_HOLDINGS("E_HOLDINGS");

  private String value;

  ExportType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ExportType fromValue(String value) {
    for (ExportType b : ExportType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

