package org.folio.bulkops.domain.bean;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

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
public class ScheduleParameters {
  @JsonProperty("id")
  private UUID id;

  @JsonProperty("scheduleFrequency")
  private Integer scheduleFrequency;

  /**
   * Time period for repeating job
   */
  public enum SchedulePeriodEnum {
    MONTH("MONTH"),

    WEEK("WEEK"),

    DAY("DAY"),

    HOUR("HOUR"),

    EXACT_DATE("EXACT_DATE"),

    NONE("NONE");

    private String value;

    SchedulePeriodEnum(String value) {
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
    public static SchedulePeriodEnum fromValue(String value) {
      for (SchedulePeriodEnum b : SchedulePeriodEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("schedulePeriod")
  private SchedulePeriodEnum schedulePeriod;

  @JsonProperty("schedulingDate")
  @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime schedulingDate;

  @JsonProperty("scheduleTime")
  private String scheduleTime;

  /**
   * Day of week
   */
  public enum WeekDaysEnum {
    MONDAY("MONDAY"),

    TUESDAY("TUESDAY"),

    WEDNESDAY("WEDNESDAY"),

    THURSDAY("THURSDAY"),

    FRIDAY("FRIDAY"),

    SATURDAY("SATURDAY"),

    SUNDAY("SUNDAY");

    private String value;

    WeekDaysEnum(String value) {
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
    public static WeekDaysEnum fromValue(String value) {
      for (WeekDaysEnum b : WeekDaysEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("weekDays")
  @Valid
  private List<WeekDaysEnum> weekDays = null;

  @JsonProperty("timeZone")
  private String timeZone = "UTC";
}

