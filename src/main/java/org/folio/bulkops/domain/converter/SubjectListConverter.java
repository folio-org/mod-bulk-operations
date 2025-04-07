package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.NEW_LINE_SEPARATOR;
import static org.folio.bulkops.util.Constants.SPECIAL_ITEM_DELIMITER;
import static org.folio.bulkops.util.Constants.SPECIAL_ITEM_DELIMITER_REGEX;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.Subject;
import org.folio.bulkops.service.SubjectHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SubjectListConverter extends BaseConverter<List<Subject>> {

  @Override
  public List<Subject> convertToObject(String value) {
    var tokens = value.split(NEW_LINE_SEPARATOR, 2);
    var subjectData = tokens.length == 2 ? tokens[1] : value;
    return Arrays.stream(subjectData.split(SPECIAL_ITEM_DELIMITER_REGEX))
      .map(SubjectHelper.service()::restoreSubjectItem)
      .filter(ObjectUtils::isNotEmpty)
      .toList();
  }

  @Override
  public String convertToString(List<Subject> object) {
    return ObjectUtils.isEmpty(object) ?
      EMPTY :
        object.stream()
          .filter(Objects::nonNull)
          .map(SubjectHelper.service()::subjectToString)
          .collect(Collectors.joining(SPECIAL_ITEM_DELIMITER));
  }

}
