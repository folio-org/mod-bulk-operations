package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AddressTypeCollection   {
  @JsonProperty("addressTypes")
  @Valid
  private List<AddressType> addressTypes = new ArrayList<>();

  @JsonProperty("totalRecords")
  private Integer totalRecords;
}

