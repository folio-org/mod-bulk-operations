package org.folio.bulkops.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.folio.querytool.domain.dto.QueryDetails;
import org.folio.querytool.domain.dto.QueryIdentifier;
import org.folio.querytool.domain.dto.SubmitQuery;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "query")
public interface QueryClient {

  @PostExchange("")
  QueryIdentifier executeQuery(@RequestBody SubmitQuery submitQuery);

  @GetExchange("/{queryId}")
  QueryDetails getQuery(@RequestHeader UUID queryId, @RequestParam Boolean includeResults);

  @GetExchange("/{queryId}?includeResults=true")
  QueryDetails getQuery(
      @RequestHeader UUID queryId, @RequestParam Integer offset, @RequestParam Integer limit);

  @GetExchange("/{queryId}/sortedIds")
  List<List<String>> getSortedIds(
      @RequestHeader UUID queryId, @RequestParam Integer offset, @RequestParam Integer limit);

  @PostExchange("/contents")
  List<Map<String, Object>> getContents(@RequestBody ContentsRequest contentsRequest);
}
