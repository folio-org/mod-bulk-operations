package org.folio.bulkops.batch.jobs.processidentifiers;

import static java.util.Objects.nonNull;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.JobParameterNames;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;

@Log4j2
public class BulkEditPartitioner implements Partitioner {

  private final String outputCsvPathTemplate;
  private final String outputJsonPathTemplate;
  private final String outputMarcPathTemplate;
  private Long offset;
  private Long limit;

  @Value("${application.batch.partition-size}")
  private int partitionSize;

  protected BulkEditPartitioner(
      String tempOutputCsvPath,
      String tempOutputJsonPath,
      String tempOutputMarcPath,
      long numOfLines) {
    offset = 0L;
    limit = numOfLines;
    outputCsvPathTemplate = tempOutputCsvPath + "_%d.csv";
    outputJsonPathTemplate = tempOutputJsonPath + "_%d.json";
    outputMarcPathTemplate = tempOutputMarcPath == null ? null : tempOutputMarcPath + "_%d.mrc";
  }

  @Override
  public @NonNull Map<String, ExecutionContext> partition(int gridSize) {

    Map<String, ExecutionContext> result = new HashMap<>();

    long numberOfPartitions = limit / partitionSize;
    if (numberOfPartitions == 0) {
      numberOfPartitions = 1;
    }

    long currentLimit;
    for (var i = 0; i < numberOfPartitions; i++) {
      final String tempOutputCsvPath = outputCsvPathTemplate.formatted(i);
      final String tempOutputJsonPath = outputJsonPathTemplate.formatted(i);
      final String tempOutputMarcPath =
          outputMarcPathTemplate == null ? null : outputMarcPathTemplate.formatted(i);
      currentLimit = limit - partitionSize >= partitionSize ? partitionSize : limit;

      var executionContext = new ExecutionContext();
      executionContext.putLong("offset", offset);
      executionContext.putLong("limit", currentLimit);
      executionContext.putLong("partition", i);
      executionContext.putString(JobParameterNames.TEMP_OUTPUT_CSV_PATH, tempOutputCsvPath);
      if (nonNull(tempOutputMarcPath)) {
        executionContext.putString(JobParameterNames.TEMP_OUTPUT_MARC_PATH, tempOutputMarcPath);
      }
      executionContext.putString(JobParameterNames.TEMP_OUTPUT_JSON_PATH, tempOutputJsonPath);
      result.put("Partition_" + i, executionContext);

      log.info(
          "Partition {}: offset {}, limit {}, tempOutputPath {}, {}, {}.",
          i,
          offset,
          currentLimit,
          tempOutputCsvPath,
          tempOutputJsonPath,
          tempOutputMarcPath);

      offset += currentLimit;
      limit -= partitionSize;
    }
    return result;
  }
}
