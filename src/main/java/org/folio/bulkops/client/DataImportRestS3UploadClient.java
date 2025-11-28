package org.folio.bulkops.client;

import java.net.URI;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DataImportRestS3UploadClient {
  public ResponseEntity<String> uploadFile(String presignedUrl, byte[] fileContent) {
    return new RestTemplate()
        .exchange(
            URI.create(presignedUrl), HttpMethod.PUT, new HttpEntity<>(fileContent), String.class);
  }
}
