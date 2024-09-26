package org.folio.bulkops.domain.bean;

import java.util.List;

public record AssembleStorageFileRequestBody(String uploadId, String key, List<String> tags) {}
