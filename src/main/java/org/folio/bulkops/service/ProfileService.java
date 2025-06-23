package org.folio.bulkops.service;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.entity.Profile;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.domain.dto.ProfileUpdateRequest;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.repository.ProfileRepository;
import org.folio.bulkops.domain.dto.ProfileDto;
import org.folio.bulkops.domain.dto.ProfileSummaryDTO;
import org.folio.bulkops.domain.dto.ProfileSummaryResultsDto;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

@Service
@Log4j2
@AllArgsConstructor
public class ProfileService {
  private final FolioExecutionContext folioExecutionContext;
  private final UserClient userClient;

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

    List<ProfileSummaryDTO> items = page.stream()
      .map(profile -> {
        ProfileSummaryDTO dto = new ProfileSummaryDTO();
        dto.setId(profile.getId());
        dto.setName(profile.getName());
        dto.setDescription(profile.getDescription());
        dto.setLocked(profile.isLocked());
        dto.setEntityType(profile.getEntityType());
        return dto;
      })
      .toList();

    ProfileSummaryResultsDto result = new ProfileSummaryResultsDto();
    result.setContent(items);
    result.setTotalRecords(page.getTotalElements());
    return result;

  }

  public ProfileDto createProfile(org.folio.bulkops.domain.dto.ProfileRequest profileRequest) {
    UUID userId = folioExecutionContext.getUserId();
    String username = getUsername(userId);

    if (Boolean.TRUE.equals(profileRequest.getLocked())) {
      validator.checkIfLockPermissionExists();
    }

    Profile entity = Profile.builder()
      .name(profileRequest.getName())
      .description(profileRequest.getDescription())
      .locked(Boolean.TRUE.equals(profileRequest.getLocked()))
      .entityType(profileRequest.getEntityType())
      .ruleDetails(profileRequest.getRuleDetails())
      .marcRuleDetails(profileRequest.getMarcRuleDetails())
      .createdDate(OffsetDateTime.now())
      .createdBy(userId)
      .createdByUser(username)
      .updatedDate(OffsetDateTime.now())
      .updatedBy(userId)
      .updatedByUser(username)
      .build();

    Profile saved = profileRepository.save(entity);
    return toDto(saved);
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
    Optional.ofNullable(profileUpdateRequest.getName()).ifPresent(existing::setName);
    Optional.ofNullable(profileUpdateRequest.getDescription()).ifPresent(existing::setDescription);
    Optional.ofNullable(profileUpdateRequest.getEntityType()).ifPresent(existing::setEntityType);
    Optional.ofNullable(profileUpdateRequest.getRuleDetails()).ifPresent(existing::setRuleDetails);
    Optional.ofNullable(profileUpdateRequest.getMarcRuleDetails()).ifPresent(existing::setMarcRuleDetails);
    Optional.ofNullable(profileUpdateRequest.getLocked()).ifPresent(existing::setLocked);

    existing.setUpdatedDate(OffsetDateTime.now());
    existing.setUpdatedBy(userId);
    existing.setUpdatedByUser(username);

    Profile updated = profileRepository.save(existing);
    return toDto(updated);
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

  private ProfileDto toDto(Profile profile) {
    ProfileDto dto = new ProfileDto();
    dto.setId(profile.getId());
    dto.setName(profile.getName());
    dto.setDescription(profile.getDescription());
    dto.setLocked(profile.isLocked());
    dto.setEntityType(profile.getEntityType());
    dto.setRuleDetails(profile.getRuleDetails());
    dto.setMarcRuleDetails(profile.getMarcRuleDetails());
    dto.setCreatedDate(profile.getCreatedDate() == null ? null : Date.from(profile.getCreatedDate().toInstant()));
    dto.setCreatedBy(profile.getCreatedBy());
    dto.setCreatedByUser(profile.getCreatedByUser());
    dto.setCreatedDate(profile.getCreatedDate() == null ? null : Date.from(profile.getUpdatedDate().toInstant()));
    dto.setUpdatedBy(profile.getUpdatedBy());
    dto.setUpdatedByUser(profile.getUpdatedByUser());
    return dto;
  }
}
