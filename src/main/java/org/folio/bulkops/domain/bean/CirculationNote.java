package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Date;
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
public class CirculationNote {
  @JsonProperty("id")
  private String id;

  public enum NoteTypeEnum {
    IN("Check in"),

    OUT("Check out");

    private final String value;

    NoteTypeEnum(String value) {
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
    public static NoteTypeEnum fromValue(String value) {
      for (NoteTypeEnum b : NoteTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("noteType")
  private NoteTypeEnum noteType;

  @JsonProperty("note")
  private String note;

  @JsonProperty("staffOnly")
  private Boolean staffOnly;

  @JsonProperty("source")
  private Source source;

  @JsonProperty("date")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  private Date date;

}

