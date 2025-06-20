package org.folio.bulkops.service;


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
import static org.mockito.Mockito.doAnswer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.Personal;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.ProfileSummaryResultsDto;
import org.folio.bulkops.domain.dto.ProfileUpdateRequest;
import org.folio.bulkops.domain.dto.ProfileSummaryDTO;
import org.folio.bulkops.domain.dto.ProfileRequest;
import org.folio.bulkops.domain.dto.ProfileDto;
import org.folio.bulkops.domain.entity.Profile;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.ProfileLockedException;
import org.folio.bulkops.mapper.ProfileMapper;
import org.folio.bulkops.mapper.ProfileRequestMapper;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.repository.ProfileRepository;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.mockito.Mock;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileServiceTest extends BaseTest {

  @Autowired
  private ProfileService profileService;

  @MockitoBean
  private ProfileRepository profileRepository;

  @MockitoBean
  private PermissionsValidator permissionsValidator;

  @MockitoBean
  private ProfileMapper profileMapper;

  @MockitoBean
  private ProfileRequestMapper profileRequestMapper;

  @Mock
  private FolioExecutionContext ec;

  @MockitoBean
  private JpaCqlRepository<Profile, UUID> profileUUIDJpaCqlRepository;

  private UUID contextUserId = UUID.randomUUID();


  @Test
  void shouldReturnAllProfileSummaries() {
    Profile profile = new Profile();
    List<Profile> profileList = List.of(profile);
    ProfileSummaryDTO summaryDto = new ProfileSummaryDTO();

    String expectedQuery = "cql.allRecords=1";
    int expectedOffset = 0;
    int expectedLimit = Integer.MAX_VALUE;
    Page<Profile> profilePage = new PageImpl<>(profileList);

    when(profileUUIDJpaCqlRepository.findByCql(expectedQuery, OffsetRequest.of(expectedOffset, expectedLimit)))
      .thenReturn(profilePage);
    when(profileMapper.toSummaryDTO(profile)).thenReturn(summaryDto);

    ProfileSummaryResultsDto result = profileService.getProfileSummaries(null, null, null);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).containsExactly(summaryDto);
    assertThat(result.getTotalRecords()).isEqualTo(1);
  }

  @Test
  void shouldReturnFilteredProfileSummariesWithPagination() {
    Profile profile = new Profile();
    List<Profile> profileList = List.of(profile);
    ProfileSummaryDTO summaryDto = new ProfileSummaryDTO();

    String query = "name==\"Test Profile\"";
    int offset = 10;
    int limit = 5;

    Page<Profile> profilePage = new PageImpl<>(profileList);

    when(profileUUIDJpaCqlRepository.findByCql(query, OffsetRequest.of(offset, limit)))
      .thenReturn(profilePage);
    when(profileMapper.toSummaryDTO(profile)).thenReturn(summaryDto);

    ProfileSummaryResultsDto result = profileService.getProfileSummaries(query, offset, limit);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).containsExactly(summaryDto);
    assertThat(result.getTotalRecords()).isEqualTo(1);
  }

  @Test
  void shouldReturnEmptyResultsWhenNoProfilesFound() {
    String query = "name==\"Nonexistent\"";

    Page<Profile> emptyPage = new PageImpl<>(List.of());

    when(profileUUIDJpaCqlRepository.findByCql(eq(query), any())).thenReturn(emptyPage);

    ProfileSummaryResultsDto result = profileService.getProfileSummaries(query, 0, 10);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).isEmpty();
    assertThat(result.getTotalRecords()).isZero();
  }

  @Test
  void shouldUseDefaultQueryWhenOnlyPaginationProvided() {
    Profile profile = new Profile();
    List<Profile> profileList = List.of(profile);
    ProfileSummaryDTO summaryDto = new ProfileSummaryDTO();

    Page<Profile> profilePage = new PageImpl<>(profileList);

    when(profileUUIDJpaCqlRepository.findByCql("cql.allRecords=1", OffsetRequest.of(20, 10)))
      .thenReturn(profilePage);
    when(profileMapper.toSummaryDTO(profile)).thenReturn(summaryDto);

    ProfileSummaryResultsDto result = profileService.getProfileSummaries(null, 20, 10);

    assertThat(result.getContent()).containsExactly(summaryDto);
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
      .when(permissionsValidator).checkIfLockPermissionExists();

    ProfileLockedException ex = assertThrows(ProfileLockedException.class, () ->
      profileService.deleteById(profileId)
    );

    assertEquals("Cannot delete a locked profile without proper permission", ex.getMessage());
  }


  @Test
  void testCreateProfile() {
    User user = createUser();
    String fullName = getFullName(user);
    ProfileRequest request = createProfileRequest();
    Profile entity = createProfile(request, UUID.randomUUID());
    Profile saved = createProfile(request, entity.getId());
    ProfileDto expected = createProfileDto(saved);

    when(ec.getUserId()).thenReturn(contextUserId);
    ReflectionTestUtils.setField(profileService, "folioExecutionContext", ec);
    when(userClient.getUserById(contextUserId.toString())).thenReturn(user);
    when(profileRequestMapper.toEntity(request, contextUserId, fullName)).thenReturn(entity);
    when(profileRepository.save(entity)).thenReturn(saved);
    when(profileMapper.toDto(saved)).thenReturn(expected);

    ProfileDto result = profileService.createProfile(request);
    assertEquals(expected, result);
  }

  @Test
  void testUpdateProfile() {
    UUID profileId = UUID.randomUUID();
    User user = createUser();
    String fullName = getFullName(user);

    ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();
    updateRequest.name("Updated Profile Name");
    updateRequest.setLocked(true);
    updateRequest.setDescription("Updated description");

    Profile existing = createProfile(createProfileRequest(), profileId);
    Profile updated = createProfile(createProfileRequest(), profileId);
    updated.setName(updateRequest.getName());
    updated.setLocked(updateRequest.getLocked());
    updated.setDescription(updateRequest.getDescription());
    updated.setUpdatedBy(contextUserId);
    updated.setUpdatedByUser(fullName);

    ProfileDto expected = createProfileDto(updated);

    when(ec.getUserId()).thenReturn(contextUserId);
    ReflectionTestUtils.setField(profileService, "folioExecutionContext", ec);
    when(userClient.getUserById(contextUserId.toString())).thenReturn(user);
    when(profileRepository.findById(profileId)).thenReturn(Optional.of(existing));
    doAnswer(invocation -> {
      Profile entity = invocation.getArgument(0);
      ProfileUpdateRequest req = invocation.getArgument(1);
      entity.setName(req.getName());
      entity.setLocked(req.getLocked());
      entity.setDescription(req.getDescription());
      return null;
    }).when(profileRequestMapper).updateEntity(existing, updateRequest);
    when(profileRepository.save(existing)).thenReturn(updated);
    when(profileMapper.toDto(updated)).thenReturn(expected);

    ProfileDto result = profileService.updateProfile(profileId, updateRequest);
    assertEquals(expected, result);
  }

  @Test
  void testUpdateProfile_notFound() {
    UUID nonExistentId = UUID.randomUUID();

    ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();
    updateRequest.name("Doesn't matter");
    updateRequest.setLocked(false);

    when(ec.getUserId()).thenReturn(contextUserId);
    ReflectionTestUtils.setField(profileService, "folioExecutionContext", ec);
    when(userClient.getUserById(contextUserId.toString())).thenReturn(createUser());
    when(profileRepository.findById(nonExistentId)).thenReturn(Optional.empty());

    NotFoundException ex = assertThrows(NotFoundException.class, () ->
      profileService.updateProfile(nonExistentId, updateRequest)
    );
    assertEquals("Profile not found with ID: " + nonExistentId, ex.getMessage());
  }

  @Test
  void testUpdateProfile_lockedProfile_throwsProfileLockedException() {
    UUID profileId = UUID.randomUUID();

    ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();
    updateRequest.setName("Attempted Update");
    updateRequest.setLocked(false);

    Profile lockedProfile = createProfile(createProfileRequest(), profileId);
    lockedProfile.setLocked(true);

    when(ec.getUserId()).thenReturn(contextUserId);
    ReflectionTestUtils.setField(profileService, "folioExecutionContext", ec);
    when(userClient.getUserById(contextUserId.toString())).thenReturn(createUser());
    when(profileRepository.findById(profileId)).thenReturn(Optional.of(lockedProfile));

    doThrow(new ProfileLockedException("Cannot update a locked profile"))
      .when(permissionsValidator).checkIfLockPermissionExists();

    ProfileLockedException ex = assertThrows(ProfileLockedException.class, () ->
      profileService.updateProfile(profileId, updateRequest)
    );

    assertEquals("Cannot update a locked profile", ex.getMessage());
  }

  @Test
  void testUpdateProfile_unlockedProfile_allowsEditWithoutPermissionCheck() {
    UUID profileId = UUID.randomUUID();
    User user = createUser();

    ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();
    updateRequest.setLocked(false); // not trying to lock

    Profile unlockedProfile = createProfile(createProfileRequest(), profileId);
    unlockedProfile.setLocked(false); // profile not locked

    when(ec.getUserId()).thenReturn(contextUserId);
    ReflectionTestUtils.setField(profileService, "folioExecutionContext", ec);
    when(userClient.getUserById(contextUserId.toString())).thenReturn(user);
    when(profileRepository.findById(profileId)).thenReturn(Optional.of(unlockedProfile));
    when(profileRepository.save(any())).thenReturn(unlockedProfile);
    when(profileMapper.toDto(any())).thenReturn(createProfileDto(unlockedProfile));

    ProfileDto result = profileService.updateProfile(profileId, updateRequest);

    assertEquals(updateRequest.getLocked(), result.getLocked());
    verify(permissionsValidator, never()).checkIfLockPermissionExists();
  }

  @Test
  void testUpdateProfile_unlockedProfile_tryLockWithoutPermission_throws() {
    UUID profileId = UUID.randomUUID();
    User user = createUser();

    ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();
    updateRequest.setLocked(true); // trying to lock

    Profile unlockedProfile = createProfile(createProfileRequest(), profileId);
    unlockedProfile.setLocked(false);

    when(ec.getUserId()).thenReturn(contextUserId);
    ReflectionTestUtils.setField(profileService, "folioExecutionContext", ec);
    when(userClient.getUserById(contextUserId.toString())).thenReturn(user);
    when(profileRepository.findById(profileId)).thenReturn(Optional.of(unlockedProfile));

    doThrow(new ProfileLockedException("Missing permission")).when(permissionsValidator).checkIfLockPermissionExists();

    ProfileLockedException ex = assertThrows(ProfileLockedException.class, () ->
      profileService.updateProfile(profileId, updateRequest)
    );

    assertEquals("Missing permission", ex.getMessage());
  }

  @Test
  void testUpdateProfile_unlockedProfile_tryLockWithPermission_allows() {
    UUID profileId = UUID.randomUUID();
    User user = createUser();
    String fullName = getFullName(user);

    ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();
    updateRequest.setLocked(true); // trying to lock
    updateRequest.setName("Locked Now");
    updateRequest.setDescription("Updated while locking");

    Profile existing = createProfile(createProfileRequest(), profileId);
    existing.setLocked(false);

    Profile updated = createProfile(createProfileRequest(), profileId);
    updated.setLocked(true);
    updated.setName(updateRequest.getName());
    updated.setDescription(updateRequest.getDescription());
    updated.setUpdatedBy(contextUserId);
    updated.setUpdatedByUser(fullName);

    ProfileDto expected = createProfileDto(updated);

    when(ec.getUserId()).thenReturn(contextUserId);
    ReflectionTestUtils.setField(profileService, "folioExecutionContext", ec);
    when(userClient.getUserById(contextUserId.toString())).thenReturn(user);
    when(profileRepository.findById(profileId)).thenReturn(Optional.of(existing));

    doAnswer(invocation -> {
      Profile entity = invocation.getArgument(0);
      ProfileUpdateRequest req = invocation.getArgument(1);
      entity.setName(req.getName());
      entity.setLocked(req.getLocked());
      entity.setDescription(req.getDescription());
      return null;
    }).when(profileRequestMapper).updateEntity(existing, updateRequest);

    when(profileRepository.save(existing)).thenReturn(updated);
    when(profileMapper.toDto(updated)).thenReturn(expected);

    doNothing().when(permissionsValidator).checkIfLockPermissionExists();

    ProfileDto result = profileService.updateProfile(profileId, updateRequest);

    assertEquals(expected.getName(), result.getName());
    assertEquals(expected.getLocked(), result.getLocked());
    assertEquals(expected.getDescription(), result.getDescription());
  }

  @Test
  void testUpdateProfile_lockedProfile_withoutPermission_throws() {
    UUID profileId = UUID.randomUUID();
    User user = createUser();

    ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();
    updateRequest.setLocked(true);

    Profile lockedProfile = createProfile(createProfileRequest(), profileId);
    lockedProfile.setLocked(true); // already locked

    when(ec.getUserId()).thenReturn(contextUserId);
    ReflectionTestUtils.setField(profileService, "folioExecutionContext", ec);
    when(userClient.getUserById(contextUserId.toString())).thenReturn(user);
    when(profileRepository.findById(profileId)).thenReturn(Optional.of(lockedProfile));

    doThrow(new ProfileLockedException("Missing permission")).when(permissionsValidator).checkIfLockPermissionExists();

    ProfileLockedException ex = assertThrows(ProfileLockedException.class, () ->
      profileService.updateProfile(profileId, updateRequest)
    );

    assertEquals("Missing permission", ex.getMessage());
  }
  @Test
  void testUpdateProfile_lockedProfile_withPermission_allows() {
    UUID profileId = UUID.randomUUID();
    User user = createUser();
    String fullName = getFullName(user);

    ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();
    updateRequest.setLocked(true);
    updateRequest.setName("Updated");
    updateRequest.setDescription("Updated description");

    Profile lockedProfile = createProfile(createProfileRequest(), profileId);
    lockedProfile.setLocked(true); // already locked

    Profile updated = createProfile(createProfileRequest(), profileId);
    updated.setLocked(true);
    updated.setName(updateRequest.getName());
    updated.setDescription(updateRequest.getDescription());
    updated.setUpdatedBy(contextUserId);
    updated.setUpdatedByUser(fullName);

    ProfileDto expected = createProfileDto(updated);

    when(ec.getUserId()).thenReturn(contextUserId);
    ReflectionTestUtils.setField(profileService, "folioExecutionContext", ec);
    when(userClient.getUserById(contextUserId.toString())).thenReturn(user);
    when(profileRepository.findById(profileId)).thenReturn(Optional.of(lockedProfile));

    doAnswer(invocation -> {
      Profile entity = invocation.getArgument(0);
      ProfileUpdateRequest req = invocation.getArgument(1);
      entity.setName(req.getName());
      entity.setLocked(req.getLocked());
      entity.setDescription(req.getDescription());
      return null;
    }).when(profileRequestMapper).updateEntity(lockedProfile, updateRequest);

    when(profileRepository.save(lockedProfile)).thenReturn(updated);
    when(profileMapper.toDto(updated)).thenReturn(expected);

    doNothing().when(permissionsValidator).checkIfLockPermissionExists();

    ProfileDto result = profileService.updateProfile(profileId, updateRequest);

    assertEquals(expected.getName(), result.getName());
    assertEquals(expected.getLocked(), result.getLocked());
    assertEquals(expected.getDescription(), result.getDescription());
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
      .when(permissionsValidator).checkIfLockPermissionExists();

    // Act & Assert
    ProfileLockedException ex = assertThrows(ProfileLockedException.class, () ->
      profileService.deleteById(profileId)
    );

    assertEquals("Cannot delete a locked profile without proper permission", ex.getMessage());
    verify(permissionsValidator).checkIfLockPermissionExists();
    verify(profileRepository, never()).delete(any());
  }


  private User createUser() {
    Personal personal = new Personal();
    personal.setFirstName("Abc");
    personal.setLastName("Abc");

    User user = new User();
    user.setId(contextUserId.toString());
    user.setPersonal(personal);
    user.setUsername("Abc");
    return user;
  }

  private String getFullName(User user) {
    return String.format("%s, %s", user.getPersonal().getLastName(), user.getPersonal().getFirstName());
  }

  private ProfileRequest createProfileRequest() {
    ProfileRequest request = new ProfileRequest();
    request.name("profile name");
    request.setLocked(false);
    request.setEntityType(USER);
    request.createdBy(contextUserId);
    request.createdByUser(getFullName(createUser()));
    return request;
  }

  private Profile createProfile(ProfileRequest request, UUID id) {
    Profile profile = new Profile();
    profile.setId(id);
    profile.setName(request.getName());
    profile.setLocked(request.getLocked());
    profile.setEntityType(request.getEntityType());
    profile.setCreatedBy(request.getCreatedBy());
    profile.setCreatedByUser(request.getCreatedByUser());
    return profile;
  }

  private ProfileDto createProfileDto(Profile profile) {
    ProfileDto dto = new ProfileDto();
    dto.setId(profile.getId());
    dto.setName(profile.getName());
    dto.setLocked(profile.isLocked());
    dto.setEntityType(profile.getEntityType());
    dto.setCreatedBy(profile.getCreatedBy());
    dto.setCreatedByUser(profile.getCreatedByUser());
    dto.setUpdatedBy(profile.getUpdatedBy());
    dto.setUpdatedByUser(profile.getUpdatedByUser());
    dto.setDescription(profile.getDescription());
    return dto;
  }
}

