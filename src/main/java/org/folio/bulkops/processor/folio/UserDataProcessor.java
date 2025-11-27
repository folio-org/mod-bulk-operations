package org.folio.bulkops.processor.folio;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REPLACE;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateOptionType.EMAIL_ADDRESS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.EXPIRATION_DATE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PATRON_GROUP;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.RuleValidationException;
import org.folio.bulkops.processor.FolioAbstractDataProcessor;
import org.folio.bulkops.processor.Updater;
import org.folio.bulkops.processor.Validator;
import org.folio.bulkops.service.UserReferenceService;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@AllArgsConstructor
public class UserDataProcessor extends FolioAbstractDataProcessor<User> {

  public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
  private final UserReferenceService userReferenceService;

  @Override
  public Validator<UpdateOptionType, Action, BulkOperationRule> validator(User entity) {
    return (option, action, rule) -> {
      if (EXPIRATION_DATE == option) {
        if (action.getType() != REPLACE_WITH) {
          throw new RuleValidationException(
              String.format(
                  "Action %s cannot be applied. The only REPLACE_WITH is supported.",
                  action.getType()));
        } else if (isEmpty(action.getUpdated())) {
          throw new RuleValidationException("Updated value cannot be null or empty");
        }
      } else if (PATRON_GROUP == option) {
        if (REPLACE_WITH != action.getType()) {
          throw new RuleValidationException(
              String.format(
                  "Action %s cannot be applied to Patron group. "
                      + "The only REPLACE_WITH is supported.",
                  action.getType()));
        } else if (isEmpty(action.getUpdated())) {
          throw new RuleValidationException("Updated value cannot be null or empty");
        }
      } else if (EMAIL_ADDRESS == option) {
        if (FIND_AND_REPLACE != action.getType()) {
          throw new RuleValidationException(
              String.format(
                  "Action %s cannot be applied to Email address. "
                      + "The only FIND_AND_REPLACE is supported.",
                  action.getType()));
        } else if (isEmpty(action.getInitial()) || isEmpty(action.getUpdated())) {
          throw new RuleValidationException("Initial or updated value cannot be empty");
        }
      } else {
        throw new RuleValidationException(
            String.format("Rule option %s is not supported for user", option.getValue()));
      }
    };
  }

  @Override
  public Updater<User> updater(
      UpdateOptionType option, Action action, User entity, boolean forPreview) {
    return switch (option) {
      case PATRON_GROUP -> user -> user.setPatronGroup(action.getUpdated());
      case EXPIRATION_DATE ->
          user -> {
            Date date;
            try {
              date = new SimpleDateFormat(DATE_TIME_FORMAT).parse(action.getUpdated());
            } catch (ParseException e) {
              throw new BulkOperationException(
                  String.format(
                      "Invalid date format: %s, expected yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                      action.getUpdated()));
            }
            user.setExpirationDate(date);
            user.setActive(date.after(new Date()));
          };
      case EMAIL_ADDRESS ->
          user -> {
            var initial = action.getInitial();
            var updated = action.getUpdated();
            if (isNull(user.getPersonal()) || isNull(user.getPersonal().getEmail())) {
              throw new BulkOperationException("Email is null");
            }
            if (user.getPersonal().getEmail().contains(initial)) {
              var personal = user.getPersonal().toBuilder().build();
              personal.setEmail(user.getPersonal().getEmail().replace(initial, updated));
              user.setPersonal(personal);
            }
          };
      default -> user -> {};
    };
  }

  @Override
  public User clone(User entity) {
    return entity.toBuilder().build();
  }

  @Override
  public boolean compare(User first, User second) {
    return Objects.equals(first, second);
  }

  @Override
  public Class<User> getProcessedType() {
    return User.class;
  }
}
