package org.folio.bulkops.batch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.exception.BulkEditException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BulkEditInstanceSkipListenerTest {

  @Mock private BulkEditSkipListener bulkEditSkipListener;
  @InjectMocks private BulkEditInstanceSkipListener skipListener;

  @Test
  void onSkipInProcess_shouldDelegateToBulkEditSkipListenerWhenBulkEditException() {
    var identifier = new ItemIdentifier().withItemId("instance-1");
    var exception = new BulkEditException("instance error", ErrorType.ERROR);

    skipListener.onSkipInProcess(identifier, exception);

    verify(bulkEditSkipListener).onSkipInProcess(identifier, exception);
  }

  @Test
  void onSkipInProcess_shouldNotDelegateWhenNotBulkEditException() {
    var identifier = new ItemIdentifier().withItemId("instance-2");
    var exception = new RuntimeException("unexpected error");

    skipListener.onSkipInProcess(identifier, exception);

    verify(bulkEditSkipListener, never()).onSkipInProcess(any(), any());
  }
}
