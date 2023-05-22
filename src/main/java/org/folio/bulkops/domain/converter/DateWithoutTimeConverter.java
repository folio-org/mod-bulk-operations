package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.util.Constants.DATE_WITHOUT_TIME_PATTERN;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.apache.commons.lang3.ObjectUtils;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;


public class DateWithoutTimeConverter extends AbstractBeanField<String, Date> {
  @Override
  protected Date convert(String value) throws CsvConstraintViolationException {
    if (isNotEmpty(value)) {
      try {
        LocalDateTime localDateTime = LocalDateTime.parse(value, DateTimeFormatter.ofPattern(DATE_WITHOUT_TIME_PATTERN));
        return Date.from(localDateTime.atZone(ZoneOffset.systemDefault()).toInstant());
      } catch (Exception e) {
        throw new CsvConstraintViolationException(String.format("Incorrect datetime value: %s", value));
      }
    }
    return null;
  }

  @Override
  protected String convertToWrite(Object value) {
    var date = (Date) value;
    var format = new SimpleDateFormat(DATE_WITHOUT_TIME_PATTERN);
    return ObjectUtils.isNotEmpty(date) ? format.format(date) : EMPTY;
  }
}
