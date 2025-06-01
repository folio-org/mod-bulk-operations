package org.folio.bulkops.client;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.folio.bulkops.domain.bean.fqm.ContentRequest;
import org.folio.querytool.domain.dto.QueryDetails;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "query")
public interface QueryClient {

  @GetMapping("/{queryId}?includeResults=false")
  QueryDetails getQuery(@RequestHeader UUID queryId);

  @GetMapping("/{queryId}?includeResults=true")
  QueryDetails getQuery(@RequestHeader UUID queryId, @RequestParam Integer offset, @RequestParam Integer limit);

  @PostMapping("/contents")
  List<Map<String, Object>> getContents(@RequestBody @Valid ContentRequest contentsRequest);

}
