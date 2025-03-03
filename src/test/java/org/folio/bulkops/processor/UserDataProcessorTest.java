package org.folio.bulkops.processor;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.Personal;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.bean.UserGroup;
import org.folio.bulkops.processor.folio.DataProcessorFactory;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.service.ErrorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.UUID;

import static java.util.Objects.isNull;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REPLACE;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateOptionType.EMAIL_ADDRESS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.EXPIRATION_DATE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PATRON_GROUP;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOCATION;
import static org.folio.bulkops.processor.folio.UserDataProcessor.DATE_TIME_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class UserDataProcessorTest extends BaseTest {

  @Autowired
  DataProcessorFactory factory;
   @MockitoBean
  ErrorService errorService;

  private FolioDataProcessor<User> processor;

   @MockitoBean
  private BulkOperationExecutionContentRepository bulkOperationExecutionContentRepository;

  public static final String IDENTIFIER = "345";

  @BeforeEach
  void setUp() {
    if (isNull(processor)) {
      processor = factory.getProcessorFromFactory(User.class);
    }
  }

  @Test
  void testUpdateUserWithInvalidData() {
    var actual = processor.process(IDENTIFIER, new User(), rules(rule(EXPIRATION_DATE, FIND, null),
      rule(EXPIRATION_DATE, REPLACE_WITH, null),
      rule(EXPIRATION_DATE, REPLACE_WITH, "1234-43")
    ));

    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);

    actual = processor.process(IDENTIFIER, new User(), rules(rule(EMAIL_ADDRESS, FIND, null),
      rule(EMAIL_ADDRESS, FIND_AND_REPLACE, "@mail", null),
      rule(EXPIRATION_DATE, REPLACE_WITH, null, "@gmail")));

    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);

    actual = processor.process(IDENTIFIER, new User(), rules(rule(PERMANENT_LOCATION, FIND, null)));

    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @Test
  void testUpdateUserWithValidData() throws ParseException {
    var date = "2023-12-08T23:59:59.000+00:00";

    var newPatronGroupId = UUID.randomUUID().toString();
    when(groupClient.getGroupById(newPatronGroupId)).thenReturn(new UserGroup().withId(newPatronGroupId));

    var user = new User().withPersonal(new Personal().withEmail("test@test.com"));

    var result = processor.process(IDENTIFIER, user, rules(rule(EXPIRATION_DATE, REPLACE_WITH, date),
      rule(PATRON_GROUP, REPLACE_WITH, newPatronGroupId),
      rule(EMAIL_ADDRESS, FIND_AND_REPLACE, "@test", "@mail")
    ));

    assertNotNull(result);
    assertEquals(new SimpleDateFormat(DATE_TIME_FORMAT).parse(date), result.getUpdated().getExpirationDate());
    assertEquals(newPatronGroupId, result.getUpdated().getPatronGroup());
    assertEquals("test@mail.com", result.getUpdated().getPersonal().getEmail());
  }
}
