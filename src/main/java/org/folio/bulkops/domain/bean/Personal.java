package org.folio.bulkops.domain.bean;

import static org.folio.bulkops.domain.dto.DataType.DATE_TIME;

import java.util.Date;
import java.util.List;

import org.folio.bulkops.domain.converter.AddressesConverter;
import org.folio.bulkops.domain.converter.DateWithoutTimeConverter;
import org.folio.bulkops.domain.converter.PreferredContactTypeIdConverter;
import org.folio.bulkops.domain.converter.StringConverter;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvCustomBindByPosition;

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
public class Personal {
  @JsonProperty("lastName")
  @CsvCustomBindByName(column = "Last name", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 9, converter = StringConverter.class)
  @UnifiedTableCell
  private String lastName;

  @JsonProperty("firstName")
  @CsvCustomBindByName(column = "First name", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 10, converter = StringConverter.class)
  @UnifiedTableCell
  private String firstName;

  @JsonProperty("middleName")
  @CsvCustomBindByName(column = "Middle name", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 11, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String middleName;

  @JsonProperty("preferredFirstName")
  @CsvCustomBindByName(column = "Preferred first name", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 12, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String preferredFirstName;

  @JsonProperty("email")
  @CsvCustomBindByName(column = "Email", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 13, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String email;

  @JsonProperty("phone")
  @CsvCustomBindByName(column = "Phone", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 14, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String phone;

  @JsonProperty("mobilePhone")
  @CsvCustomBindByName(column = "Mobile phone", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 15, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String mobilePhone;

  @JsonProperty("dateOfBirth")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @CsvCustomBindByName(column = "Date Of Birth", converter = DateWithoutTimeConverter.class)
  @CsvCustomBindByPosition(position = 16, converter = DateWithoutTimeConverter.class)
  @UnifiedTableCell(dataType = DATE_TIME, visible = false)
  private Date dateOfBirth;

  @JsonProperty("addresses")
  @Valid
  @CsvCustomBindByName(column = "Addresses", converter = AddressesConverter.class)
  @CsvCustomBindByPosition(position = 17, converter = AddressesConverter.class)
  @UnifiedTableCell(visible = false)
  private List<Address> addresses;

  @JsonProperty("preferredContactTypeId")
  @CsvCustomBindByName(column = "Preferred contact type id", converter = PreferredContactTypeIdConverter.class)
  @CsvCustomBindByPosition(position = 18, converter = PreferredContactTypeIdConverter.class)
  @UnifiedTableCell(visible = false)
  private String preferredContactTypeId;

  @JsonProperty("profilePictureLink")
  @CsvCustomBindByName(column = "Link to the profile picture", converter = StringConverter.class)
  @CsvCustomBindByPosition(position = 19, converter = StringConverter.class)
  @UnifiedTableCell(visible = false)
  private String profilePictureLink;
}
