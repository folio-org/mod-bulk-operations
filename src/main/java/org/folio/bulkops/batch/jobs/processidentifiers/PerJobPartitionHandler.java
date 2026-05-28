package org.folio.bulkops.batch.jobs.processidentifiers;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.partition.support.AbstractPartitionHandler;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * A {@link org.springframework.batch.core.partition.PartitionHandler} that creates a fresh,
 * dedicated thread pool for every job execution. This ensures complete isolation between
 * concurrently running jobs — a large job's partition workers cannot block a small job's workers.
 */
@Log4j2
public class PerJobPartitionHandler extends AbstractPartitionHandler {

  private final Step step;
  private final int threadPoolSize;

  /**
   * Creates a new handler for the given worker step.
   *
   * @param step           the worker step to execute for each partition
   * @param gridAndThreadPoolSize       maximum number of partitions (passed to
   *                                    {@link AbstractPartitionHandler}) and size of the per-job
   *                                    thread pool (in this case should equal {@code gridSize}
   *                                    so all partitions can run concurrently)
   */
  public PerJobPartitionHandler(Step step, int gridAndThreadPoolSize) {
    this.step = step;
    setGridSize(gridAndThreadPoolSize);
    this.threadPoolSize = gridAndThreadPoolSize;
  }

  @Override
  protected Set<StepExecution> doHandle(
      StepExecution managerStepExecution, Set<StepExecution> partitionStepExecutions)
      throws Exception {

    var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(threadPoolSize);
    executor.setMaxPoolSize(threadPoolSize);
    executor.setTaskDecorator(
        FolioExecutionScopeExecutionContextManager::getRunnableWithCurrentFolioContext);
    executor.initialize();

    Set<Future<StepExecution>> tasks = new HashSet<>(partitionStepExecutions.size());
    Set<StepExecution> result = new HashSet<>(partitionStepExecutions.size());

    try {
      for (StepExecution stepExecution : partitionStepExecutions) {
        FutureTask<StepExecution> task =
            new FutureTask<>(
                () -> {
                  step.execute(stepExecution);
                  return stepExecution;
                });
        try {
          executor.execute(task);
          tasks.add(task);
        } catch (TaskRejectedException e) {
          log.error(
              "Executor rejected partition task for step '{}'. Marking as FAILED.",
              stepExecution.getStepName(),
              e);
          stepExecution.upgradeStatus(BatchStatus.FAILED);
          stepExecution.setExitStatus(
              ExitStatus.FAILED.addExitDescription(
                  "TaskExecutor rejected the task for step: " + stepExecution.getStepName()));
          result.add(stepExecution);
        }
      }

      for (Future<StepExecution> task : tasks) {
        result.add(task.get());
      }
    } finally {
      executor.shutdown();
    }

    return result;
  }
}
