package org.folio.bulkops.client;

import org.folio.bulkops.configs.DataImportFeignConfig;
import org.folio.bulkops.domain.bean.UploadFileDefinition;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "data-import-upload", url = "http://data-import",
        configuration = DataImportFeignConfig.class)
public interface DataImportUploadClient {

  @PostMapping(
      value = "/uploadDefinitions/{uploadDefinitionId}/files/{fileId}",
      consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
      headers = {"Content-Type: application/octet-stream"}
  )
  UploadFileDefinition uploadFileDefinitionsFiles(@PathVariable String uploadDefinitionId,
      @PathVariable String fileId, @RequestBody byte[] bytes);

}
