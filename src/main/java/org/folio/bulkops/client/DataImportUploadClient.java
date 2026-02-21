package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.UploadFileDefinition;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "http://data-import")
public interface DataImportUploadClient {

  @PostExchange(
      value = "/data-import-upload/uploadDefinitions/{uploadDefinitionId}/files/{fileId}",
      accept = MediaType.APPLICATION_OCTET_STREAM_VALUE,
      headers = {"Content-Type: application/octet-stream"})
  UploadFileDefinition uploadFileDefinitionsFiles(
      @PathVariable String uploadDefinitionId,
      @PathVariable String fileId,
      @RequestBody byte[] bytes);
}
