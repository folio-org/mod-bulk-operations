package org.folio.bulkops.processor;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.client.GroupClient;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.RuleValidationException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Objects;

import static org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REPLACE;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateOptionType.EMAIL_ADDRESS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.EXPIRATION_DATE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PATRON_GROUP;


@Log4j2
@Component
@AllArgsConstructor
public class UserDataProcessor extends AbstractDataProcessor<User> {

  public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSX";
  private final GroupClient groupClient;

  @Override
  public Validator<UpdateOptionType, Action> validator(User entity) {
    return (option, action) -> {
      if (EXPIRATION_DATE == option) {
        if (action.getType() != REPLACE_WITH) {
          throw new RuleValidationException(String.format("Action %s cannot be applied to Expiration date. The only REPLACE_WITH is supported.", action.getType()));
        } else if (action.getUpdated() == null) {
          throw new RuleValidationException("Value cannot be empty");
        } else {
          try {
            LocalDateTime.parse(action.getUpdated(), DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
          } catch (DateTimeParseException e) {
            throw new RuleValidationException(String.format("Invalid date format: %s, expected yyyy-MM-dd HH:mm:ss.SSSX", action.getUpdated()));
          }
        }
      } else if (PATRON_GROUP == option) {
        if (REPLACE_WITH != action.getType()) {
          throw new RuleValidationException(String.format("Action %s cannot be applied to Patron group. The only REPLACE_WITH is supported.", action.getType()));
        } else if (ObjectUtils.isEmpty(action.getUpdated())) {
          throw new RuleValidationException("REPLACE_WITH value cannot be null or empty");
        } else if (groupClient.getGroupByQuery(String.format("group==\"%s\"", action.getUpdated())).getUsergroups().isEmpty()) {
          throw new RuleValidationException(String.format("Non-existing patron group: %s", action.getUpdated()));
        }
      } else if (EMAIL_ADDRESS == option) {
        if (FIND_AND_REPLACE != action.getType()) {
          throw new RuleValidationException(String.format("Action %s cannot be applied to Email address. The only FIND_AND_REPLACE is supported.", action.getType()));
        } else if (ObjectUtils.isEmpty(action.getInitial()) || ObjectUtils.isEmpty(action.getUpdated())) {
          throw new RuleValidationException(String.format("Action %s cannot be applied to Email address. The only FIND_AND_REPLACE is supported.", action.getType()));
        } else if (Objects.equals(action.getInitial(), action.getUpdated())) {
          throw new RuleValidationException("Initial and updated values cannot be equal");
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
          var date = new Date(action.getUpdated());
          user.setExpirationDate(date);
          user.setActive(date.after(new Date()));
        };
      case EMAIL_ADDRESS:
        return user -> {
          var from = action.getInitial();
          var to = action.getUpdated();
          if (user.getPersonal().getEmail().contains(from)) {
            user.getPersonal().setEmail(user.getPersonal().getEmail().replace(from, to));
          }
          throw new BulkOperationException("Email does not match find criteria");
        };
      default:
        return user -> {};
    }
  }

  @Override
  public User clone(User entity) {
    return entity.toBuilder().build();
  }

  @Override
  public boolean compare(User first, User second) {
    return Objects.equals(first, second);
  }
}
