package org.folio.bulkops.domain.bean;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonProperty;

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
public class TimePeriod {
  @JsonProperty("duration")
  private int duration;

  @JsonProperty("intervalId")
  private String intervalId;

  public ChronoUnit getInterval() {
    final String id = getIntervalId();
    if (id != null) {
      return ChronoUnit.valueOf(id.toUpperCase());
    } else {
      // Default is days
      return ChronoUnit.DAYS;
    }
  }

  public long between(ZonedDateTime start, ZonedDateTime end) {
    return getInterval().between(start, end);
  }

  public boolean isLongTermPeriod() {
    final ChronoUnit chronoUnit = getInterval();

    return chronoUnit == ChronoUnit.DAYS || chronoUnit == ChronoUnit.WEEKS
      || chronoUnit == ChronoUnit.MONTHS;
  }
}
