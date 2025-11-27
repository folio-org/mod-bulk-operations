package org.folio.bulkops.processor.folio;

import static java.lang.String.format;
import static org.folio.bulkops.domain.dto.UpdateActionType.CHANGE_TYPE;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ADMINISTRATIVE_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.INSTANCE_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SET_RECORDS_FOR_DELETE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.STAFF_SUPPRESS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.InstanceNote;
import org.folio.bulkops.domain.bean.Publication;
import org.folio.bulkops.domain.bean.Subject;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.RuleValidationException;
import org.folio.bulkops.processor.FolioAbstractDataProcessor;
import org.folio.bulkops.processor.Updater;
import org.folio.bulkops.processor.Validator;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class FolioInstanceDataProcessor extends FolioAbstractDataProcessor<ExtendedInstance> {

  private final InstanceNotesUpdaterFactory instanceNotesUpdaterFactory;

  @Override
  public Validator<UpdateOptionType, Action, BulkOperationRule> validator(
      ExtendedInstance extendedInstance) {
    return (option, action, rule) -> {
      boolean suppressClear =
          CLEAR_FIELD.equals(action.getType())
              && Set.of(STAFF_SUPPRESS, SUPPRESS_FROM_DISCOVERY).contains(option);
      if (suppressClear) {
        throw new RuleValidationException("Suppress flag cannot be cleared.");
      }

      boolean instanceNoteUnsupported =
          INSTANCE_NOTE.equals(option) && !"FOLIO".equals(extendedInstance.getEntity().getSource());
      if (instanceNoteUnsupported) {
        throw new RuleValidationException(
            "Bulk edit of instance notes is not supported for MARC Instances.");
      }

      boolean adminNoteChangeUnsupported =
          ADMINISTRATIVE_NOTE.equals(option)
              && CHANGE_TYPE.equals(action.getType())
              && !"FOLIO".equals(extendedInstance.getEntity().getSource());
      if (adminNoteChangeUnsupported) {
        String msg =
            "Change note type for administrative notes is not supported for MARC Instances.";
        throw new RuleValidationException(msg);
      }
    };
  }

  @Override
  public Updater<ExtendedInstance> updater(
      UpdateOptionType option, Action action, ExtendedInstance entity, boolean forPreview) {
    if (STAFF_SUPPRESS.equals(option)) {
      if (SET_TO_TRUE.equals(action.getType())) {
        return extendedInstance -> extendedInstance.getEntity().setStaffSuppress(true);
      } else if (SET_TO_FALSE.equals(action.getType())) {
        return extendedInstance -> extendedInstance.getEntity().setStaffSuppress(false);
      }

    } else if (SUPPRESS_FROM_DISCOVERY.equals(option)) {
      if (SET_TO_TRUE.equals(action.getType())) {
        return extendedInstance -> extendedInstance.getEntity().setDiscoverySuppress(true);
      } else if (SET_TO_FALSE.equals(action.getType())) {
        return extendedInstance -> extendedInstance.getEntity().setDiscoverySuppress(false);
      }

    } else if (SET_RECORDS_FOR_DELETE.equals(option)) {
      if (SET_TO_TRUE.equals(action.getType())) {
        return extendedInstance -> {
          extendedInstance.getEntity().setDeleted(true);
          extendedInstance.getEntity().setStaffSuppress(true);
          extendedInstance.getEntity().setDiscoverySuppress(true);
        };
      } else if (SET_TO_FALSE.equals(action.getType())) {
        return extendedInstance -> extendedInstance.getEntity().setDeleted(false);
      }
    }

    var updaterOpt = instanceNotesUpdaterFactory.getUpdater(option, action, forPreview);
    return updaterOpt.orElseGet(
        () ->
            instance -> {
              throw new BulkOperationException(
                  format("Combination %s and %s isn't supported yet", option, action.getType()));
            });
  }

  @Override
  public ExtendedInstance clone(ExtendedInstance extendedInstance) {
    var instance = extendedInstance.getEntity();
    var clone = extendedInstance.getEntity().toBuilder().build();

    if (instance.getAdministrativeNotes() != null) {
      List<String> adminNotes = new ArrayList<>(instance.getAdministrativeNotes());
      clone.setAdministrativeNotes(adminNotes);
    }

    if (instance.getInstanceNotes() != null) {
      List<InstanceNote> clonedNotes = new ArrayList<>(instance.getInstanceNotes().size());
      for (InstanceNote inote : instance.getInstanceNotes()) {
        clonedNotes.add(inote.toBuilder().build());
      }
      clone.setInstanceNotes(clonedNotes);
    }

    if (instance.getStatisticalCodeIds() != null) {
      clone.setStatisticalCodeIds(new ArrayList<>(instance.getStatisticalCodeIds()));
    }

    if (instance.getElectronicAccess() != null) {
      List<ElectronicAccess> elAccList = new ArrayList<>(instance.getElectronicAccess().size());
      for (ElectronicAccess el : instance.getElectronicAccess()) {
        elAccList.add(el.toBuilder().build());
      }
      clone.setElectronicAccess(elAccList);
    }

    if (instance.getSubject() != null) {
      List<Subject> subjList = new ArrayList<>(instance.getSubject().size());
      for (Subject s : instance.getSubject()) {
        subjList.add(s.toBuilder().build());
      }
      clone.setSubject(subjList);
    }

    if (instance.getPublication() != null) {
      List<Publication> pubs = new ArrayList<>(instance.getPublication().size());
      for (Publication p : instance.getPublication()) {
        pubs.add(p.toBuilder().build());
      }
      clone.setPublication(pubs);
    }

    return ExtendedInstance.builder()
        .tenantId(extendedInstance.getTenantId())
        .entity(clone)
        .build();
  }

  @Override
  public boolean compare(ExtendedInstance first, ExtendedInstance second) {
    if (StringUtils.equals(first.getTenantId(), second.getTenantId())) {
      return Objects.equals(first.getEntity(), second.getEntity());
    }
    return false;
  }

  @Override
  public Class<ExtendedInstance> getProcessedType() {
    return ExtendedInstance.class;
  }
}
