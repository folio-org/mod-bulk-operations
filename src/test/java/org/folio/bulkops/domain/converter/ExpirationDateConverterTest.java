package org.folio.bulkops.domain.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;
import org.folio.bulkops.domain.bean.Locale;
import org.folio.bulkops.service.UserReferenceHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpirationDateConverterTest {

  private static final String UTC = "UTC";
  private static final String US_EASTERN = "America/New_York";

  private ExpirationDateConverter converter;
  private UserReferenceHelper helperMock;

  @BeforeEach
  void setUp() throws Exception {
    converter = new ExpirationDateConverter();
    helperMock = mock(UserReferenceHelper.class);
    injectStaticService(helperMock);
  }

  @AfterEach
  void tearDown() throws Exception {
    // Restore static field to null to avoid test pollution
    injectStaticService(null);
    // Reset ThreadLocal formatter timezone back to UTC so other tests are unaffected
    DateWithoutTimeConverter.DATE_WITHOUT_TIME_FORMAT.get().setTimeZone(TimeZone.getTimeZone(UTC));
  }

  /**
   * Case 1: locale has a valid timezone → formatter should use that timezone. Date "2024-03-15" at
   * start-of-day UTC must still format as "2024-03-15" when timezone is UTC.
   */
  @Test
  void convertToString_withValidTimezone_formatsWithGivenTimezone() {
    when(helperMock.getTenantLocale())
        .thenReturn(Locale.builder().locale("en").timezone(UTC).build());

    Date date = toDate("2024-03-15");
    String result = converter.convertToString(date);

    assertThat(result).isEqualTo("2024-03-15");
  }

  /**
   * Case 2: locale has a non-UTC timezone → the same instant may render as the previous date if the
   * local time is behind UTC midnight. "2024-03-15T00:00:00Z" is "2024-03-14T20:00:00" in
   * America/New_York (UTC-4 during DST).
   */
  @Test
  void convertToString_withNonUtcTimezone_formatsInThatTimezone() {
    when(helperMock.getTenantLocale())
        .thenReturn(Locale.builder().locale("en").timezone(US_EASTERN).build());

    // Instant = 2024-03-15T00:00:00 UTC
    Date date = toDate("2024-03-15");
    String result = converter.convertToString(date);

    // In America/New_York this UTC-midnight instant falls on 2024-03-14 (20:00 local)
    assertThat(result).isEqualTo("2024-03-14");
  }

  /**
   * Case 3: getTenantLocale() returns null → formatter falls back to its current default timezone
   * and still returns a non-null, non-empty formatted string.
   */
  @Test
  void convertToString_whenLocaleIsNull_usesDefaultTimezoneAndReturnsFormattedDate() {
    when(helperMock.getTenantLocale()).thenReturn(null);
    // Reset formatter to UTC so the assertion is deterministic
    DateWithoutTimeConverter.DATE_WITHOUT_TIME_FORMAT.get().setTimeZone(TimeZone.getTimeZone(UTC));

    Date date = toDate("2024-06-01");
    String result = converter.convertToString(date);

    assertThat(result).isNotNull().isNotEmpty().isEqualTo("2024-06-01");
  }

  /**
   * Case 4: locale exists but timezone field is null → formatter falls back to its current default
   * timezone and still returns a valid formatted string.
   */
  @Test
  void convertToString_whenTimezoneIsNull_usesDefaultTimezoneAndReturnsFormattedDate() {
    when(helperMock.getTenantLocale())
        .thenReturn(Locale.builder().locale("en").timezone(null).build());
    DateWithoutTimeConverter.DATE_WITHOUT_TIME_FORMAT.get().setTimeZone(TimeZone.getTimeZone(UTC));

    Date date = toDate("2024-06-01");
    String result = converter.convertToString(date);

    assertThat(result).isNotNull().isNotEmpty().isEqualTo("2024-06-01");
  }

  // --- convertToObject tests ---

  /**
   * Case: locale has a valid UTC timezone → "2024-03-15" is parsed as 2024-03-15T00:00:00Z
   * (start-of-day in UTC).
   */
  @Test
  void convertToObject_withUtcTimezone_parsesAsStartOfDayUtc() {
    when(helperMock.getTenantLocale())
        .thenReturn(Locale.builder().locale("en").timezone(UTC).build());

    Date result = converter.convertToObject("2024-03-15");

    Instant expected = LocalDate.of(2024, 3, 15).atStartOfDay(ZoneId.of(UTC)).toInstant();
    assertThat(result).isEqualTo(Date.from(expected));
  }

  /**
   * Case: locale has a non-UTC timezone (America/New_York, UTC-4 during DST) → "2024-03-15" is
   * parsed as 2024-03-15T04:00:00Z (start-of-day in that zone).
   */
  @Test
  void convertToObject_withNonUtcTimezone_parsesAsStartOfDayInThatTimezone() {
    when(helperMock.getTenantLocale())
        .thenReturn(Locale.builder().locale("en").timezone(US_EASTERN).build());

    Date result = converter.convertToObject("2024-03-15");

    // America/New_York is UTC-4 during DST → midnight local = 04:00 UTC
    Instant expected = LocalDate.of(2024, 3, 15).atStartOfDay(ZoneId.of(US_EASTERN)).toInstant();
    assertThat(result).isEqualTo(Date.from(expected));
    // Confirm it is NOT the same as the UTC interpretation
    Instant utcMidnight = LocalDate.of(2024, 3, 15).atStartOfDay(ZoneId.of(UTC)).toInstant();
    assertThat(result.toInstant()).isNotEqualTo(utcMidnight);
  }

  /**
   * Case: getTenantLocale() returns null → code logs an error and falls back to UTC_ZONE, so
   * "2024-06-01" becomes 2024-06-01T00:00:00Z.
   */
  @Test
  void convertToObject_whenLocaleIsNull_fallsBackToUtcZone() {
    when(helperMock.getTenantLocale()).thenReturn(null);

    Date result = converter.convertToObject("2024-06-01");

    Instant expected = LocalDate.of(2024, 6, 1).atStartOfDay(ZoneId.of(UTC)).toInstant();
    assertThat(result).isEqualTo(Date.from(expected));
  }

  /**
   * Case: locale exists but its timezone field is null → code logs an error and falls back to
   * UTC_ZONE, so "2024-06-01" becomes 2024-06-01T00:00:00Z.
   */
  @Test
  void convertToObject_whenTimezoneIsNull_fallsBackToUtcZone() {
    when(helperMock.getTenantLocale())
        .thenReturn(Locale.builder().locale("en").timezone(null).build());

    Date result = converter.convertToObject("2024-06-01");

    Instant expected = LocalDate.of(2024, 6, 1).atStartOfDay(ZoneId.of(UTC)).toInstant();
    assertThat(result).isEqualTo(Date.from(expected));
  }

  /**
   * Case: the input string does not match the expected "yyyy-MM-dd" format → DateTimeParseException
   * is thrown.
   */
  @Test
  void convertToObject_withInvalidDateFormat_throwsException() {
    when(helperMock.getTenantLocale())
        .thenReturn(Locale.builder().locale("en").timezone(UTC).build());

    assertThatThrownBy(() -> converter.convertToObject("15-03-2024")).isInstanceOf(Exception.class);
  }

  // --- helpers ---

  private static Date toDate(String localDate) {
    return Date.from(
        LocalDate.parse(localDate)
            .atStartOfDay(ZoneId.of(ExpirationDateConverterTest.UTC))
            .toInstant());
  }

  private static void injectStaticService(UserReferenceHelper value) throws Exception {
    Field field = UserReferenceHelper.class.getDeclaredField("service");
    field.setAccessible(true);
    field.set(null, value);
  }
}
