package org.folio.bulkops.domain.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Objects;

public class Progress {

  @JsonProperty("total")
  private Integer total = 0;

  @JsonProperty("processed")
  private Integer processed = 0;

  @JsonProperty("progress")
  private Integer progress = 0;

  @JsonProperty("success")
  private Integer success = 0;

  @JsonProperty("errors")
  private Integer errors = 0;

  public Progress total(Integer total) {
    this.total = total;
    return this;
  }

  /**
   * Total number of records being processed
   * minimum: 0
   *
   * @return total
   */
  @Min(0)
  @Schema(name = "total", description = "Total number of records being processed", required = false)
  public Integer getTotal() {
    return total;
  }

  public void setTotal(Integer total) {
    this.total = total;
  }

  public Progress processed(Integer processed) {
    this.processed = processed;
    return this;
  }

  /**
   * Current number of records already processed
   * minimum: 0
   *
   * @return processed
   */
  @Min(0)
  @Schema(name = "processed", description = "Current number of records already processed", required = false)
  public Integer getProcessed() {
    return processed;
  }

  public void setProcessed(Integer processed) {
    this.processed = processed;
  }

  public Progress progress(Integer progress) {
    this.progress = progress;
    return this;
  }

  /**
   * Current progress in %
   * minimum: 0
   * maximum: 100
   *
   * @return progress
   */
  @Min(0)
  @Max(100)
  @Schema(name = "progress", description = "Current progress in %", required = false)
  public Integer getProgress() {
    return progress;
  }

  public void setProgress(Integer progress) {
    this.progress = progress;
  }

  public Progress success(Integer success) {
    this.success = success;
    return this;
  }

  /**
   * Current number of success processed records
   * minimum: 0
   * maximum: 0
   *
   * @return success
   */
  @Min(0)
  @Max(0)
  @Schema(name = "success", description = "Current number of success processed records", required = false)
  public Integer getSuccess() {
    return success;
  }

  public void setSuccess(Integer success) {
    this.success = success;
  }

  public Progress errors(Integer errors) {
    this.errors = errors;
    return this;
  }

  /**
   * Current number of errors
   * minimum: 0
   * maximum: 0
   *
   * @return errors
   */
  @Min(0)
  @Max(0)
  @Schema(name = "errors", description = "Current number of errors", required = false)
  public Integer getErrors() {
    return errors;
  }

  public void setErrors(Integer errors) {
    this.errors = errors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Progress progress = (Progress) o;
    return Objects.equals(this.total, progress.total) &&
      Objects.equals(this.processed, progress.processed) &&
      Objects.equals(this.progress, progress.progress) &&
      Objects.equals(this.success, progress.success) &&
      Objects.equals(this.errors, progress.errors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(total, processed, progress, success, errors);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Progress {\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
    sb.append("    processed: ").append(toIndentedString(processed)).append("\n");
    sb.append("    progress: ").append(toIndentedString(progress)).append("\n");
    sb.append("    success: ").append(toIndentedString(success)).append("\n");
    sb.append("    errors: ").append(toIndentedString(errors)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
