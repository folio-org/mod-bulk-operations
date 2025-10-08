package org.folio.bulkops.service;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.util.Constants.HYPHEN;
import static org.folio.bulkops.util.Constants.SPECIAL_ARRAY_DELIMITER;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.Classification;
import org.folio.bulkops.exception.EntityFormatException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class ClassificationService {

  private static final int NUMBER_OF_CLASSIFICATION_COMPONENTS = 2;
  private static final int CLASSIFICATION_TYPE_INDEX = 0;
  private static final int CLASSIFICATION_NUMBER_INDEX = 1;

  private final ClassificationReferenceService classificationReferenceService;

  private final String delimiter = SPECIAL_ARRAY_DELIMITER;

  public String classificationToString(Classification classification) {
    return String.join(delimiter,
      isEmpty(classification.getClassificationTypeId()) ? HYPHEN
              : classificationReferenceService.getClassificationTypeNameById(
                      classification.getClassificationTypeId(), null),
      isEmpty(classification.getClassificationNumber()) ? HYPHEN
              : classification.getClassificationNumber());
  }

  public Classification restoreClassificationItem(@NotNull String classificationString) {
    if (isNotEmpty(classificationString)) {
      var tokens = classificationString.split(delimiter, -1);
      if (NUMBER_OF_CLASSIFICATION_COMPONENTS == tokens.length) {
        return Classification.builder()
          .classificationTypeId(classificationReferenceService.getClassificationTypeIdByName(
                  tokens[CLASSIFICATION_TYPE_INDEX]))
          .classificationNumber(tokens[CLASSIFICATION_NUMBER_INDEX])
          .build();
      }
      throw new EntityFormatException(String.format(
              "Illegal number of classification elements: %d, expected: %d",
              tokens.length, NUMBER_OF_CLASSIFICATION_COMPONENTS));
    }
    return null;
  }
}
