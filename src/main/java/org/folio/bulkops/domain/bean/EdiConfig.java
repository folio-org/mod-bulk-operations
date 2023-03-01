package org.folio.bulkops.domain.bean;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.validation.Valid;
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
public class EdiConfig   {
  @JsonProperty("accountNoList")
  @Valid
  private List<String> accountNoList = null;

  @JsonProperty("defaultAcquisitionMethods")
  @Valid
  private List<UUID> defaultAcquisitionMethods = null;

  @JsonProperty("ediNamingConvention")
  private String ediNamingConvention;

  @JsonProperty("libEdiCode")
  private String libEdiCode;

  /**
   * The library type for this EDI
   */
  public enum LibEdiTypeEnum {
    _014_EAN("014/EAN"),

    _31B_US_SAN("31B/US-SAN"),

    _091_VENDOR_ASSIGNED("091/Vendor-assigned"),

    _092_CUSTOMER_ASSIGNED("092/Customer-assigned");

    private String value;

    LibEdiTypeEnum(String value) {
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
    public static LibEdiTypeEnum fromValue(String value) {
      for (LibEdiTypeEnum b : LibEdiTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("libEdiType")
  private LibEdiTypeEnum libEdiType;

  @JsonProperty("vendorEdiCode")
  private String vendorEdiCode;

  /**
   * The library type for this EDI
   */
  public enum VendorEdiTypeEnum {
    _014_EAN("014/EAN"),

    _31B_US_SAN("31B/US-SAN"),

    _091_VENDOR_ASSIGNED("091/Vendor-assigned"),

    _092_CUSTOMER_ASSIGNED("092/Customer-assigned");

    private String value;

    VendorEdiTypeEnum(String value) {
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
    public static VendorEdiTypeEnum fromValue(String value) {
      for (VendorEdiTypeEnum b : VendorEdiTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("vendorEdiType")
  private VendorEdiTypeEnum vendorEdiType;

  @JsonProperty("notes")
  private String notes;

  @JsonProperty("sendAccountNumber")
  private Boolean sendAccountNumber = false;

  @JsonProperty("supportOrder")
  private Boolean supportOrder = false;

  @JsonProperty("supportInvoice")
  private Boolean supportInvoice = false;
}

