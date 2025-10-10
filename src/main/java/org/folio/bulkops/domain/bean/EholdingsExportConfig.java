package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import java.util.List;
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
public class EholdingsExportConfig {
  @JsonProperty("recordId")
  private String recordId;

  /**
   * The record type.
   */
  public enum RecordTypeEnum {
    PACKAGE("PACKAGE"),

    RESOURCE("RESOURCE");

    private final String value;

    RecordTypeEnum(String value) {
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
    public static RecordTypeEnum fromValue(String value) {
      for (RecordTypeEnum b : RecordTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("recordType")
  private RecordTypeEnum recordType;

  @JsonProperty("titleSearchFilters")
  private String titleSearchFilters;

  @JsonProperty("packageFields")
  @Valid
  private List<String> packageFields = null;

  @JsonProperty("titleFields")
  @Valid
  private List<String> titleFields = null;
}

