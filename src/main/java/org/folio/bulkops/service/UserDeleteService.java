package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.util.Constants.OPERATION_UPDATING_STEP;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.processor.UserDeleteProcessor;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.RemoteStorageUtils;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserDeleteService {
  private static final String QUERY_FILENAME_TEMPLATE = "%s/%s.fql";

  private final UserDeleteProcessor userDeleteProcessor;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final ErrorService errorService;
  private final BulkOperationRepository bulkOperationRepository;
  private final ObjectMapper objectMapper;
  private final BulkOperationServiceHelper bulkOperationServiceHelper;

  public void deleteUsers(BulkOperation bulkOperation) {
    log.info("Deleting users for bulk operation {}", bulkOperation.getId());

    String errorMessage;
    if (isNotEmpty(bulkOperation.getLinkToMatchedRecordsJsonFile())) {
      try (var userReader =
          new InputStreamReader(
              new BufferedInputStream(
                  RemoteStorageUtils.downloadToInputStream(
                      remoteFileSystemClient, bulkOperation.getLinkToMatchedRecordsJsonFile())))) {
        var iterator = objectMapper.readValues(objectMapper.createParser(userReader), User.class);
        while (iterator.hasNext()) {
          var user = iterator.next();
          deleteUser(bulkOperation, user);
          bulkOperation.setCommittedNumOfRecords(bulkOperation.getCommittedNumOfRecords() + 1);
          if (bulkOperation.getCommittedNumOfRecords() % OPERATION_UPDATING_STEP == 0) {
            bulkOperationRepository.save(bulkOperation);
          }
        }
        bulkOperation.setLinkToTriggeringQueryFile(saveFqlQuery(bulkOperation));
        bulkOperationServiceHelper.completeBulkOperation(bulkOperation);
      } catch (Exception e) {
        log.error("Error deleting users", e);
        errorMessage = e.getMessage();
        bulkOperationServiceHelper.failBulkOperation(bulkOperation, errorMessage);
      }
    } else {
      errorMessage = "File with matched records does not exist.";
      bulkOperationServiceHelper.failBulkOperation(bulkOperation, errorMessage);
    }
  }

  private void deleteUser(BulkOperation bulkOperation, User user) {
    try {
      userDeleteProcessor.delete(user);
    } catch (Exception e) {
      errorService.saveError(bulkOperation.getId(), user.getId(), e.getMessage(), ErrorType.ERROR);
    }
  }

  private String saveFqlQuery(BulkOperation bulkOperation) throws IOException {
    if (isNotEmpty(bulkOperation.getFqlQuery())
        && isNotEmpty(bulkOperation.getLinkToTriggeringCsvFile())) {
      var queryFilename =
          QUERY_FILENAME_TEMPLATE.formatted(
              bulkOperation.getId(),
              FilenameUtils.getBaseName(bulkOperation.getLinkToTriggeringCsvFile()));
      try (Writer queryFileWriter = remoteFileSystemClient.writer(queryFilename)) {
        queryFileWriter.write(bulkOperation.getUserFriendlyQuery());
        return queryFilename;
      }
    }
    return null;
  }
}
