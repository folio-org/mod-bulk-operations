package org.folio.bulkops.service;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.SPECIAL_ARRAY_DELIMITER;

import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.exception.EntityFormatException;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class ElectronicAccessService {
  private final ElectronicAccessReferenceService electronicAccessReferenceService;

  private static final int NUMBER_OF_ELECTRONIC_ACCESS_COMPONENTS = 5;
  private static final int ELECTRONIC_ACCESS_RELATIONSHIP_ID_INDEX = 0;
  private static final int ELECTRONIC_ACCESS_URI_INDEX = 1;
  private static final int ELECTRONIC_ACCESS_LINK_TEXT_INDEX = 2;
  private static final int ELECTRONIC_ACCESS_MATERIAL_SPECIFICATION_INDEX = 3;
  private static final int ELECTRONIC_ACCESS_PUBLIC_NOTE_INDEX = 4;

  private final FolioExecutionContext folioExecutionContext;


  public String electronicAccessToString(ElectronicAccess access) {
    return electronicAccessToString(access, SPECIAL_ARRAY_DELIMITER);
  }

  public String itemElectronicAccessToString(ElectronicAccess access) {
    return electronicAccessToString(access, ARRAY_DELIMITER);
  }

  public ElectronicAccess restoreElectronicAccessItem(String access) {
    return restoreElectronicAccessItem(access, SPECIAL_ARRAY_DELIMITER);
  }

  public ElectronicAccess restoreItemElectronicAccessItem(String access) {
    return restoreElectronicAccessItem(access, ARRAY_DELIMITER);
  }

  private String electronicAccessToString(ElectronicAccess access, String delimiter) {
    log.info("electronicAccessToString: {}, {}", access.getRelationshipId(), folioExecutionContext.getTenantId());
    return String.join(delimiter,
      isEmpty(access.getRelationshipId()) ? EMPTY : getRelationshipName(access),
      isNull(access.getUri()) ? EMPTY : access.getUri(),
      isEmpty(access.getLinkText()) ? EMPTY : access.getLinkText(),
      isEmpty(access.getMaterialsSpecification()) ? EMPTY : access.getMaterialsSpecification(),
      isEmpty(access.getPublicNote()) ? EMPTY : access.getPublicNote());
  }

  private ElectronicAccess restoreElectronicAccessItem(String access, String delimiter) {
    if (isNotEmpty(access)) {
      var tokens = access.split(delimiter, -1);
      if (NUMBER_OF_ELECTRONIC_ACCESS_COMPONENTS == tokens.length) {
        var uri = tokens[ELECTRONIC_ACCESS_URI_INDEX];
        return ElectronicAccess.builder()
          .relationshipId(electronicAccessReferenceService.getRelationshipIdByName(tokens[ELECTRONIC_ACCESS_RELATIONSHIP_ID_INDEX]))
          .uri(isNull(uri) ? EMPTY : uri)
          .linkText(isEmpty(tokens[ELECTRONIC_ACCESS_LINK_TEXT_INDEX]) ? null : tokens[ELECTRONIC_ACCESS_LINK_TEXT_INDEX])
          .materialsSpecification(isEmpty(tokens[ELECTRONIC_ACCESS_MATERIAL_SPECIFICATION_INDEX]) ? null : tokens[ELECTRONIC_ACCESS_MATERIAL_SPECIFICATION_INDEX])
          .publicNote(isEmpty(tokens[ELECTRONIC_ACCESS_PUBLIC_NOTE_INDEX]) ? null : tokens[ELECTRONIC_ACCESS_PUBLIC_NOTE_INDEX])
          .build();
      }
      throw new EntityFormatException(String.format("Illegal number of electronic access elements: %d, expected: %d", tokens.length, NUMBER_OF_ELECTRONIC_ACCESS_COMPONENTS));
    }
    return null;
  }

  private String getRelationshipName(ElectronicAccess access) {
    log.info("getRelationshipName {}", access.getRelationshipId());
    var idTenant = access.getRelationshipId().split(ARRAY_DELIMITER);
    access.setRelationshipId(idTenant[0]);
    return electronicAccessReferenceService.getRelationshipNameById(idTenant[0], idTenant.length > 1 ? idTenant[1] : null);
  }
}
