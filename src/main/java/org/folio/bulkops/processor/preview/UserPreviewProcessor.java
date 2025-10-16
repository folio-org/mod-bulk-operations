package org.folio.bulkops.processor.preview;

import org.folio.bulkops.domain.bean.User;
import org.springframework.stereotype.Component;

@Component
public class UserPreviewProcessor extends AbstractPreviewProcessor<User> {

  @Override
  public Class<User> getProcessedType() {
    return User.class;
  }
}
