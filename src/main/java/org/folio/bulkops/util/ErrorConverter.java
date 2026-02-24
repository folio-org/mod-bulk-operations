package org.folio.bulkops.util;

import static java.util.Objects.isNull;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Log4j2
@RequiredArgsConstructor
@Converter
public class ErrorConverter implements AttributeConverter<Error, String> {
  private final ObjectMapper objectMapper;

  @Override
  public String convertToDatabaseColumn(Error error) {
    try {
      return objectMapper.writeValueAsString(error);
    } catch (JacksonException e) {
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
