package org.folio.bulkops.service;

import static org.folio.bulkops.domain.dto.OperationStatusType.CANCELLED;
import static org.folio.bulkops.domain.dto.OperationStatusType.FAILED;
import static org.folio.bulkops.domain.dto.OperationStatusType.RETRIEVING_IDENTIFIERS;
import static org.folio.bulkops.domain.dto.OperationStatusType.SAVED_IDENTIFIERS;
import static org.folio.bulkops.util.Constants.ERROR_COMMITTING_FILE_NAME_PREFIX;
import static org.folio.bulkops.util.Constants.NEW_LINE_SEPARATOR;
import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.QueryClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.querytool.domain.dto.QueryDetails;
import org.folio.querytool.domain.dto.SubmitQuery;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Log4j2
@RequiredArgsConstructor
public class QueryService {
  public static final String QUERY_FILENAME_TEMPLATE = "%1$s/Query-%1$s.csv";

  private final QueryClient queryClient;
  private final BulkOperationRepository bulkOperationRepository;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final ErrorService errorService;

  private final ExecutorService executor = Executors.newCachedThreadPool();

  public UUID executeQuery(SubmitQuery submitQuery) {
    return queryClient.executeQuery(submitQuery).getQueryId();
  }

  public QueryDetails getResult(UUID queryId) {
    return queryClient.getQuery(queryId, true);
  }

  public BulkOperation checkQueryExecutionStatus(BulkOperation bulkOperation, QueryDetails queryResult) {
    return switch (queryResult.getStatus()) {
      case SUCCESS -> {
        if (queryResult.getTotalRecords() == 0) {
          yield failBulkOperation(bulkOperation, "No records found for the query");
        }
        executor.execute(getRunnableWithCurrentFolioContext(() -> saveIdentifiers(bulkOperation, retrieveIdentifiers(queryResult))));
        bulkOperation.setStatus(RETRIEVING_IDENTIFIERS);
        yield bulkOperationRepository.save(bulkOperation);
      }
      case FAILED -> failBulkOperation(bulkOperation, queryResult.getFailureReason());
      case CANCELLED -> cancelBulkOperation((bulkOperation));
      case IN_PROGRESS -> bulkOperation;
    };
  }

  public void saveIdentifiers(BulkOperation bulkOperation, List<String> identifiers) {
    try {
      var identifiersString = String.join(NEW_LINE_SEPARATOR, identifiers);
      var path = String.format(QUERY_FILENAME_TEMPLATE, bulkOperation.getId());
      remoteFileSystemClient.put(new ByteArrayInputStream(identifiersString.getBytes()), path);
      bulkOperation.setLinkToTriggeringCsvFile(path);
      bulkOperation.setStatus(SAVED_IDENTIFIERS);
      bulkOperationRepository.save(bulkOperation);
    } catch (Exception e) {
      var errorMessage = "Failed to save identifiers, reason: " + e.getMessage();
      log.error(errorMessage);
      failBulkOperation(bulkOperation, errorMessage);
    }
  }

  private BulkOperation failBulkOperation(BulkOperation bulkOperation, String errorMessage) {
    bulkOperation.setStatus(FAILED);
    bulkOperation.setErrorMessage(errorMessage);
    bulkOperation.setEndTime(LocalDateTime.now());
    var linkToCommittingErrorsFile = errorService.uploadErrorsToStorage(bulkOperation.getId(), ERROR_COMMITTING_FILE_NAME_PREFIX, bulkOperation.getErrorMessage());
    bulkOperation.setLinkToCommittedRecordsErrorsCsvFile(linkToCommittingErrorsFile);
    return bulkOperationRepository.save(bulkOperation);
  }

  private BulkOperation cancelBulkOperation(BulkOperation bulkOperation) {
    bulkOperation.setStatus(CANCELLED);
    bulkOperation.setErrorMessage("Query execution was cancelled");
    bulkOperation.setEndTime(LocalDateTime.now());
    return bulkOperationRepository.save(bulkOperation);
  }

  private List<String> retrieveIdentifiers(QueryDetails queryDetails) {
    return queryDetails.getContent().stream().map(content -> content.get("instance.id").toString()).sorted().distinct().toList();
  }
}
