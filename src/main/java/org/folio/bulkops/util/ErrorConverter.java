package org.folio.bulkops.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import javax.persistence.AttributeConverter;

import static java.util.Objects.isNull;

@Log4j2
@RequiredArgsConstructor
public class ErrorConverter implements AttributeConverter<Error, String> {
  private final ObjectMapper objectMapper;

  @Override
  public String convertToDatabaseColumn(Error error) {
    try {
      return objectMapper.writeValueAsString(error);
    } catch (JsonProcessingException e) {
      log.error("Failed to convert Error to JSON: {}", e.getMessage());
      return null;
    }
  }

  @Override
  public Error convertToEntityAttribute(String s) {
    try {
      return isNull(s) ? null : objectMapper.readValue(s, Error.class);
    } catch (Exception e) {
      log.error("Failed to read Error from JSON: {}", e.getMessage());
      return null;
    }
  }
}
