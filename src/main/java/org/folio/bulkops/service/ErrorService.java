package org.folio.bulkops.service;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.folio.bulkops.domain.dto.ErrorParameterName.IDENTIFIER;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationError;
import org.folio.bulkops.repository.BulkOperationErrorRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.repository.RemoteFileSystemRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.folio.bulkops.domain.dto.Error;
import org.folio.bulkops.domain.dto.Parameter;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ErrorService {
  private final BulkOperationRepository operationRepository;
  private final BulkOperationErrorRepository errorRepository;
  private final RemoteFileSystemRepository remoteFileSystemRepository;
  public void saveError(UUID bulkOperationId, Error error) {
    errorRepository.save(BulkOperationError.builder().bulkOperationId(bulkOperationId).error(error).build());
  }

  public List<Error> getErrors(UUID bulkOperationId, int limit) {
    return errorRepository.findAllByBulkOperationId(bulkOperationId, PageRequest.of(0, limit))
      .map(BulkOperationError::getError)
      .toList();
  }

  public String uploadErrorsToStorage(UUID bulkOperationId) {
    var errors = errorRepository.findAllByBulkOperationId(bulkOperationId, PageRequest.of(0, Integer.MAX_VALUE));
    if (!errors.isEmpty()) {
      var errorsString = errors.stream()
        .map(BulkOperationError::getError)
        .filter(Objects::nonNull)
        .map(this::errorToCsvLine)
        .collect(Collectors.joining(LF));
      var errorsFileName = LocalDate.now().format(ISO_LOCAL_DATE) + operationRepository.findById(bulkOperationId)
        .map(BulkOperation::getLinkToOriginFile)
        .map(FilenameUtils::getName)
        .map(fileName -> "-Errors-" + fileName)
        .orElse("-Errors.csv");
      return remoteFileSystemRepository.put(new ByteArrayInputStream(errorsString.getBytes()), bulkOperationId + "/" + errorsFileName);
    }
    return EMPTY;
  }

  private String errorToCsvLine(Error error) {
    return error.getParameters().stream()
      .filter(parameter -> IDENTIFIER.getValue().equals(parameter.getKey()))
      .findFirst()
      .map(Parameter::getValue)
      .orElse("n/a") + "," + error.getMessage();
  }
}
