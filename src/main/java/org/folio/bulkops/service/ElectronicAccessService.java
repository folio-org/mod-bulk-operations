package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.restore;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Utils.ofEmptyString;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.ElectronicAccessRelationshipClient;
import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.exception.EntityFormatException;
import org.folio.bulkops.exception.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class ElectronicAccessService {
  private final ElectronicAccessRelationshipClient relationshipClient;

  private static final int NUMBER_OF_ELECTRONIC_ACCESS_COMPONENTS = 6;
  private static final int ELECTRONIC_ACCESS_URI_INDEX = 0;
  private static final int ELECTRONIC_ACCESS_LINK_TEXT_INDEX = 1;
  private static final int ELECTRONIC_ACCESS_MATERIAL_SPECIFICATION_INDEX = 2;
  private static final int ELECTRONIC_ACCESS_PUBLIC_NOTE_INDEX = 3;

  public String electronicAccessToString(ElectronicAccess access) {
    List<String> entries = new ArrayList<>();
    ofEmptyString(access.getUri()).ifPresent(e -> entries.add(escape(e)));
    ofEmptyString(access.getLinkText()).ifPresent(e -> entries.add(escape(e)));
    ofEmptyString(access.getMaterialsSpecification()).ifPresent(e -> entries.add(escape(e)));
    ofEmptyString(access.getPublicNote()).ifPresent(e -> entries.add(escape(e)));
    ofEmptyString(access.getRelationshipId()).ifPresent(e -> entries.add(getRelationshipNameAndIdById(access.getRelationshipId())));
    return String.join(ARRAY_DELIMITER, entries);
  }

  @Cacheable(cacheNames = "electronicAccessRelationshipNamesAndIds")
  public String getRelationshipNameAndIdById(String id) {
    try {
      return escape(relationshipClient.getById(id).getName()) + ARRAY_DELIMITER + id;
    } catch (NotFoundException e) {
      log.error("Electronic access relationship not found by id={}", id);
      return ARRAY_DELIMITER + id;
    }
  }

  public ElectronicAccess restoreElectronicAccessItem(String s) {
    if (isNotEmpty(s)) {
      var tokens = s.split(ARRAY_DELIMITER, -1);
      if (NUMBER_OF_ELECTRONIC_ACCESS_COMPONENTS == tokens.length) {
        return ElectronicAccess.builder()
          .uri(restore(tokens[ELECTRONIC_ACCESS_URI_INDEX]))
          .linkText(restore(tokens[ELECTRONIC_ACCESS_LINK_TEXT_INDEX]))
          .materialsSpecification(restore(tokens[ELECTRONIC_ACCESS_MATERIAL_SPECIFICATION_INDEX]))
          .publicNote(restore(tokens[ELECTRONIC_ACCESS_PUBLIC_NOTE_INDEX]))
          .relationshipId(restore(tokens[tokens.length - 1]))
          .build();
      }
      throw new EntityFormatException(String.format("Illegal number of electronic access elements: %d, expected: %d", tokens.length, NUMBER_OF_ELECTRONIC_ACCESS_COMPONENTS));
    }
    return null;
  }
}
