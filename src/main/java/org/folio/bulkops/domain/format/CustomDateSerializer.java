package org.folio.bulkops.domain.format;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class CustomDateSerializer extends ValueSerializer<Date> {
  private static final SimpleDateFormat formatter =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

  static {
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  @Override
  public void serialize(Date value, JsonGenerator gen, SerializationContext ctxt)
      throws JacksonException {
    String formattedDate = formatter.format(value);
    if (formattedDate.endsWith("Z")) {
      formattedDate = formattedDate.replace("Z", "+00:00");
    }
    gen.writeString(formattedDate);
  }
}
