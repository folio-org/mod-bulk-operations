package org.folio.bulkops.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum JobStatus {

  SCHEDULED("SCHEDULED"),

  IN_PROGRESS("IN_PROGRESS"),

  SUCCESSFUL("SUCCESSFUL"),

  FAILED("FAILED");

  private String value;

  JobStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static JobStatus fromValue(String value) {
    for (JobStatus jobStatus : JobStatus.values()) {
      if (jobStatus.value.equals(value)) {
        return jobStatus;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
