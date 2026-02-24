package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.AssembleStorageFileRequestBody;
import org.folio.bulkops.domain.bean.SplitStatus;
import org.folio.bulkops.domain.bean.UploadFileDefinition;
import org.folio.bulkops.domain.bean.UploadFileDefinitionProcessFiles;
import org.folio.bulkops.domain.bean.UploadUrlResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "data-import")
public interface DataImportClient {
  @GetExchange(value = "/splitStatus", accept = MediaType.APPLICATION_JSON_VALUE)
  SplitStatus getSplitStatus();

  @GetExchange(value = "/uploadUrl", accept = MediaType.APPLICATION_JSON_VALUE)
  UploadUrlResponse getUploadUrl(@RequestParam("filename") String filename);

  @PostExchange(value = "/uploadDefinitions", accept = MediaType.APPLICATION_JSON_VALUE)
  UploadFileDefinition postUploadDefinition(@RequestBody UploadFileDefinition uploadFileDefinition);

  @GetExchange(
      value = "/uploadDefinitions/{uploadDefinitionId}",
      accept = MediaType.APPLICATION_JSON_VALUE)
  UploadFileDefinition getUploadDefinitionById(@PathVariable String uploadDefinitionId);

  @PostExchange(value = "/uploadDefinitions/{uploadDefinitionId}/processFiles")
  void uploadFileDefinitionsProcessFiles(
      @RequestBody UploadFileDefinitionProcessFiles uploadFileDefinitionProcessFiles,
      @PathVariable String uploadDefinitionId);

  @PostExchange(
      value = "/uploadDefinitions/{uploadDefinitionId}/files/{fileId}/assembleStorageFile")
  void assembleStorageFile(
      @PathVariable String uploadDefinitionId,
      @PathVariable String fileId,
      @RequestBody AssembleStorageFileRequestBody body);
}
