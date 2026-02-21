package org.folio.bulkops.util;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.folio.bulkops.util.Constants.LINE_BREAK;

import java.util.Objects;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.exception.ExceptionUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

@UtilityClass
public class ExceptionHelper {
  public static String fetchMessage(Throwable throwable) {
    var cause = ExceptionUtils.getRootCause(throwable);
    if (cause instanceof InvalidFormatException ife) {
      var path =
          ife.getPath().stream()
              .map(JacksonException.Reference::getPropertyName)
              .filter(Objects::nonNull)
              .collect(Collectors.joining("."));
      return String.format(
          "Failed to parse %s from value \"%s\" in %s",
          ife.getTargetType().getSimpleName(), ife.getValue(), path);
    }
    return ExceptionUtils.getRootCauseMessage(throwable).replace(LINE_BREAK, SPACE);
  }
}
