package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.SPECIAL_ITEM_DELIMITER;
import static org.folio.bulkops.util.Constants.SUBJECT_HEADINGS;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.Subject;
import org.folio.bulkops.service.SubjectHelper;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SubjectListConverter extends BaseConverter<List<Subject>> {

  @Override
  public String convertToString(List<Subject> object) {
    return ObjectUtils.isEmpty(object) ?
      EMPTY :
      SUBJECT_HEADINGS +
        object.stream()
          .filter(Objects::nonNull)
          .map(SubjectHelper.service()::subjectToString)
          .collect(Collectors.joining(SPECIAL_ITEM_DELIMITER));
  }

}
