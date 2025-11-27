package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EdiFtp {
  public enum FtpConnModeEnum {
    ACTIVE("Active"),

    PASSIVE("Passive");

    private final String value;

    FtpConnModeEnum(String value) {
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
    public static FtpConnModeEnum fromValue(String value) {
      for (FtpConnModeEnum b : FtpConnModeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("ftpConnMode")
  private FtpConnModeEnum ftpConnMode;

  /** The FTP format for this EDI. */
  public enum FtpFormatEnum {
    SFTP("SFTP"),

    FTP("FTP");

    private final String value;

    FtpFormatEnum(String value) {
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
    public static FtpFormatEnum fromValue(String value) {
      for (FtpFormatEnum b : FtpFormatEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("ftpFormat")
  private FtpFormatEnum ftpFormat;

  /** The FTP mode for this EDI. */
  public enum FtpModeEnum {
    ASCII("ASCII"),

    BINARY("Binary");

    private final String value;

    FtpModeEnum(String value) {
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
    public static FtpModeEnum fromValue(String value) {
      for (FtpModeEnum b : FtpModeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("ftpMode")
  private FtpModeEnum ftpMode;

  @JsonProperty("ftpPort")
  private Integer ftpPort;

  @JsonProperty("invoiceDirectory")
  private String invoiceDirectory;

  @JsonProperty("isPrimaryTransmissionMethod")
  private Boolean isPrimaryTransmissionMethod;

  @JsonProperty("notes")
  private String notes;

  @JsonProperty("orderDirectory")
  private String orderDirectory;

  @JsonProperty("password")
  private String password;

  @JsonProperty("serverAddress")
  private String serverAddress;

  @JsonProperty("username")
  private String username;
}
