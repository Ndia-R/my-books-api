package com.example.my_books_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.my_books_api.dto.user.UpdateSubscriptionPlanRequest;
import com.example.my_books_api.dto.user.UserProfileResponse;
import com.example.my_books_api.entity.User;
import com.example.my_books_api.exception.NotFoundException;
import com.example.my_books_api.mapper.UserMapper;
import com.example.my_books_api.repository.UserRepository;
import com.example.my_books_api.service.impl.UserServiceImpl;
import com.example.my_books_api.util.JwtClaimExtractor;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtClaimExtractor jwtClaimExtractor;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private UserServiceImpl userService;

    // --- updateSubscriptionPlan ---

    @Test
    @DisplayName("updateSubscriptionPlan - 正常系: FREEからPREMIUMへ変更")
    void updateSubscriptionPlan_FreeToPremium_Success() {
        // Given
        String userId = "user-uuid-001";
        UpdateSubscriptionPlanRequest request = new UpdateSubscriptionPlanRequest("PREMIUM");

        User user = new User();
        user.setId(userId);
        user.setSubscriptionPlan("FREE");
        user.setDisplayName("テストユーザー");
        user.setAvatarPath("/avatar00.png");

        UserProfileResponse expectedResponse = new UserProfileResponse();
        expectedResponse.setId(userId);
        expectedResponse.setSubscriptionPlan("PREMIUM");

        when(jwtClaimExtractor.getUserId()).thenReturn(userId);
        when(userRepository.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserProfileResponse(user)).thenReturn(expectedResponse);
        when(jwtClaimExtractor.getUsername()).thenReturn("testuser");
        when(jwtClaimExtractor.getEmail()).thenReturn("test@example.com");
        when(jwtClaimExtractor.getFamilyName()).thenReturn("テスト");
        when(jwtClaimExtractor.getGivenName()).thenReturn("ユーザー");
        when(jwtClaimExtractor.getRoles()).thenReturn(List.of("USER"));
        when(jwtClaimExtractor.getGroups()).thenReturn(List.of());

        // When
        UserProfileResponse response = userService.updateSubscriptionPlan(request);

        // Then
        assertThat(response.getSubscriptionPlan()).isEqualTo("PREMIUM");
        verify(userRepository).save(user);
        verify(subscriptionService).evictSubscriptionPlanCache(userId);
    }

    @Test
    @DisplayName("updateSubscriptionPlan - 正常系: PREMIUMからFREEへ変更")
    void updateSubscriptionPlan_PremiumToFree_Success() {
        // Given
        String userId = "user-uuid-001";
        UpdateSubscriptionPlanRequest request = new UpdateSubscriptionPlanRequest("FREE");

        User user = new User();
        user.setId(userId);
        user.setSubscriptionPlan("PREMIUM");
        user.setDisplayName("テストユーザー");
        user.setAvatarPath("/avatar00.png");

        UserProfileResponse expectedResponse = new UserProfileResponse();
        expectedResponse.setId(userId);
        expectedResponse.setSubscriptionPlan("FREE");

        when(jwtClaimExtractor.getUserId()).thenReturn(userId);
        when(userRepository.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserProfileResponse(user)).thenReturn(expectedResponse);
        when(jwtClaimExtractor.getUsername()).thenReturn("testuser");
        when(jwtClaimExtractor.getEmail()).thenReturn("test@example.com");
        when(jwtClaimExtractor.getFamilyName()).thenReturn("テスト");
        when(jwtClaimExtractor.getGivenName()).thenReturn("ユーザー");
        when(jwtClaimExtractor.getRoles()).thenReturn(List.of("USER"));
        when(jwtClaimExtractor.getGroups()).thenReturn(List.of());

        // When
        UserProfileResponse response = userService.updateSubscriptionPlan(request);

        // Then
        assertThat(response.getSubscriptionPlan()).isEqualTo("FREE");
        verify(userRepository).save(user);
        verify(subscriptionService).evictSubscriptionPlanCache(userId);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("updateSubscriptionPlan - ユーザーが存在しない場合はNotFoundExceptionをスロー")
    void updateSubscriptionPlan_UserNotFound_ThrowsNotFoundException() {
        // Given
        String userId = "non-existent";
        when(jwtClaimExtractor.getUserId()).thenReturn(userId);
        when(userRepository.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> userService.updateSubscriptionPlan(new UpdateSubscriptionPlanRequest("PREMIUM")))
            .isInstanceOf(NotFoundException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(subscriptionService, never()).evictSubscriptionPlanCache(anyString());
    }

    // --- updateUserSubscriptionPlan ---

    @Test
    @DisplayName("updateUserSubscriptionPlan - 正常系: 管理者が指定ユーザーのプランを変更")
    void updateUserSubscriptionPlan_Success() {
        // Given
        String targetUserId = "user-uuid-002";
        UpdateSubscriptionPlanRequest request = new UpdateSubscriptionPlanRequest("PREMIUM");

        User user = new User();
        user.setId(targetUserId);
        user.setSubscriptionPlan("FREE");
        user.setDisplayName("対象ユーザー");
        user.setAvatarPath("/avatar01.png");

        UserProfileResponse expectedResponse = new UserProfileResponse();
        expectedResponse.setId(targetUserId);
        expectedResponse.setSubscriptionPlan("PREMIUM");

        when(userRepository.findByIdAndIsDeletedFalse(targetUserId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserProfileResponse(user)).thenReturn(expectedResponse);

        // When
        UserProfileResponse response = userService.updateUserSubscriptionPlan(targetUserId, request);

        // Then
        assertThat(response.getSubscriptionPlan()).isEqualTo("PREMIUM");
        verify(userRepository).save(user);
        verify(subscriptionService).evictSubscriptionPlanCache(targetUserId);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("updateUserSubscriptionPlan - 対象ユーザーが存在しない場合はNotFoundExceptionをスロー")
    void updateUserSubscriptionPlan_UserNotFound_ThrowsNotFoundException() {
        // Given
        String targetUserId = "non-existent";
        when(userRepository.findByIdAndIsDeletedFalse(targetUserId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(
            () -> userService.updateUserSubscriptionPlan(targetUserId, new UpdateSubscriptionPlanRequest("FREE"))
        )
            .isInstanceOf(NotFoundException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(subscriptionService, never()).evictSubscriptionPlanCache(anyString());
    }
}
