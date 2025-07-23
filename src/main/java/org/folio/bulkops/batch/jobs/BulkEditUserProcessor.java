package org.folio.bulkops.batch.jobs;

import static java.util.Objects.nonNull;
import static org.folio.bulkops.util.BulkEditProcessorHelper.dateToString;
import static org.folio.bulkops.util.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.bulkops.util.Constants.MIN_YEAR_FOR_BIRTH_DATE;
import static org.folio.bulkops.util.Constants.MULTIPLE_MATCHES_MESSAGE;
import static org.folio.bulkops.util.Constants.NO_MATCH_FOUND_MESSAGE;
import static org.folio.bulkops.util.Constants.NO_USER_VIEW_PERMISSIONS;

import feign.codec.DecodeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.batch.jobs.processidentifiers.DuplicationCheckerFactory;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.exception.BulkEditException;
import org.folio.bulkops.processor.EntityExtractor;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.util.ExceptionHelper;
import org.folio.spring.FolioExecutionContext;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditUserProcessor implements ItemProcessor<ItemIdentifier, User>, EntityExtractor {
  private static final String USER_SEARCH_QUERY = "(cql.allRecords=1 NOT type=\"\" or type<>\"shadow\") and %s==\"%s\"";

  private final UserClient userClient;
  private final DuplicationCheckerFactory duplicationCheckerFactory;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;
  @Value("#{stepExecution.jobExecution}")
  private JobExecution jobExecution;
  private final FolioExecutionContext folioExecutionContext;
  private final PermissionsValidator permissionsValidator;

  @Override
  public User process(ItemIdentifier itemIdentifier) throws BulkEditException {
    if (!permissionsValidator.isBulkEditReadPermissionExists(folioExecutionContext.getTenantId(), EntityType.USER)) {
      var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
      throw new BulkEditException(NO_USER_VIEW_PERMISSIONS.formatted(user.getUsername(), resolveIdentifier(identifierType), itemIdentifier.getItemId(), folioExecutionContext.getTenantId()), ErrorType.ERROR);
    }
    if (!duplicationCheckerFactory.getIdentifiersToCheckDuplication(jobExecution).add(itemIdentifier)) {
      throw new BulkEditException("Duplicate entry", ErrorType.WARNING);
    }
    try {
      var limit = 1;
      var userCollection = userClient.getByQuery(
        USER_SEARCH_QUERY.formatted(resolveIdentifier(identifierType), itemIdentifier.getItemId()),
        limit);

      if (userCollection.getUsers().isEmpty()) {
        throw new BulkEditException(NO_MATCH_FOUND_MESSAGE, ErrorType.ERROR);
      } else if (userCollection.getTotalRecords() > limit) {
        throw new BulkEditException(MULTIPLE_MATCHES_MESSAGE, ErrorType.ERROR);
      }
      var user = userCollection.getUsers().getFirst();
      var birthDate = user.getPersonal().getDateOfBirth();
      validateBirthDate(birthDate);
      return user;
    } catch (DecodeException e) {
      throw new BulkEditException(ExceptionHelper.fetchMessage(e), ErrorType.ERROR);
    }
  }

  private void validateBirthDate(Date birthDate) {
    if (nonNull(birthDate)) {
      var year = LocalDateTime.ofInstant(Instant.ofEpochMilli(birthDate.getTime()), ZoneOffset.UTC).getYear();
      if (year < MIN_YEAR_FOR_BIRTH_DATE) {
        throw new BulkEditException(String.format("Failed to parse Date from value \"%s\" in users.personal.dateOfBirth", dateToString(birthDate)), ErrorType.ERROR);
      }
    }
  }
}
