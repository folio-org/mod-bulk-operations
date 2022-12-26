package org.folio.bulkops.processor;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.Personal;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.bean.UserGroup;
import org.folio.bulkops.domain.bean.UserGroupCollection;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import static org.folio.bulkops.domain.dto.UpdateActionType.FIND;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REPLACE;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateOptionType.EMAIL_ADDRESS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.EXPIRATION_DATE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PATRON_GROUP;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOCATION;
import static org.folio.bulkops.processor.UserDataProcessor.DATE_TIME_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

public class UserDataProcessorTest extends BaseTest {

  @Autowired
  DataProcessorFactory<User> factory;

  @MockBean
  private BulkOperationExecutionContentRepository bulkOperationExecutionContentRepository;

  public static final String IDENTIFIER = "345";

  @Test
  public void testUpdateUserWithInvalidData() {
    var processor = factory.getProcessorFromFactory(User.class);

    assertNull(processor.process(IDENTIFIER, new User(), rules(rule(EXPIRATION_DATE, FIND, null),
      rule(EXPIRATION_DATE, REPLACE_WITH, null),
      rule(EXPIRATION_DATE, REPLACE_WITH, "1234-43")
    )));

    var patronGroupName = "non-existed-patron-group";
    when(groupClient.getGroupByQuery(String.format("group==\"%s\"", patronGroupName))).thenReturn(new UserGroupCollection());
    assertNull(processor.process(IDENTIFIER, new User(), rules(
      rule(PATRON_GROUP, FIND, null),
      rule(PATRON_GROUP, REPLACE_WITH, null),
      rule(PATRON_GROUP, REPLACE_WITH, patronGroupName))));

    assertNull(processor.process(IDENTIFIER, new User(), rules(rule(EMAIL_ADDRESS, FIND, null),
      rule(EMAIL_ADDRESS, FIND_AND_REPLACE, "@mail", null),
      rule(EXPIRATION_DATE, REPLACE_WITH, null, "@gmail"))));

    assertNull(processor.process(IDENTIFIER, new User(), rules(rule(PERMANENT_LOCATION, FIND, null))));
  }

  @Test
  public void testUpdateUserWithValidData() throws ParseException {
    var date = "2023-12-08T23:59:59.000+00:00";

    var patronGroupName = "existed-patron-group";
    when(groupClient.getGroupByQuery(String.format("group==\"%s\"", patronGroupName))).thenReturn(new UserGroupCollection().withUsergroups(List.of(new UserGroup().withGroup(patronGroupName))));

    var user = new User().withPersonal(new Personal().withEmail("test@test.com"));

    var processor = factory.getProcessorFromFactory(User.class);
    var result = processor.process(IDENTIFIER, user, rules(rule(EXPIRATION_DATE, REPLACE_WITH, date),
      rule(PATRON_GROUP, REPLACE_WITH, patronGroupName),
      rule(EMAIL_ADDRESS, FIND_AND_REPLACE, "@test", "@mail")
    ));

    assertNotNull(result);
    assertEquals(new SimpleDateFormat(DATE_TIME_FORMAT).parse(date), result.getExpirationDate());
    assertEquals(patronGroupName, result.getPatronGroup());
    assertEquals(result.getPersonal().getEmail(), "test@mail.com");
  }
}
