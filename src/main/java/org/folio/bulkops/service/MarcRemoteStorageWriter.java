package org.folio.bulkops.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import lombok.extern.log4j.Log4j2;
import org.marc4j.MarcStreamWriter;
import org.marc4j.marc.Record;

@Log4j2
public class MarcRemoteStorageWriter extends StringWriter {

  private final Writer remoteStorageWriter;
  private final ByteArrayOutputStream byteArrayOutputStream;
  private final MarcStreamWriter marcStreamWriter;

  public MarcRemoteStorageWriter(Writer remoteStorageWriter) {
    this.remoteStorageWriter = remoteStorageWriter;
    byteArrayOutputStream = new ByteArrayOutputStream();
    marcStreamWriter = new MarcStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8.name());
  }

  public void writeRecord(Record marcRecord) throws IOException {
    marcStreamWriter.write(marcRecord);
    remoteStorageWriter.write(byteArrayOutputStream.toString(StandardCharsets.UTF_8));
    byteArrayOutputStream.reset();
  }

  @Override
  public void close() throws IOException {
    byteArrayOutputStream.flush();
    marcStreamWriter.close();
    remoteStorageWriter.close();
    byteArrayOutputStream.close();
  }
}
