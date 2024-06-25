package org.folio.bulkops.service;

import lombok.extern.log4j.Log4j2;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.client.RemoteStorageWriter;
import org.marc4j.MarcStreamWriter;
import org.marc4j.marc.Record;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Log4j2
public class MarcRemoteStorageWriter extends RemoteStorageWriter {

  public MarcRemoteStorageWriter(String path, int size, FolioS3Client s3Client) {
    super(path, size, s3Client);
  }

  public void writeRecord(Record record) throws IOException {
    try (var byteArrayOutputStream = new ByteArrayOutputStream()) {
      var marcStreamWriter = new MarcStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8.name());
      marcStreamWriter.write(record);
      super.write(byteArrayOutputStream.toString());
    } catch (IOException e) {
      log.error(e.getMessage());
      throw e;
    }
  }
}
