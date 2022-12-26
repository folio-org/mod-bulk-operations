package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SelectFieldOptions   {
  @JsonProperty("values")
  @Valid
  private List<SelectFieldOption> values = null;

  /**
   * Defines sorting order for the custom field
   */
  public enum SortingOrderEnum {
    ASC("ASC"),

    DESC("DESC"),

    CUSTOM("CUSTOM");

    private final String value;

    SortingOrderEnum(String value) {
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
    public static SortingOrderEnum fromValue(String value) {
      for (SortingOrderEnum b : SortingOrderEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("sortingOrder")
  private SortingOrderEnum sortingOrder;
}

