package org.folio.bulkops.util;

import static java.util.Objects.isNull;

import javax.persistence.AttributeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.dto.Error;

@Log4j2
public class ErrorConverter implements AttributeConverter<Error, String> {
  @Override
  public String convertToDatabaseColumn(Error error) {
    try {
      return new ObjectMapper().writeValueAsString(error);
    } catch (JsonProcessingException e) {
      log.error("Failed to convert Error to JSON: {}", e.getMessage());
      return null;
    }
  }

  @Override
  public Error convertToEntityAttribute(String s) {
    try {
      return isNull(s) ? null : new ObjectMapper().readValue(s, Error.class);
    } catch (Exception e) {
      log.error("Failed to read Error from JSON: {}", e.getMessage());
      return null;
    }
  }
}
