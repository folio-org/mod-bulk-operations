package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.AssembleStorageFileRequestBody;
import org.folio.bulkops.domain.bean.SplitStatus;
import org.folio.bulkops.domain.bean.UploadFileDefinition;
import org.folio.bulkops.domain.bean.UploadFileDefinitionProcessFiles;
import org.folio.bulkops.domain.bean.UploadUrlResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "data-import", configuration = FeignClientConfiguration.class)
public interface DataImportClient {
  @GetMapping(value = "/splitStatus", produces = MediaType.APPLICATION_JSON_VALUE)
  SplitStatus getSplitStatus();

  @GetMapping(value = "/uploadUrl", produces = MediaType.APPLICATION_JSON_VALUE)
  UploadUrlResponse getUploadUrl(@RequestParam("filename") String filename);

  @PostMapping(value = "/uploadDefinitions", produces = MediaType.APPLICATION_JSON_VALUE)
  UploadFileDefinition uploadFileDefinitions(@RequestBody UploadFileDefinition uploadFileDefinition);

  @GetMapping(value = "/uploadDefinitions/{uploadDefinitionId}", produces = MediaType.APPLICATION_JSON_VALUE)
  UploadFileDefinition getUploadDefinitionById(@PathVariable String uploadDefinitionId);

  @PostMapping(value = "/uploadDefinitions/{uploadDefinitionId}/processFiles")
  void uploadFileDefinitionsProcessFiles(@RequestBody UploadFileDefinitionProcessFiles uploadFileDefinitionProcessFiles, @PathVariable String uploadDefinitionId);

  @PostMapping(value = "/uploadDefinitions/{uploadDefinitionId}/files/{fileId}/assembleStorageFile")
  void assembleStorageFile(@PathVariable String uploadDefinitionId, @PathVariable String fileId, @RequestBody AssembleStorageFileRequestBody body);
}
