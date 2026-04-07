package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.Locale;
import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "locale", accept = MediaType.APPLICATION_JSON_VALUE)
public interface LocaleClient {

  @GetExchange
  Locale getTenantLocale();
}
