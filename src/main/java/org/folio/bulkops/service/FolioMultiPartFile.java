package org.folio.bulkops.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

public class FolioMultiPartFile implements MultipartFile {

  private final String name;
  private final String originalFilename;
  private final String contentType;
  private final byte[] content;

  public FolioMultiPartFile(
    String name, String contentType, InputStream contentStream) throws IOException {

    this.name = name;
    this.originalFilename = name;
    this.contentType = contentType;
    this.content = FileCopyUtils.copyToByteArray(contentStream);
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  @NonNull
  public String getOriginalFilename() {
    return this.originalFilename;
  }

  @Override
  @Nullable
  public String getContentType() {
    return this.contentType;
  }

  @Override
  public boolean isEmpty() {
    return (this.content.length == 0);
  }

  @Override
  public long getSize() {
    return this.content.length;
  }

  @Override
  public byte[] getBytes() {
    return this.content;
  }

  @Override
  public InputStream getInputStream() {
    return new ByteArrayInputStream(this.content);
  }

  @Override
  public void transferTo(File dest) throws IOException, IllegalStateException {
    FileCopyUtils.copy(this.content, dest);
  }

}
