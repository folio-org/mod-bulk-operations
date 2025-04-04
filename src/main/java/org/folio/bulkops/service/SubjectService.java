package org.folio.bulkops.service;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.util.Constants.SPECIAL_ARRAY_DELIMITER;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.Subject;
import org.folio.bulkops.exception.EntityFormatException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class SubjectService {

  private static final String HYPHEN = "-";
  private static final int NUMBER_OF_SUBJECT_COMPONENTS = 3;
  private static final int SUBJECT_HEADINGS_INDEX = 0;
  private static final int SUBJECT_SOURCE_INDEX = 1;
  private static final int SUBJECT_TYPE_INDEX = 2;

  private final SubjectReferenceService subjectReferenceService;

  public Subject restoreSubjectItem(@NotNull String subject) {
    return restoreSubjectItem(subject, SPECIAL_ARRAY_DELIMITER);
  }

  public String subjectToString(Subject subject) {
    return subjectToString(subject, SPECIAL_ARRAY_DELIMITER);
  }

  private String subjectToString(Subject subject, String delimiter) {
    return String.join(delimiter,
      isEmpty(subject.getValue()) ? HYPHEN : subject.getValue(),
      isEmpty(subject.getSourceId()) ? HYPHEN : subjectReferenceService.getSubjectSourceNameById(subject.getSourceId(), null),
      isEmpty(subject.getTypeId()) ? HYPHEN : subjectReferenceService.getSubjectTypeNameById(subject.getTypeId(), null));
  }

  private Subject restoreSubjectItem(String subject, String delimiter) {
    if (isNotEmpty(subject)) {
      var tokens = subject.split(delimiter, -1);
      if (NUMBER_OF_SUBJECT_COMPONENTS == tokens.length) {
        return Subject.builder()
          .value(tokens[SUBJECT_HEADINGS_INDEX])
          .sourceId(subjectReferenceService.getSubjectSourceIdByName(tokens[SUBJECT_SOURCE_INDEX]))
          .typeId(subjectReferenceService.getSubjectTypeIdByName(tokens[SUBJECT_TYPE_INDEX]))
          .build();
      }
      throw new EntityFormatException(String.format("Illegal number of subject elements: %d, expected: %d", tokens.length, NUMBER_OF_SUBJECT_COMPONENTS));
    }
    return null;
  }
}
