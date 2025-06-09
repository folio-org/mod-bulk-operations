package org.folio.bulkops.mapper;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Component
public class MappingMethods {

  public OffsetDateTime mapDateToOffsetDateTime(Date date) {
    return date == null ? null : date.toInstant().atOffset(ZoneOffset.UTC);
  }

  public Date offsetDateTimeAsDate(OffsetDateTime offsetDateTime) {
    return offsetDateTime == null ? null : Date.from(offsetDateTime.toInstant());
  }
}
