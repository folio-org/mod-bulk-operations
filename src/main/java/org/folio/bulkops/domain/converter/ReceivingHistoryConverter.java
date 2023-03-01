package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.restore;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_PATTERN;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.folio.bulkops.domain.bean.ReceivingHistoryEntries;
import org.folio.bulkops.domain.bean.ReceivingHistoryEntry;
import org.folio.bulkops.exception.EntityFormatException;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

public class ReceivingHistoryConverter extends AbstractBeanField<String, ReceivingHistoryEntries> {
  private static final int NUMBER_OF_RECEIVING_HISTORY_ENTRY_ELEMENTS = 3;
  private static final int RECEIVING_HISTORY_ENTRY_PUBLIC_DISPLAY_INDEX = 0;
  private static final int RECEIVING_HISTORY_ENTRY_ENUMERATION_INDEX = 1;
  private static final int RECEIVING_HISTORY_ENTRY_CHRONOLOGY_INDEX = 2;

  @Override
  protected ReceivingHistoryEntries convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    if (isEmpty(value)) {
      return null;
    }
    var tokens = value.split(ITEM_DELIMITER_PATTERN);
    if (tokens.length > 1) {
      return ReceivingHistoryEntries.builder()
        .displayType(isEmpty(tokens[0]) ? null : tokens[0])
        .entries(Arrays.stream(tokens)
          .skip(1)
          .map(this::restoreReceivingHistoryEntry)
          .toList())
        .build();
    }
    throw new EntityFormatException("Invalid number of tokens in receiving history entries");
  }

  private ReceivingHistoryEntry restoreReceivingHistoryEntry(String entryString) {
    var tokens = entryString.split(ARRAY_DELIMITER);
    if (tokens.length == NUMBER_OF_RECEIVING_HISTORY_ENTRY_ELEMENTS) {
      return ReceivingHistoryEntry.builder()
        .publicDisplay(isEmpty(tokens[RECEIVING_HISTORY_ENTRY_PUBLIC_DISPLAY_INDEX]) ? null : Boolean.parseBoolean(tokens[RECEIVING_HISTORY_ENTRY_PUBLIC_DISPLAY_INDEX]))
        .enumeration(restore(tokens[RECEIVING_HISTORY_ENTRY_ENUMERATION_INDEX]))
        .chronology(restore(tokens[RECEIVING_HISTORY_ENTRY_CHRONOLOGY_INDEX]))
        .build();
    }
    throw new EntityFormatException(String.format("Invalid number of tokens in receiving history entry: %d, expected %d", tokens.length, NUMBER_OF_RECEIVING_HISTORY_ENTRY_ELEMENTS));
  }

  @Override
  protected String convertToWrite(Object value) {
    if (isEmpty(value)) {
      return EMPTY;
    }
    var entries = (ReceivingHistoryEntries) value;
    var displayType = isEmpty(entries.getDisplayType()) ? EMPTY : entries.getDisplayType();
    var entriesString = isEmpty(entries.getEntries()) ? EMPTY : entries.getEntries().stream()
      .map(this::receivingHistoryEntryToString)
      .collect(Collectors.joining(ITEM_DELIMITER));
    return String.join(ITEM_DELIMITER, displayType, entriesString);
  }

  private String receivingHistoryEntryToString(ReceivingHistoryEntry entry) {
    return String.join(ARRAY_DELIMITER,
      isEmpty(entry.getPublicDisplay()) ? EMPTY : Boolean.toString(entry.getPublicDisplay()),
      isEmpty(entry.getEnumeration()) ? EMPTY : escape(entry.getEnumeration()),
      isEmpty(entry.getChronology()) ? EMPTY : escape(entry.getChronology()));
  }
}
