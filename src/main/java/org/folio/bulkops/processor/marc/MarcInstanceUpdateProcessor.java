package org.folio.bulkops.processor.marc;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.util.Constants.MARC;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.bulkops.builder.DataImportProfilesBuilder;
import org.folio.bulkops.client.DataImportClient;
import org.folio.bulkops.client.DataImportProfilesClient;
import org.folio.bulkops.client.DataImportRestS3UploadClient;
import org.folio.bulkops.client.DataImportUploadClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.ActionProfile;
import org.folio.bulkops.domain.bean.ActionProfilePost;
import org.folio.bulkops.domain.bean.AssembleStorageFileRequestBody;
import org.folio.bulkops.domain.bean.FileDefinition;
import org.folio.bulkops.domain.bean.JobProfile;
import org.folio.bulkops.domain.bean.JobProfileInfo;
import org.folio.bulkops.domain.bean.JobProfilePost;
import org.folio.bulkops.domain.bean.MappingProfile;
import org.folio.bulkops.domain.bean.MappingProfilePost;
import org.folio.bulkops.domain.bean.MatchProfilePost;
import org.folio.bulkops.domain.bean.UploadFileDefinition;
import org.folio.bulkops.domain.bean.UploadFileDefinitionProcessFiles;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.processor.MarcUpdateProcessor;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.service.BulkOperationService;
import org.folio.bulkops.service.BulkOperationServiceHelper;
import org.folio.bulkops.util.MarcDateHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class MarcInstanceUpdateProcessor implements MarcUpdateProcessor {
  private static final String POSTFIX_PATTERN = "%s-%s";

  private final DataImportClient dataImportClient;
  private final DataImportUploadClient dataImportUploadClient;
  private final DataImportProfilesClient dataImportProfilesClient;
  private final DataImportProfilesBuilder dataImportProfilesBuilder;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final BulkOperationRepository bulkOperationRepository;
  private final DataImportRestS3UploadClient dataImportRestS3UploadClient;
  private final BulkOperationServiceHelper bulkOperationServiceHelper;

  private BulkOperationService bulkOperationService;

  public void updateMarcRecords(BulkOperation bulkOperation) throws IOException {
    try (var is = remoteFileSystemClient.get(bulkOperation.getLinkToCommittedRecordsMarcFile())) {
      var content = is.readAllBytes();
      if (content.length != 0) {
        var jobProfile = createJobProfile();
        var uploadDefinition = uploadMarcFile(bulkOperation, content);
        dataImportClient.uploadFileDefinitionsProcessFiles(
            UploadFileDefinitionProcessFiles.builder()
                .uploadFileDefinition(uploadDefinition)
                .jobProfileInfo(
                    JobProfileInfo.builder().id(jobProfile.getId()).dataType(MARC).build())
                .build(),
            uploadDefinition.getId());
        bulkOperation.setDataImportJobProfileId(UUID.fromString(jobProfile.getId()));
        bulkOperationRepository.save(bulkOperation);
        log.info("updateMarcRecords last");
        try {
          Thread.sleep(2000);
        } catch (Exception e) {
          e.printStackTrace();
        }
        bulkOperationService.processDataImportResult(bulkOperation);
      } else {
        bulkOperation.setLinkToCommittedRecordsMarcFile(null);
        bulkOperationServiceHelper.completeBulkOperation(bulkOperation);
      }
    }
  }

  @Autowired
  public void setBulkOperationService(@Lazy BulkOperationService bulkOperationService) {
    this.bulkOperationService = bulkOperationService;
  }

  private JobProfile createJobProfile() throws IOException {
    var date = new Date();

    var matchProfile = dataImportProfilesBuilder.getMatchProfile();
    matchProfile.setName(addPostfixToName(matchProfile.getName(), date));
    var savedMatchProfile =
        dataImportProfilesClient.createMatchProfile(
            MatchProfilePost.builder().profile(matchProfile).build());

    var mappingProfileSrs =
        postMappingProfile(dataImportProfilesBuilder.getMappingProfileToUpdateSrs(), date);
    var actionProfileSrs =
        postActionProfile(
            dataImportProfilesBuilder.getActionProfilePostToUpdateSrs(mappingProfileSrs), date);

    return postJobProfile(
        dataImportProfilesBuilder.getJobProfilePost(savedMatchProfile, actionProfileSrs), date);
  }

  private MappingProfile postMappingProfile(MappingProfile mappingProfile, Date date) {
    mappingProfile.setName(addPostfixToName(mappingProfile.getName(), date));
    return dataImportProfilesClient.createMappingProfile(
        MappingProfilePost.builder().profile(mappingProfile).build());
  }

  private ActionProfile postActionProfile(ActionProfilePost actionProfilePost, Date date) {
    actionProfilePost
        .getProfile()
        .setName(addPostfixToName(actionProfilePost.getProfile().getName(), date));
    return dataImportProfilesClient.createActionProfile(actionProfilePost);
  }

  private JobProfile postJobProfile(JobProfilePost jobProfilePost, Date date) {
    jobProfilePost
        .getProfile()
        .setName(addPostfixToName(jobProfilePost.getProfile().getName(), date));
    return dataImportProfilesClient.createJobProfile(jobProfilePost);
  }

  private String addPostfixToName(String name, Date date) {
    return String.format(
        POSTFIX_PATTERN, isEmpty(name) ? EMPTY : name, MarcDateHelper.getDateTimeForMarc(date));
  }

  private UploadFileDefinition uploadMarcFile(BulkOperation bulkOperation, byte[] content) {
    var uploadDefinition =
        dataImportClient.postUploadDefinition(
            UploadFileDefinition.builder()
                .fileDefinitions(
                    List.of(
                        FileDefinition.builder()
                            .name(
                                FilenameUtils.getName(
                                    bulkOperation.getLinkToCommittedRecordsMarcFile()))
                            .build()))
                .build());
    var uploadDefinitionId = uploadDefinition.getId();
    var fileDefinitionId = uploadDefinition.getFileDefinitions().getFirst().getId();

    var splitStatus = dataImportClient.getSplitStatus();
    log.info("Split status: {}", splitStatus.getSplitStatus());

    if (TRUE.equals(splitStatus.getSplitStatus())) {
      var uploadUrlResponse =
          dataImportClient.getUploadUrl(uploadDefinition.getFileDefinitions().getFirst().getName());
      var etag =
          dataImportRestS3UploadClient
              .uploadFile(uploadUrlResponse.getUrl(), content)
              .getHeaders()
              .getETag();
      dataImportClient.assembleStorageFile(
          uploadDefinitionId,
          fileDefinitionId,
          new AssembleStorageFileRequestBody(
              uploadUrlResponse.getUploadId(), uploadUrlResponse.getKey(), List.of(etag)));
      uploadDefinition = dataImportClient.getUploadDefinitionById(uploadDefinitionId);
    } else {
      uploadDefinition =
          dataImportUploadClient.uploadFileDefinitionsFiles(
              uploadDefinitionId, fileDefinitionId, content);
    }
    return uploadDefinition;
  }
}
