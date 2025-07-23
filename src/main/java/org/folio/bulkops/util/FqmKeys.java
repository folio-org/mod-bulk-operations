package org.folio.bulkops.util;

import lombok.experimental.UtilityClass;

/*
  * This class contains json keys used in FQM Query Response.
 */
@UtilityClass
public class FqmKeys {

  public static final String FQM_INSTANCE_TITLE_KEY = "instance.title";
  public static final String FQM_INSTANCES_TITLE_KEY = "instances.title";
  public static final String FQM_HOLDINGS_CALL_NUMBER_PREFIX_KEY = "holdings.call_number_prefix";
  public static final String FQM_HOLDINGS_CALL_NUMBER_KEY = "holdings.call_number";
  public static final String FQM_HOLDINGS_CALL_NUMBER_SUFFIX_KEY = "holdings.call_number_suffix";
  public static final String FQM_PERMANENT_LOCATION_NAME_KEY = "permanent_location.name";
  public static final String FQM_USERS_JSONB_KEY = "users.jsonb";
  public static final String FQM_ITEMS_JSONB_KEY = "items.jsonb";
}
