package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Date;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;
import org.springframework.format.annotation.DateTimeFormat;

@With
@Getter

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemStatus {
  public enum NameEnum {
    AGED_TO_LOST("Aged to lost"),
    AVAILABLE("Available"),
    AWAITING_DELIVERY("Awaiting delivery"),
    AWAITING_PICKUP("Awaiting pickup"),
    CHECKED_OUT("Checked out"),
    CLAIMED_RETURNED("Claimed returned"),
    DECLARED_LOST("Declared lost"),
    LOST_AND_PAID("Lost and paid"),
    LONG_MISSING("Long missing"),
    MISSING("Missing"),
    IN_PROCESS("In process"),
    IN_PROCESS_NON_REQUESTABLE_("In process (non-requestable)"),
    IN_TRANSIT("In transit"),
    INTELLECTUAL_ITEM("Intellectual item"),
    ON_ORDER("On order"),
    ORDER_CLOSED("Order closed"),
    PAGED("Paged"),
    RESTRICTED("Restricted"),
    UNAVAILABLE("Unavailable"),
    UNKNOWN("Unknown"),
    WITHDRAWN("Withdrawn");

    private final String value;

    NameEnum(String value) {
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
    public static NameEnum fromValue(String value) {
      for (NameEnum b : NameEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("name")
  private NameEnum name;

  @JsonProperty("date")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Date date;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InventoryItemStatus that = (InventoryItemStatus) o;
    return name == that.name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}

