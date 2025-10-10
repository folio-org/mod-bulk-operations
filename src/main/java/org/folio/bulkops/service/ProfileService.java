package org.folio.bulkops.service;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.dto.ProfileDto;
import org.folio.bulkops.domain.dto.ProfileRequest;
import org.folio.bulkops.domain.dto.ProfilesDto;
import org.folio.bulkops.domain.entity.Profile;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.repository.ProfileRepository;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@AllArgsConstructor
public class ProfileService {
  private final FolioExecutionContext folioExecutionContext;
  private final ProfileRepository profileRepository;
  private final JpaCqlRepository<Profile, UUID> profileCqlRepository;
  private final PermissionsValidator validator;


  public ProfilesDto getProfiles(String query, Integer offset, Integer limit) {
    String effectiveQuery = (query == null || query.isBlank()) ? "cql.allRecords=1" : query;

    var page = profileCqlRepository.findByCql(
            effectiveQuery,
            OffsetRequest.of(
                    Objects.requireNonNullElse(offset, 0),
                    Objects.requireNonNullElse(limit, Integer.MAX_VALUE)
            )
    );

    List<ProfileDto> items = page.stream()
            .map(this::toDto)
            .toList();

    return new ProfilesDto()
            .content(items)
            .totalRecords(page.getTotalElements());
  }

  public ProfileDto createProfile(ProfileRequest profileRequest) {
    UUID userId = folioExecutionContext.getUserId();

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
            .updatedDate(OffsetDateTime.now())
            .updatedBy(userId)
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

  public ProfileDto updateProfile(UUID profileId, ProfileRequest profileRequest) {

    Profile existing = getProfile(profileId);

    boolean isLocked = existing.isLocked();
    boolean isLockAttempt = !isLocked && Boolean.TRUE.equals(profileRequest.getLocked());

    if (isLocked || isLockAttempt) {
      validator.checkIfLockPermissionExists();
    }

    existing.setName(profileRequest.getName());
    existing.setDescription(profileRequest.getDescription());
    existing.setEntityType(profileRequest.getEntityType());
    existing.setRuleDetails(profileRequest.getRuleDetails());
    existing.setMarcRuleDetails(profileRequest.getMarcRuleDetails());
    existing.setLocked(profileRequest.getLocked());
    existing.setUpdatedDate(OffsetDateTime.now());
    UUID userId = folioExecutionContext.getUserId();
    existing.setUpdatedBy(userId);

    Profile updated = profileRepository.save(existing);
    return toDto(updated);
  }

  private Profile getProfile(UUID profileId) {
    return profileRepository.findById(profileId)
            .orElseThrow(() -> new NotFoundException("Profile not found with ID: " + profileId));
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
    dto.setCreatedDate(profile.getCreatedDate() == null ? null : Date.from(profile.getCreatedDate()
            .toInstant()));
    dto.setCreatedBy(profile.getCreatedBy());
    dto.setUpdatedDate(profile.getUpdatedDate() == null ? null : Date.from(profile.getUpdatedDate()
            .toInstant()));
    dto.setUpdatedBy(profile.getUpdatedBy());
    return dto;
  }
}
