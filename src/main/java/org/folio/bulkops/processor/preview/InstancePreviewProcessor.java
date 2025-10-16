package org.folio.bulkops.processor.preview;

import org.folio.bulkops.domain.bean.Instance;
import org.springframework.stereotype.Component;

@Component
public class InstancePreviewProcessor extends AbstractPreviewProcessor<Instance> {

  @Override
  public Class<Instance> getProcessedType() {
    return Instance.class;
  }
}
