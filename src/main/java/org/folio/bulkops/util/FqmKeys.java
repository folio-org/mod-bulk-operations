package org.folio.bulkops.util;

import lombok.experimental.UtilityClass;

/*
  * This class contains json keys used in FQM Query Response.
 */
@UtilityClass
public class FqmKeys {
  public static final String FQM_DATE_OF_PUBLICATION_KEY = "dateOfPublication";
  public static final String FQM_PUBLISHER_KEY = "publisher";

  public static final String FQM_HOLDING_PERMANENT_LOCATION_NAME_KEY = "holding_permanent_location.name";
  public static final String FQM_HOLDINGS_CALL_NUMBER_KEY = "holdings.call_number";
  public static final String FQM_HOLDINGS_CALL_NUMBER_PREFIX_KEY = "holdings.call_number_prefix";
  public static final String FQM_HOLDINGS_CALL_NUMBER_SUFFIX_KEY = "holdings.call_number_suffix";

  public static final String FQM_INSTANCE_CHILD_INSTANCES_KEY = "instance.childInstances";
  public static final String FQM_INSTANCE_ID_KEY = "instance.id";
  public static final String FQM_INSTANCE_PARENT_INSTANCES_KEY = "instance.parentInstances";
  public static final String FQM_INSTANCE_PRECEDING_TITLES_KEY = "instance.precedingTitles";
  public static final String FQM_INSTANCE_PUBLICATION_KEY = "instance.publication";
  public static final String FQM_INSTANCE_SHARED_KEY = "instance.shared";
  public static final String FQM_INSTANCE_SUCCEEDING_TITLES_KEY = "instance.succeedingTitles";
  public static final String FQM_INSTANCE_TITLE_KEY = "instance.title";
  public static final String FQM_INSTANCES_PUBLICATION_KEY = "instances.publication";
  public static final String FQM_INSTANCES_TITLE_KEY = "instances.title";

  public static final String FQM_ITEMS_JSONB_KEY = "items.jsonb";

  public static final String FQM_USERS_JSONB_KEY = "users.jsonb";
  public static final String FQM_USERS_ID_KEY = "users.id";
  public static final String FQM_USERS_TYPE_KEY = "users.type";
}
