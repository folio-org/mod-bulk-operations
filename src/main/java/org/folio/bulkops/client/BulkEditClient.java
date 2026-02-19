package org.folio.bulkops.client;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "bulk-edit")
public interface BulkEditClient {
  @PostExchange(value = "/{jobId}/upload", accept = MediaType.MULTIPART_FORM_DATA_VALUE)
  String uploadFile(@PathVariable UUID jobId, @RequestPart(value = "file") MultipartFile file);

  @PostExchange(value = "/{jobId}/start")
  void startJob(@PathVariable UUID jobId);
}
