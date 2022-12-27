package org.folio.bulkops.processor;

import static java.lang.String.format;
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

import org.folio.bulkops.client.GroupClient;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.RuleValidationException;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@AllArgsConstructor
public class UserDataProcessor extends AbstractDataProcessor<User> {

  public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
  private final GroupClient groupClient;

  @Override
  public Validator<UpdateOptionType, Action> validator(User entity) {
    return (option, action) -> {
      if (EXPIRATION_DATE == option) {
        if (action.getType() != REPLACE_WITH) {
          throw new RuleValidationException(
              String.format("Action %s cannot be applied. The only REPLACE_WITH is supported.", action.getType()));
        } else if (isEmpty(action.getUpdated())) {
          throw new RuleValidationException("Updated value cannot be null or empty");
        }
      } else if (PATRON_GROUP == option) {
        if (REPLACE_WITH != action.getType()) {
          throw new RuleValidationException(
              String.format("Action %s cannot be applied to Patron group. The only REPLACE_WITH is supported.", action.getType()));
        } else if (isEmpty(action.getUpdated())) {
          throw new RuleValidationException("Updated value cannot be null or empty");
        } else if (isEmpty(groupClient.getGroupByQuery(String.format("group==\"%s\"", action.getUpdated()))
          .getUsergroups())) {
          throw new RuleValidationException(String.format("Non-existing patron group: %s", action.getUpdated()));
        }
      } else if (EMAIL_ADDRESS == option) {
        if (FIND_AND_REPLACE != action.getType()) {
          throw new RuleValidationException(String
            .format("Action %s cannot be applied to Email address. The only FIND_AND_REPLACE is supported.", action.getType()));
        } else if (isEmpty(action.getInitial()) || isEmpty(action.getUpdated())) {
          throw new RuleValidationException("Initial or updated value cannot be empty");
        }
      }
    };
  }

  @Override
  public Updater<User> updater(UpdateOptionType option, Action action) {
    switch (option) {
    case PATRON_GROUP:
      return user -> user.setPatronGroup(action.getUpdated());
    case EXPIRATION_DATE:
      return user -> {
        Date date;
        try {
          date = new SimpleDateFormat(DATE_TIME_FORMAT).parse(action.getUpdated());
        } catch (ParseException e) {
          throw new BulkOperationException(
              String.format("Invalid date format: %s, expected yyyy-MM-dd'T'HH:mm:ss.SSSXXX", action.getUpdated()));
        }
        user.setExpirationDate(date);
        user.setActive(date.after(new Date()));
      };
    case EMAIL_ADDRESS:
      return user -> {
        var initial = action.getInitial();
        var updated = action.getUpdated();
        if (user.getPersonal()
          .getEmail()
          .contains(initial)) {
          var personal = user.getPersonal()
            .toBuilder()
            .build();
          personal.setEmail(user.getPersonal()
            .getEmail()
            .replace(initial, updated));
          user.setPersonal(personal);
        }
      };
    default:
      return user -> {
        throw new BulkOperationException(format("Combination %s and %s isn't supported yet", option, action.getType()));
      };
    }
  }

  @Override
  public User clone(User entity) {
    return entity.toBuilder()
      .build();
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
