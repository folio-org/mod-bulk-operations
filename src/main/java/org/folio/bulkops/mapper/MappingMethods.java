package org.folio.bulkops.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

//  private final ObjectMapper objectMapper;
//
//  public MappingMethods(ObjectMapper objectMapper) {
//    this.objectMapper = objectMapper;
//  }
  public Date offsetDateTimeAsDate(OffsetDateTime offsetDateTime) {
    return offsetDateTime == null ? null : Date.from(offsetDateTime.toInstant());
  }

//  @Named("mapMarcRuleJsonToObject")
//  public org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection mapMarcRuleJsonToObject(String json) {
//    try {
//      return objectMapper.readValue(json, org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection.class);
//    } catch (JsonProcessingException e) {
//      throw new RuntimeException("Invalid MARC rule JSON", e);
//    }
//  }
//
//  @Named("mapRuleJsonToObject")
//  public org.folio.bulkops.domain.dto.BulkOperationRuleCollection mapRuleJsonToObject(String json) {
//    try {
//      return objectMapper.readValue(json, org.folio.bulkops.domain.dto.BulkOperationRuleCollection.class);
//    } catch (JsonProcessingException e) {
//      throw new RuntimeException("Invalid MARC rule JSON", e);
//    }
//  }


//  @Named("mapToBulkOperationRule")
//  public BulkOperationRule mapToBulkOperationRuleCollection(List<BulkOperationRule> source) {
//    if (source == null) return null;
//    return objectMapper.convertValue(source, BulkOperationRule.class);
//  }
//
//  @Named("mapToBulkOperationMarcRule")
//  public BulkOperationMarcRule mapToBulkOperationMarcRuleCollection(List<BulkOperationMarcRule> source) {
//    if (source == null) return null;
//    return objectMapper.convertValue(source, BulkOperationMarcRule.class);
//  }
}
