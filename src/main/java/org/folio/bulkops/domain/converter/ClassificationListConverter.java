package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.CLASSIFICATION_HEADINGS;
import static org.folio.bulkops.util.Constants.NEW_LINE_SEPARATOR;
import static org.folio.bulkops.util.Constants.SPECIAL_ITEM_DELIMITER;
import static org.folio.bulkops.util.Constants.SPECIAL_ITEM_DELIMITER_REGEX;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.Classification;
import org.folio.bulkops.service.ClassificationHelper;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ClassificationListConverter extends BaseConverter<List<Classification>> {

  @Override
  public List<Classification> convertToObject(String value) {
    var tokens = value.split(NEW_LINE_SEPARATOR, 2);
    var classificationData = tokens.length == 2 ? tokens[1] : value;
    return Arrays.stream(classificationData.split(SPECIAL_ITEM_DELIMITER_REGEX))
      .map(ClassificationHelper.service()::restoreClassificationItem)
      .filter(ObjectUtils::isNotEmpty)
      .toList();
  }

  @Override
  public String convertToString(List<Classification> object) {
    return ObjectUtils.isEmpty(object) ?
      EMPTY :
      CLASSIFICATION_HEADINGS +
        object.stream()
          .filter(Objects::nonNull)
          .map(ClassificationHelper.service()::classificationToString)
          .collect(Collectors.joining(SPECIAL_ITEM_DELIMITER));
  }

}
