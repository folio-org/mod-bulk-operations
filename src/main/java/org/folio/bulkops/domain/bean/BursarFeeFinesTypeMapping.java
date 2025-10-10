package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BursarFeeFinesTypeMapping {
  @JsonProperty("feefineTypeId")
  private UUID feefineTypeId;

  @JsonProperty("itemType")
  private String itemType;

  @JsonProperty("itemDescription")
  private String itemDescription;

  /**
   * Gets or Sets itemCode.
   */
  public enum ItemCodeEnum {
    CHARGE("CHARGE"),

    PAYMENT("PAYMENT");

    private final String value;

    ItemCodeEnum(String value) {
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
    public static ItemCodeEnum fromValue(String value) {
      for (ItemCodeEnum b : ItemCodeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("itemCode")
  private ItemCodeEnum itemCode;
}

