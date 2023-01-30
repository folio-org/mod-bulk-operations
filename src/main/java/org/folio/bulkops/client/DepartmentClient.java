package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.Department;
import org.folio.bulkops.domain.bean.DepartmentCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "departments", configuration = FeignClientConfiguration.class)
public interface DepartmentClient {

  @GetMapping(value = "/{deptId}", produces = MediaType.APPLICATION_JSON_VALUE)
  Department getDepartmentById(@PathVariable String deptId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  DepartmentCollection getDepartmentByQuery(@RequestParam String query);
}
