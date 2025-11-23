package com.example.my_books_backend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.my_books_backend.dto.user.UpdateUserProfileRequest;
import com.example.my_books_backend.dto.user.UserProfileCountsResponse;
import com.example.my_books_backend.dto.user.UserProfileResponse;
import com.example.my_books_backend.dto.user.UserResponse;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.mapper.UserMapper;
import com.example.my_books_backend.repository.UserRepository;
import com.example.my_books_backend.util.JwtClaimExtractor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtClaimExtractor jwtClaimExtractor;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "550e8400-e29b-41d4-a716-446655440000";
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setDisplayName("Test User");
        testUser.setAvatarPath("/avatars/test.png");
    }

    @Test
    void testGetAllUsers_正常系() {
        // Given
        List<User> users = Arrays.asList(testUser);
        List<UserResponse> expectedResponses = Arrays.asList(new UserResponse());

        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.toUserResponseList(users)).thenReturn(expectedResponses);

        // When
        List<UserResponse> result = userService.getAllUsers();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).findAll();
        verify(userMapper).toUserResponseList(users);
    }

    @Test
    void testGetUserById_正常系() {
        // Given
        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setId(testUserId);

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userMapper.toUserResponse(testUser)).thenReturn(expectedResponse);

        // When
        UserResponse result = userService.getUserById(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getId());
        verify(userRepository).findById(testUserId);
        verify(userMapper).toUserResponse(testUser);
    }

    @Test
    void testGetUserById_ユーザーが存在しない場合() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            userService.getUserById(testUserId);
        });
        verify(userRepository).findById(testUserId);
    }

    @Test
    void testDeleteUser_正常系() {
        // When
        userService.deleteUser(testUserId);

        // Then
        verify(userRepository).deleteById(testUserId);
    }

    @Test
    void testGetUserProfile_正常系_既存ユーザー() {
        // Given
        UserProfileResponse expectedResponse = new UserProfileResponse();
        expectedResponse.setId(testUserId);
        expectedResponse.setDisplayName("Test User");

        when(userRepository.findByIdAndIsDeletedFalse(testUserId)).thenReturn(Optional.of(testUser));
        when(userMapper.toUserProfileResponse(testUser)).thenReturn(expectedResponse);
        when(jwtClaimExtractor.getCurrentUserEmail()).thenReturn("test@example.com");
        when(jwtClaimExtractor.getCurrentUserName()).thenReturn("Test Name");

        // When
        UserProfileResponse result = userService.getUserProfile(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Test Name", result.getName());
        verify(userRepository).findByIdAndIsDeletedFalse(testUserId);
        verify(userMapper).toUserProfileResponse(testUser);
        verify(jwtClaimExtractor).getCurrentUserEmail();
        verify(jwtClaimExtractor).getCurrentUserName();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testGetUserProfile_正常系_新規ユーザー自動作成() {
        // Given
        User newUser = new User();
        newUser.setId(testUserId);
        newUser.setDisplayName("User");
        newUser.setAvatarPath("");

        UserProfileResponse expectedResponse = new UserProfileResponse();
        expectedResponse.setId(testUserId);

        when(userRepository.findByIdAndIsDeletedFalse(testUserId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userMapper.toUserProfileResponse(any(User.class))).thenReturn(expectedResponse);
        when(jwtClaimExtractor.getCurrentUserEmail()).thenReturn("test@example.com");
        when(jwtClaimExtractor.getCurrentUserName()).thenReturn("Test Name");

        // When
        UserProfileResponse result = userService.getUserProfile(testUserId);

        // Then
        assertNotNull(result);
        verify(userRepository).findByIdAndIsDeletedFalse(testUserId);
        verify(userRepository).save(any(User.class));
        verify(userMapper).toUserProfileResponse(any(User.class));
    }

    @Test
    void testGetUserProfileCounts_正常系() {
        // Given
        UserProfileCountsResponse expectedResponse = new UserProfileCountsResponse(3L, 2L, 5L);

        when(userRepository.getUserProfileCountsResponse(testUserId)).thenReturn(expectedResponse);

        // When
        UserProfileCountsResponse result = userService.getUserProfileCounts(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(3L, result.getFavoriteCount());
        assertEquals(2L, result.getBookmarkCount());
        assertEquals(5L, result.getReviewCount());
        verify(userRepository).getUserProfileCountsResponse(testUserId);
    }

    @Test
    void testUpdateUserProfile_正常系() {
        // Given
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setDisplayName("Updated Name");
        request.setAvatarPath("/avatars/updated.png");

        User updatedUser = new User();
        updatedUser.setId(testUserId);
        updatedUser.setDisplayName("Updated Name");
        updatedUser.setAvatarPath("/avatars/updated.png");

        UserProfileResponse expectedResponse = new UserProfileResponse();
        expectedResponse.setId(testUserId);
        expectedResponse.setDisplayName("Updated Name");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(updatedUser);
        when(userMapper.toUserProfileResponse(updatedUser)).thenReturn(expectedResponse);
        when(jwtClaimExtractor.getCurrentUserEmail()).thenReturn("test@example.com");
        when(jwtClaimExtractor.getCurrentUserName()).thenReturn("Test Name");

        // When
        UserProfileResponse result = userService.updateUserProfile(request, testUserId);

        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getId());
        assertEquals("Updated Name", result.getDisplayName());
        verify(userRepository).findById(testUserId);
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateUserProfile_ユーザーが存在しない場合() {
        // Given
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setDisplayName("Updated Name");

        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            userService.updateUserProfile(request, testUserId);
        });
        verify(userRepository).findById(testUserId);
        verify(userRepository, never()).save(any(User.class));
    }
}
