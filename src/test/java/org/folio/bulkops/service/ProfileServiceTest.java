package org.folio.bulkops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.dto.ProfileDto;
import org.folio.bulkops.domain.dto.ProfileRequest;
import org.folio.bulkops.domain.dto.ProfilesDto;
import org.folio.bulkops.domain.entity.Profile;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.ProfileLockedException;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.repository.ProfileRepository;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

class ProfileServiceTest extends BaseTest {

  @Autowired private ProfileService profileService;

  @MockitoBean private ProfileRepository profileRepository;

  @MockitoBean private PermissionsValidator permissionsValidator;

  @MockitoBean private FolioExecutionContext ec;

  @MockitoBean private JpaCqlRepository<Profile, UUID> profileUuidJpaCqlRepository;

  private final UUID contextUserId = UUID.randomUUID();

  @Test
  void shouldReturnAllProfileSummaries() {
    Profile profile = new Profile();
    profile.setId(UUID.randomUUID());
    profile.setName("Test Profile");
    profile.setDescription("desc");
    profile.setLocked(false);
    profile.setEntityType(USER);

    List<Profile> profileList = List.of(profile);
    Page<Profile> profilePage = new PageImpl<>(profileList);

    when(profileUuidJpaCqlRepository.findByCql(
            "cql.allRecords=1", OffsetRequest.of(0, Integer.MAX_VALUE)))
        .thenReturn(profilePage);

    ProfilesDto result = profileService.getProfiles(null, null, null);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);

    ProfileDto summary = result.getContent().getFirst();

    assertThat(summary.getId()).isEqualTo(profile.getId());
    assertThat(summary.getName()).isEqualTo(profile.getName());
    assertThat(summary.getDescription()).isEqualTo(profile.getDescription());
    assertThat(summary.getLocked()).isEqualTo(profile.isLocked());
    assertThat(summary.getEntityType()).isEqualTo(profile.getEntityType());

    assertThat(result.getTotalRecords()).isEqualTo(1);
  }

  @Test
  void shouldReturnFilteredProfileSummariesWithPagination() {
    Profile profile = new Profile();
    profile.setId(UUID.randomUUID());
    profile.setName("Filtered Profile");
    profile.setDescription("desc");
    profile.setLocked(false);
    profile.setEntityType(USER);

    List<Profile> profileList = List.of(profile);
    Page<Profile> profilePage = new PageImpl<>(profileList);

    String query = "name==\"Test Profile\"";
    int offset = 10;
    int limit = 5;

    when(profileUuidJpaCqlRepository.findByCql(query, OffsetRequest.of(offset, limit)))
        .thenReturn(profilePage);

    ProfilesDto result = profileService.getProfiles(query, offset, limit);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);

    ProfileDto summary = result.getContent().getFirst();
    assertThat(summary.getId()).isEqualTo(profile.getId());

    assertThat(result.getTotalRecords()).isEqualTo(1);
  }

  @Test
  void shouldReturnEmptyResultsWhenNoProfilesFound() {
    String query = "name==\"Nonexistent\"";

    Page<Profile> emptyPage = new PageImpl<>(List.of());

    when(profileUuidJpaCqlRepository.findByCql(eq(query), any())).thenReturn(emptyPage);

    ProfilesDto result = profileService.getProfiles(query, 0, 10);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).isEmpty();
    assertThat(result.getTotalRecords()).isZero();
  }

  @Test
  void shouldUseDefaultQueryWhenOnlyPaginationProvided() {
    Profile profile = new Profile();
    profile.setId(UUID.randomUUID());
    profile.setName("Paged Profile");
    profile.setDescription("desc");
    profile.setLocked(false);
    profile.setEntityType(USER);

    List<Profile> profileList = List.of(profile);
    Page<Profile> profilePage = new PageImpl<>(profileList);

    when(profileUuidJpaCqlRepository.findByCql("cql.allRecords=1", OffsetRequest.of(20, 10)))
        .thenReturn(profilePage);

    ProfilesDto result = profileService.getProfiles(null, 20, 10);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getTotalRecords()).isEqualTo(1);
  }

  @Test
  void shouldDeleteProfile() {
    UUID id = UUID.randomUUID();
    Profile profile = new Profile();
    when(profileRepository.findById(id)).thenReturn(Optional.of(profile));

    profileService.deleteById(id);

    verify(profileRepository).delete(profile);
  }

  @Test
  void shouldThrowOnDeleteWhenProfileNotFound() {
    UUID id = UUID.randomUUID();
    when(profileRepository.findById(id)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> profileService.deleteById(id));
  }

  @Test
  void testDeleteProfile_lockedProfile_throwsProfileLockedException() {
    UUID profileId = UUID.randomUUID();

    Profile lockedProfile = createProfile(createProfileRequest(), profileId);
    lockedProfile.setLocked(true);

    when(profileRepository.findById(profileId)).thenReturn(Optional.of(lockedProfile));

    doThrow(new ProfileLockedException("Cannot delete a locked profile without proper permission"))
        .when(permissionsValidator)
        .checkIfLockPermissionExists();

    ProfileLockedException ex =
        assertThrows(ProfileLockedException.class, () -> profileService.deleteById(profileId));

    assertEquals("Cannot delete a locked profile without proper permission", ex.getMessage());
  }

  @Test
  void testCreateProfile() {
    ProfileRequest request = createProfileRequest();

    UUID userId = contextUserId;
    ReflectionTestUtils.setField(profileService, "folioExecutionContext", ec);

    when(ec.getUserId()).thenReturn(userId);

    Profile entity = new Profile();
    entity.setName(request.getName());
    entity.setLocked(request.getLocked());
    entity.setEntityType(request.getEntityType());
    entity.setCreatedBy(userId);
    entity.setDescription(request.getDescription());

    Profile saved = new Profile();
    saved.setId(UUID.randomUUID());
    saved.setName(entity.getName());
    saved.setEntityType(entity.getEntityType());
    saved.setCreatedBy(userId);
    saved.setDescription(entity.getDescription());

    when(profileRepository.save(any(Profile.class))).thenReturn(saved);

    ProfileDto result = profileService.createProfile(request);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(saved.getId());
    assertThat(result.getName()).isEqualTo(saved.getName());
    assertThat(result.getLocked()).isEqualTo(saved.isLocked());
  }

  @Test
  void testUpdateProfile() {
    ProfileRequest updateRequest = new ProfileRequest();
    updateRequest.setName("Updated Profile Name");
    updateRequest.setLocked(true);
    updateRequest.setDescription("Updated description");

    UUID profileId = UUID.randomUUID();

    Profile existing = new Profile();
    existing.setId(profileId);
    existing.setName("Old Name");
    existing.setLocked(false);
    existing.setDescription("Old description");

    Profile updated = new Profile();
    updated.setId(profileId);
    updated.setName(updateRequest.getName());
    updated.setLocked(updateRequest.getLocked());
    updated.setDescription(updateRequest.getDescription());
    updated.setUpdatedBy(contextUserId);

    ReflectionTestUtils.setField(profileService, "folioExecutionContext", ec);

    when(ec.getUserId()).thenReturn(contextUserId);
    when(profileRepository.findById(profileId)).thenReturn(Optional.of(existing));
    when(profileRepository.save(existing)).thenReturn(updated);

    ProfileDto result = profileService.updateProfile(profileId, updateRequest);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo(updated.getName());
    assertThat(result.getLocked()).isEqualTo(updated.isLocked());
    assertThat(result.getDescription()).isEqualTo(updated.getDescription());
  }

  @Test
  void testUpdateProfile_notFound() {

    ProfileRequest updateRequest = new ProfileRequest();
    updateRequest.setName("Doesn't matter");
    updateRequest.setLocked(false);

    when(ec.getUserId()).thenReturn(contextUserId);
    ReflectionTestUtils.setField(profileService, "folioExecutionContext", ec);
    UUID nonExistentId = UUID.randomUUID();
    when(profileRepository.findById(nonExistentId)).thenReturn(Optional.empty());

    NotFoundException ex =
        assertThrows(
            NotFoundException.class,
            () -> profileService.updateProfile(nonExistentId, updateRequest));
    assertEquals("Profile not found with ID: " + nonExistentId, ex.getMessage());
  }

  @Test
  void testUpdateProfile_lockedProfile_throwsProfileLockedException() {
    UUID profileId = UUID.randomUUID();

    ProfileRequest updateRequest = new ProfileRequest();
    updateRequest.setLocked(true);

    Profile lockedProfile = createProfile(createProfileRequest(), profileId);
    lockedProfile.setLocked(true);

    when(ec.getUserId()).thenReturn(contextUserId);
    ReflectionTestUtils.setField(profileService, "folioExecutionContext", ec);
    when(profileRepository.findById(profileId)).thenReturn(Optional.of(lockedProfile));

    doThrow(new ProfileLockedException("Cannot update a locked profile"))
        .when(permissionsValidator)
        .checkIfLockPermissionExists();

    ProfileLockedException ex =
        assertThrows(
            ProfileLockedException.class,
            () -> profileService.updateProfile(profileId, updateRequest));

    assertEquals("Cannot update a locked profile", ex.getMessage());
  }

  @Test
  void testUpdateProfile_unlockedProfile_allowsEditWithoutPermissionCheck() {
    UUID profileId = UUID.randomUUID();

    ProfileRequest updateRequest = new ProfileRequest();
    updateRequest.setLocked(false);

    Profile unlockedProfile = createProfile(createProfileRequest(), profileId);
    unlockedProfile.setLocked(false);

    when(ec.getUserId()).thenReturn(contextUserId);
    ReflectionTestUtils.setField(profileService, "folioExecutionContext", ec);
    when(profileRepository.findById(profileId)).thenReturn(Optional.of(unlockedProfile));
    when(profileRepository.save(any())).thenReturn(unlockedProfile);

    ProfileDto result = profileService.updateProfile(profileId, updateRequest);

    assertEquals(updateRequest.getLocked(), result.getLocked());
    verify(permissionsValidator, never()).checkIfLockPermissionExists();
  }

  @Test
  void testUpdateProfile_unlockedProfile_tryLockWithoutPermission_throws() {
    UUID profileId = UUID.randomUUID();

    ProfileRequest updateRequest = new ProfileRequest();
    updateRequest.setLocked(true);

    Profile unlockedProfile = createProfile(createProfileRequest(), profileId);
    unlockedProfile.setLocked(false);

    when(ec.getUserId()).thenReturn(contextUserId);
    ReflectionTestUtils.setField(profileService, "folioExecutionContext", ec);
    when(profileRepository.findById(profileId)).thenReturn(Optional.of(unlockedProfile));

    doThrow(new ProfileLockedException("Missing permission"))
        .when(permissionsValidator)
        .checkIfLockPermissionExists();

    ProfileLockedException ex =
        assertThrows(
            ProfileLockedException.class,
            () -> profileService.updateProfile(profileId, updateRequest));

    assertEquals("Missing permission", ex.getMessage());
  }

  @Test
  void testUpdateProfile_unlockedProfile_tryLockWithPermission_allows() {
    UUID profileId = UUID.randomUUID();

    ProfileRequest updateRequest = new ProfileRequest();
    updateRequest.setLocked(true);
    updateRequest.setName("Locked Now");
    updateRequest.setDescription("Updated while locking");

    Profile existing = createProfile(createProfileRequest(), profileId);
    existing.setLocked(false);

    Profile updated = createProfile(createProfileRequest(), profileId);
    updated.setLocked(true);
    updated.setName(updateRequest.getName());
    updated.setDescription(updateRequest.getDescription());
    updated.setUpdatedBy(contextUserId);

    ReflectionTestUtils.setField(profileService, "folioExecutionContext", ec);

    when(ec.getUserId()).thenReturn(contextUserId);
    when(profileRepository.findById(profileId)).thenReturn(Optional.of(existing));
    when(profileRepository.save(existing)).thenReturn(updated);

    doNothing().when(permissionsValidator).checkIfLockPermissionExists();

    ProfileDto result = profileService.updateProfile(profileId, updateRequest);

    assertEquals(updated.getName(), result.getName());
    assertEquals(updated.isLocked(), result.getLocked());
    assertEquals(updated.getDescription(), result.getDescription());
  }

  @Test
  void testUpdateProfile_lockedProfile_withoutPermission_throws() {
    UUID profileId = UUID.randomUUID();

    ProfileRequest updateRequest = new ProfileRequest();
    updateRequest.setLocked(true);

    Profile lockedProfile = createProfile(createProfileRequest(), profileId);
    lockedProfile.setLocked(true);

    ReflectionTestUtils.setField(profileService, "folioExecutionContext", ec);

    when(ec.getUserId()).thenReturn(contextUserId);
    when(profileRepository.findById(profileId)).thenReturn(Optional.of(lockedProfile));

    doThrow(new ProfileLockedException("Missing permission"))
        .when(permissionsValidator)
        .checkIfLockPermissionExists();

    ProfileLockedException ex =
        assertThrows(
            ProfileLockedException.class,
            () -> profileService.updateProfile(profileId, updateRequest));

    assertEquals("Missing permission", ex.getMessage());
  }

  @Test
  void testUpdateProfile_lockedProfile_withPermission_allows() {
    UUID profileId = UUID.randomUUID();

    ProfileRequest updateRequest = new ProfileRequest();
    updateRequest.setLocked(true);
    updateRequest.setName("Updated");
    updateRequest.setDescription("Updated description");

    Profile lockedProfile = createProfile(createProfileRequest(), profileId);
    lockedProfile.setLocked(true);

    Profile updated = createProfile(createProfileRequest(), profileId);
    updated.setLocked(true);
    updated.setName(updateRequest.getName());
    updated.setDescription(updateRequest.getDescription());
    updated.setUpdatedBy(contextUserId);

    ReflectionTestUtils.setField(profileService, "folioExecutionContext", ec);

    when(ec.getUserId()).thenReturn(contextUserId);
    when(profileRepository.findById(profileId)).thenReturn(Optional.of(lockedProfile));
    when(profileRepository.save(lockedProfile)).thenReturn(updated);

    doNothing().when(permissionsValidator).checkIfLockPermissionExists();

    ProfileDto result = profileService.updateProfile(profileId, updateRequest);

    assertEquals(updated.getName(), result.getName());
    assertEquals(updated.isLocked(), result.getLocked());
    assertEquals(updated.getDescription(), result.getDescription());
  }

  @Test
  void testDeleteById_notLockedProfile_allowsDelete() {
    UUID profileId = UUID.randomUUID();
    Profile unlockedProfile = createProfile(createProfileRequest(), profileId);
    unlockedProfile.setLocked(false);

    when(profileRepository.findById(profileId)).thenReturn(Optional.of(unlockedProfile));
    doNothing().when(profileRepository).delete(unlockedProfile);

    profileService.deleteById(profileId);

    verify(profileRepository).delete(unlockedProfile);
  }

  @Test
  void testDeleteById_lockedProfile_withPermission_allowsDelete() {
    UUID profileId = UUID.randomUUID();
    Profile lockedProfile = createProfile(createProfileRequest(), profileId);
    lockedProfile.setLocked(true);

    when(profileRepository.findById(profileId)).thenReturn(Optional.of(lockedProfile));
    doNothing().when(permissionsValidator).checkIfLockPermissionExists();
    doNothing().when(profileRepository).delete(lockedProfile);

    profileService.deleteById(profileId);

    verify(permissionsValidator).checkIfLockPermissionExists();
    verify(profileRepository).delete(lockedProfile);
  }

  @Test
  void testDeleteById_lockedProfile_withoutPermission_throwsProfileLockedException() {
    UUID profileId = UUID.randomUUID();
    Profile lockedProfile = createProfile(createProfileRequest(), profileId);
    lockedProfile.setLocked(true);

    when(profileRepository.findById(profileId)).thenReturn(Optional.of(lockedProfile));
    doThrow(new ProfileLockedException("Cannot delete a locked profile without proper permission"))
        .when(permissionsValidator)
        .checkIfLockPermissionExists();

    ProfileLockedException ex =
        assertThrows(ProfileLockedException.class, () -> profileService.deleteById(profileId));

    assertEquals("Cannot delete a locked profile without proper permission", ex.getMessage());
    verify(permissionsValidator).checkIfLockPermissionExists();
    verify(profileRepository, never()).delete(any());
  }

  private ProfileRequest createProfileRequest() {
    ProfileRequest request = new ProfileRequest();
    request.setName("Sample Profile");
    request.setDescription("Sample description");
    request.setLocked(false);
    request.setEntityType(USER);
    return request;
  }

  private Profile createProfile(ProfileRequest request, UUID id) {
    Profile profile = new Profile();
    profile.setId(id);
    profile.setName(request.getName());
    profile.setDescription(request.getDescription());
    profile.setLocked(request.getLocked());
    profile.setEntityType(request.getEntityType());
    return profile;
  }
}
