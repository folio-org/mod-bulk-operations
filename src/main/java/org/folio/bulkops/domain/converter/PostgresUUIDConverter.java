package org.folio.bulkops.domain.converter;

import java.util.UUID;

import jakarta.persistence.AttributeConverter;

public class PostgresUUIDConverter implements AttributeConverter<UUID, String> {

  @Override
  public String convertToDatabaseColumn(UUID attribute) {
    return attribute == null ? null : attribute.toString();
  }

  @Override
  public UUID convertToEntityAttribute(String dbData) {
    return dbData == null ? null : UUID.fromString(dbData);
  }
}
