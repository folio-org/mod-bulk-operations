package org.folio.bulkops.domain.bean;

public enum BatchStatus {
  COMPLETED,
  STARTING,
  STARTED,
  STOPPING,
  STOPPED,
  FAILED,
  ABANDONED,
  UNKNOWN;
}
