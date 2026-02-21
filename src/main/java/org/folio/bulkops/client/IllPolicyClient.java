package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.IllPolicy;
import org.folio.bulkops.domain.bean.IllPolicyCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "ill-policies", accept = MediaType.APPLICATION_JSON_VALUE)
public interface IllPolicyClient {
  @GetExchange
  IllPolicyCollection getByQuery(@RequestParam String query);

  @GetExchange(value = "/{illPolicyId}")
  IllPolicy getById(@PathVariable String illPolicyId);
}
