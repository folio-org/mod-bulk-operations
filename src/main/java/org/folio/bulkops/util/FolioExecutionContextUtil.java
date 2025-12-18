package org.folio.bulkops.util;

import java.util.HashMap;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;

@Log4j2
public class FolioExecutionContextUtil {

  private FolioExecutionContextUtil() {}

  public static FolioExecutionContext prepareContextForTenant(
      String tenantId, FolioModuleMetadata folioModuleMetadata, FolioExecutionContext context) {
    if (MapUtils.isNotEmpty(context.getOkapiHeaders())) {
      var headersCopy = new HashMap<>(context.getAllHeaders());
      headersCopy.put(XOkapiHeaders.TENANT, List.of(tenantId));
      log.debug("FOLIO context initialized with tenant {}", tenantId);
      return new DefaultFolioExecutionContext(folioModuleMetadata, headersCopy);
    }
    throw new IllegalStateException("Okapi headers not provided");
  }
}
