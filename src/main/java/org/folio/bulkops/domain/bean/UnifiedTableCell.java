package org.folio.bulkops.domain.bean;

import org.folio.bulkops.domain.dto.DataType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface UnifiedTableCell {
  DataType dataType() default DataType.STRING;
  boolean visible() default true;
}
