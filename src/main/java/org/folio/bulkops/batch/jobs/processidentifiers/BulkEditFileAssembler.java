package org.folio.bulkops.batch.jobs.processidentifiers;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.folio.bulkops.domain.bean.JobParameterNames.AT_LEAST_ONE_MARC_EXISTS;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE;
import static org.folio.bulkops.util.Constants.LINE_BREAK;
import static org.folio.bulkops.util.Constants.UTF8_BOM;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.JobParameterNames;
import org.folio.bulkops.exception.BulkEditException;
import org.folio.bulkops.exception.FileOperationException;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class BulkEditFileAssembler implements StepExecutionAggregator {
  @Value("${application.batch.merge-csv-json-mrc-pool-size}")
  private int mergeCsvJsonMrcPoolSize = 0;

  @Value("${application.batch.minutes-for-merge}")
  private int numMinutesForMerge = 0;

  @Override
  public void aggregate(
      @NotNull StepExecution stepExecution, @NotNull Collection<StepExecution> executions) {
    if (atLeastOnePartitionFailed(executions)) {
      stepExecution.setStatus(BatchStatus.FAILED);
    } else {
      mergeCsvJsonMarcInParallel(stepExecution, executions);
    }
  }

  private boolean atLeastOnePartitionFailed(Collection<StepExecution> executions) {
    return executions.stream().anyMatch(step -> BatchStatus.FAILED.equals(step.getStatus()));
  }

  private void mergeCsvJsonMarcInParallel(
      StepExecution stepExecution, Collection<StepExecution> executions) {
    ExecutorService exec = Executors.newFixedThreadPool(mergeCsvJsonMrcPoolSize);
    List<String> filesToDelete = new ArrayList<>();
    if (atLeastOneMarcExists(stepExecution)) {
      mergeMarc(stepExecution, executions, exec, filesToDelete);
    }
    mergeCsv(stepExecution, executions, exec, filesToDelete);
    mergeJson(stepExecution, executions, exec, filesToDelete);
    exec.shutdown();
    try {
      boolean finished = exec.awaitTermination(numMinutesForMerge, TimeUnit.MINUTES);
      if (finished) {
        log.info("Merge csv, json and mrc files has been completed successfully.");
        removePartFiles(filesToDelete);
      } else {
        var errorMsg =
            "Merge csv, json and mrc files exceeded allowed %d minutes"
                .formatted(numMinutesForMerge);
        log.error(errorMsg);
        throw new BulkEditException(errorMsg);
      }
    } catch (InterruptedException e) {
      log.error(e);
      Thread.currentThread().interrupt();
      throw new FileOperationException(e);
    }
  }

  private void mergeCsv(
      StepExecution stepExecution,
      Collection<StepExecution> executions,
      ExecutorService exec,
      List<String> filesToDelete) {
    exec.execute(
        () -> {
          var csvFileParts =
              executions.stream()
                  .map(
                      e ->
                          e.getExecutionContext().getString(JobParameterNames.TEMP_OUTPUT_CSV_PATH))
                  .toList();
          filesToDelete.addAll(csvFileParts);

          var tmpLocalFilePath =
              stepExecution
                  .getJobExecution()
                  .getJobParameters()
                  .getString(JobParameterNames.TEMP_LOCAL_FILE_PATH);
          ofNullable(tmpLocalFilePath)
              .ifPresent(
                  path -> {
                    try (var writer = new BufferedWriter(new FileWriter(path))) {
                      for (int i = 0; i < csvFileParts.size(); i++) {
                        var reader = new BufferedReader(new FileReader(csvFileParts.get(i)));
                        String line = reader.readLine();
                        if (i == 0 && nonNull(line)) {
                          writer.write(UTF8_BOM + line + LINE_BREAK);
                        }
                        while ((line = reader.readLine()) != null) {
                          writer.write(line + LINE_BREAK);
                        }
                        reader.close();
                      }
                    } catch (Exception e) {
                      log.error("Error occurred while merging CSV part files", e);
                      throw new FileOperationException(e);
                    }
                  });
        });
  }

  private void mergeMarc(
      StepExecution stepExecution,
      Collection<StepExecution> executions,
      ExecutorService exec,
      List<String> filesToDelete) {
    if (isInstanceJob(stepExecution)) {
      exec.execute(
          () -> {
            var marcFileParts =
                executions.stream()
                    .map(
                        e ->
                            e.getExecutionContext()
                                .getString(JobParameterNames.TEMP_OUTPUT_MARC_PATH))
                    .toList();
            mergePartFiles(
                stepExecution, JobParameterNames.TEMP_LOCAL_MARC_PATH, ".mrc", marcFileParts);
            filesToDelete.addAll(marcFileParts);
          });
    }
  }

  private void mergeJson(
      StepExecution stepExecution,
      Collection<StepExecution> executions,
      ExecutorService exec,
      List<String> filesToDelete) {
    exec.execute(
        () -> {
          var jsonFileParts =
              executions.stream()
                  .map(
                      e ->
                          e.getExecutionContext()
                              .getString(JobParameterNames.TEMP_OUTPUT_JSON_PATH))
                  .toList();
          mergePartFiles(
              stepExecution, JobParameterNames.TEMP_LOCAL_FILE_PATH, ".json", jsonFileParts);
          filesToDelete.addAll(jsonFileParts);
        });
  }

  private void mergePartFiles(
      StepExecution stepExecution,
      String jobParamOfOutputFileName,
      String outputFileExtension,
      List<String> fileParts) {
    try {
      var baseOutput =
          stepExecution.getJobExecution().getJobParameters().getString(jobParamOfOutputFileName);
      var outputPath = baseOutput + outputFileExtension;
      var options =
          new java.nio.file.OpenOption[] {StandardOpenOption.CREATE, StandardOpenOption.APPEND};
      try (OutputStream out = Files.newOutputStream(Paths.get(outputPath), options)) {
        for (String part : fileParts) {
          Files.copy(Path.of(part), out);
        }
      }
    } catch (Exception e) {
      log.error("Error occurred while merging part files", e);
      throw new FileOperationException(e);
    }
  }

  private boolean isInstanceJob(StepExecution stepExecution) {
    var jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
    return jobName.endsWith(INSTANCE.getValue());
  }

  private void removePartFiles(List<String> partFiles) {
    try (var exec = Executors.newCachedThreadPool()) {
      exec.execute(
          () -> {
            partFiles.forEach(
                file -> {
                  try {
                    Files.delete(Path.of(file));
                  } catch (IOException e) {
                    log.error("Error occurred while deleting the part files", e);
                    throw new FileOperationException(e);
                  }
                });
            var deletedCount = partFiles.size();
            log.info("All {} part files have been deleted successfully.", deletedCount);
          });
    }
  }

  private boolean atLeastOneMarcExists(StepExecution stepExecution) {
    var ctx = stepExecution.getJobExecution().getExecutionContext();
    return ctx.containsKey(AT_LEAST_ONE_MARC_EXISTS);
  }
}
