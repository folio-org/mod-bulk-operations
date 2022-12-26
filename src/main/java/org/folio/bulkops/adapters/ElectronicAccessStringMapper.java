package org.folio.bulkops.adapters;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.ElectronicAccessRelationshipClient;
import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.error.NotFoundException;
import org.folio.bulkops.service.ErrorService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.adapters.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.adapters.Constants.ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER;
import static org.folio.bulkops.adapters.Constants.ITEM_DELIMITER;

@Component
@RequiredArgsConstructor
@Log4j2
public class ElectronicAccessStringMapper {
  private final ElectronicAccessRelationshipClient relationshipClient;
  private final ErrorService errorService;


  public String getElectronicAccessesToString(List<ElectronicAccess> electronicAccesses, UUID bulkOperationId, String identifier) {
    var errors = new HashSet<String>();
    var stringOutput = isEmpty(electronicAccesses) ?
      EMPTY :
      electronicAccesses.stream()
        .map(electronicAccess -> this.electronicAccessToString(electronicAccess, errors))
        .collect(Collectors.joining(ITEM_DELIMITER));
    if (Objects.nonNull(bulkOperationId)) {
      errors.forEach(e -> errorService.saveError(bulkOperationId, identifier, e));
    }
    return stringOutput;
  }

  private String electronicAccessToString(ElectronicAccess access, Set<String> errors) {
    var relationshipNameAndId = isEmpty(access.getRelationshipId()) ? ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER : getRelationshipNameAndIdById(access.getRelationshipId());
    if (isNotEmpty(access.getRelationshipId()) && relationshipNameAndId.startsWith(ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER)) {
      var error = "Electronic access relationship not found by id=" + access.getRelationshipId();
      errors.add(error);
    }
    return String.join(ARRAY_DELIMITER,
      access.getUri(),
      isEmpty(access.getLinkText()) ? EMPTY : access.getLinkText(),
      isEmpty(access.getMaterialsSpecification()) ? EMPTY : access.getMaterialsSpecification(),
      isEmpty(access.getPublicNote()) ? EMPTY : access.getPublicNote(),
      relationshipNameAndId);
  }

  @Cacheable(cacheNames = "relationships")
  public String getRelationshipNameAndIdById(String id) {
    try {
      return relationshipClient.getById(id).getName() + ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER + id;
    } catch (NotFoundException e) {
      return ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER + id;
    }
  }

}
