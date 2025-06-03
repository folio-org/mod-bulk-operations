package org.folio.bulkops.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.springframework.stereotype.Component;
import org.mapstruct.Named;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class MappingMethods {

  private final ObjectMapper objectMapper;

  public MappingMethods(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }
  public Date offsetDateTimeAsDate(OffsetDateTime offsetDateTime) {
    return offsetDateTime == null ? null : Date.from(offsetDateTime.toInstant());
  }


//  @Named("mapToBulkOperationRuleCollection")
//  public BulkOperationRuleCollection mapToBulkOperationRuleCollection(Map<String, Object> source) {
//    if (source == null) return null;
//    return objectMapper.convertValue(source, BulkOperationRuleCollection.class);
//  }
//
//  @Named("mapToBulkOperationMarcRuleCollection")
//  public BulkOperationMarcRuleCollection mapToBulkOperationMarcRuleCollection(Map<String, Object> source) {
//    if (source == null) return null;
//    return objectMapper.convertValue(source, BulkOperationMarcRuleCollection.class);
//  }
}
