package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.Department;
import org.folio.bulkops.domain.bean.DepartmentCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "departments", accept = MediaType.APPLICATION_JSON_VALUE)
public interface DepartmentClient {

  @GetExchange(value = "/{deptId}")
  Department getDepartmentById(@PathVariable String deptId);

  @GetExchange
  DepartmentCollection getByQuery(@RequestParam String query);
}
