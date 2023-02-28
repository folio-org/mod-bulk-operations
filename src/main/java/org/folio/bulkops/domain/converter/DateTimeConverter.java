package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.util.Constants.DATE_TIME_PATTERN;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.apache.commons.lang3.ObjectUtils;

import com.opencsv.bean.AbstractBeanField;


public class DateTimeConverter extends AbstractBeanField<String, Date> {
  @Override
  protected Date convert(String value) {
    if (isNotEmpty(value)) {
      LocalDateTime localDateTime = LocalDateTime.parse(value, DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
      return java.sql.Timestamp.valueOf(localDateTime);
    }
    return null;
  }

  @Override
  protected String convertToWrite(Object value) {
    var date = (Date) value;
    var format = new SimpleDateFormat(DATE_TIME_PATTERN);
    return ObjectUtils.isNotEmpty(date) ? format.format(date) : EMPTY;
  }
}
