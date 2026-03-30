package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.UploadFileDefinition;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "data-import")
public interface DataImportUploadClient {

  @PostExchange(
      value = "/uploadDefinitions/{uploadDefinitionId}/files/{fileId}",
      contentType = MediaType.APPLICATION_JSON_VALUE,
      accept = MediaType.APPLICATION_JSON_VALUE)
  UploadFileDefinition uploadFileDefinitionsFiles(
      @PathVariable String uploadDefinitionId,
      @PathVariable String fileId,
      @RequestBody byte[] bytes);
}
