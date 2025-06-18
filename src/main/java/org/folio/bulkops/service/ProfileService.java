package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.entity.Profile;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.mapper.ProfileMapper;
import org.folio.bulkops.mapper.ProfileRequestMapper;
import org.folio.bulkops.domain.dto.ProfileUpdateRequest;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.repository.ProfileRepository;
import org.folio.bulkops.domain.dto.ProfileDto;
import org.folio.bulkops.domain.dto.ProfileSummaryResultsDto;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class ProfileService {
  private final ProfileRequestMapper profileRequestMapper;
  private final FolioExecutionContext folioExecutionContext;
  private final UserClient userClient;
  private final ProfileMapper profileMapper;
  private final ProfileRepository profileRepository;
  private final JpaCqlRepository<Profile, UUID> profileCqlRepository;

  private final PermissionsValidator validator;

  public ProfileSummaryResultsDto getProfileSummaries(String query, Integer offset, Integer limit) {
    String effectiveQuery = (query == null || query.isBlank()) ? "cql.allRecords=1" : query;

    var page = profileCqlRepository.findByCql(
      effectiveQuery,
      OffsetRequest.of(
        Objects.requireNonNullElse(offset, 0),
        Objects.requireNonNullElse(limit, Integer.MAX_VALUE)
      )
    );

    List<org.folio.bulkops.domain.dto.ProfileSummaryDTO> items = page.stream()
      .map(profileMapper::toSummaryDTO)
      .toList();

    return new ProfileSummaryResultsDto()
      .content(items)
      .totalRecords(page.getTotalElements());
  }

  public ProfileDto createProfile(org.folio.bulkops.domain.dto.ProfileRequest profileRequest) {
    UUID userId = folioExecutionContext.getUserId();
    String username = getUsername(userId);

    if (Boolean.TRUE.equals(profileRequest.getLocked())) {
      validator.checkIfLockPermissionExists();
    }

    Profile entity = profileRequestMapper.toEntity(profileRequest, userId, username);
    Profile saved = profileRepository.save(entity);
    return profileMapper.toDto(saved);
  }

  public void deleteById(UUID id) {
    Profile profile = getProfile(id);

    if (profile.isLocked()) {
      validator.checkIfLockPermissionExists();
    }

    profileRepository.delete(profile);
  }

  public ProfileDto updateProfile(UUID profileId, ProfileUpdateRequest profileUpdateRequest) {
    UUID userId = folioExecutionContext.getUserId();
    String username = getUsername(userId);

    Profile existing = getProfile(profileId);

    boolean isLocked = existing.isLocked();
    boolean isLockAttempt = !isLocked && Boolean.TRUE.equals(profileUpdateRequest.getLocked());

    if (isLocked || isLockAttempt) {
      validator.checkIfLockPermissionExists();
    }

    profileRequestMapper.updateEntity(existing, profileUpdateRequest);
    existing.setUpdatedDate(OffsetDateTime.now());
    existing.setUpdatedBy(userId);
    existing.setUpdatedByUser(username);

    return profileMapper.toDto(profileRepository.save(existing));
  }

  private Profile getProfile(UUID profileId) {
    return profileRepository.findById(profileId)
      .orElseThrow(() -> new NotFoundException("Profile not found with ID: " + profileId));
  }

  private String getUsername(UUID userId) {
    try {
      log.info("Attempting to retrieve username for id {}", userId);
      User user = userClient.getUserById(userId.toString());
      var personal = user.getPersonal();
      if (personal != null && personal.getFirstName() != null && personal.getLastName() != null) {
        return String.format("%s, %s", personal.getLastName(), personal.getFirstName());
      }
      return userId.toString();
    } catch (Exception exception) {
      log.error("Unexpected error when fetching user: {}", exception.getMessage(), exception);
      return userId.toString();
    }
  }
}
