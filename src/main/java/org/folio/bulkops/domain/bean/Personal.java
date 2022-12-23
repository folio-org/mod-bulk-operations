package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.springframework.format.annotation.DateTimeFormat;

import javax.annotation.Generated;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Personal {
  @JsonProperty("lastName")
  private String lastName;

  @JsonProperty("firstName")
  private String firstName;

  @JsonProperty("middleName")
  private String middleName;

  @JsonProperty("preferredFirstName")
  private String preferredFirstName;

  @JsonProperty("email")
  private String email;

  @JsonProperty("phone")
  private String phone;

  @JsonProperty("mobilePhone")
  private String mobilePhone;

  @JsonProperty("dateOfBirth")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Date dateOfBirth;

  @JsonProperty("addresses")
  @Valid
  private List<Address> addresses = null;

  @JsonProperty("preferredContactTypeId")
  private String preferredContactTypeId;
}

