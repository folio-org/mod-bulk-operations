package org.folio.bulkops.util;

public class ErrorCode {

  private ErrorCode() {}

  public static final String ERROR_MESSAGE_PATTERN = "%s : %s";
  public static final String ERROR_UPLOAD_IDENTIFIERS_S3_ISSUE = "error.mod-bulk-operations.not.upload.identifiers.s3.issue";
  public static final String ERROR_NOT_DOWNLOAD_ORIGIN_FILE_FROM_S3 = "error.mod-bulk-operations.not.download.file.from.s3";
  public static final String ERROR_NOT_CONFIRM_CHANGES_S3_ISSUE = "error.mod-bulk-operations.not.confirm.changes.s3.issue";
}
