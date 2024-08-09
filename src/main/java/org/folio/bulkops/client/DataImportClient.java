package org.folio.bulkops.client;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.UploadFileDefinition;
import org.folio.bulkops.domain.bean.UploadFileDefinitionProcessFiles;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "data-import", configuration = FeignClientConfiguration.class)
public interface DataImportClient {

  @PostMapping(value = "/uploadDefinitions", produces = MediaType.APPLICATION_JSON_VALUE)
  UploadFileDefinition uploadFileDefinitions(@RequestBody UploadFileDefinition uploadFileDefinition);

  @PostMapping(value = "/uploadDefinitions/{uploadDefinitionId}/files/{fileId}")
  void uploadFileDefinitionsFiles(@RequestBody UploadFileDefinition uploadFileDefinition, @PathVariable String uploadDefinitionId, @PathVariable String fileId);

  @PostMapping(value = "/uploadDefinitions/{uploadDefinitionId}/processFiles")
  void uploadFileDefinitionsProcessFiles(@RequestBody UploadFileDefinitionProcessFiles uploadFileDefinitionProcessFiles, @PathVariable String uploadDefinitionId);
}
