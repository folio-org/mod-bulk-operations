package org.folio.bulkops.domain.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.folio.bulkops.domain.bean.LoanType;

public class LoanTypeDeserializer extends JsonDeserializer<LoanType> {
    @Override
    public LoanType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String uuidStr = p.getText();
        return new LoanType().withId(uuidStr);
    }
}
