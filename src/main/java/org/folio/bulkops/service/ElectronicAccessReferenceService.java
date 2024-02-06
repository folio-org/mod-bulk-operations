package org.folio.bulkops.service;

import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.ElectronicAccessRelationshipClient;
import org.folio.bulkops.exception.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class ElectronicAccessReferenceService {
  private final ElectronicAccessRelationshipClient relationshipClient;

  @Cacheable(cacheNames = "electronicAccessRelationshipNames")
  public String getRelationshipNameById(String id) {
    try {
      return relationshipClient.getById(id).getName();
    } catch (NotFoundException e) {
      log.error("Electronic access relationship was not found by id={}", id);
      return id;
    }
  }

  @Cacheable(cacheNames = "electronicAccessRelationshipIds")
  public String getRelationshipIdByName(String name) {
    var relationShips = relationshipClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    return relationShips.getElectronicAccessRelationships().isEmpty() ?
      name :
      relationShips.getElectronicAccessRelationships().get(0).getId();
  }
}
