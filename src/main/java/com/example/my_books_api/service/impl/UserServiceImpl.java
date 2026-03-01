package com.example.my_books_api.service.impl;

import com.example.my_books_api.dto.user.UserProfileCountsResponse;
import com.example.my_books_api.dto.user.UserProfileResponse;
import com.example.my_books_api.dto.user.UserResponse;
import com.example.my_books_api.dto.PageResponse;
import com.example.my_books_api.dto.user.UpdateSubscriptionPlanRequest;
import com.example.my_books_api.dto.user.UpdateUserProfileRequest;
import com.example.my_books_api.entity.User;
import com.example.my_books_api.exception.NotFoundException;
import com.example.my_books_api.mapper.UserMapper;
import com.example.my_books_api.repository.UserRepository;
import com.example.my_books_api.service.SubscriptionService;
import com.example.my_books_api.service.UserService;
import com.example.my_books_api.util.JwtClaimExtractor;
import com.example.my_books_api.util.PageableUtils;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtClaimExtractor jwtClaimExtractor;
    private final SubscriptionService subscriptionService;

    private final String DEFAULT_DISPLAY_NAME = "User";
    private final String DEFAULT_AVATAR_PATH = "/avatar00.png";
    private final String DEFAULT_SUBSCRIPTION_PLAN = "FREE";

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('user:manage:all', 'user:read:own')")
    public UserResponse getUser() {
        String userId = jwtClaimExtractor.getUserId();

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('user:manage:all')")
    public PageResponse<UserProfileResponse> getUsers(
        Long page,
        Long size,
        String sortString
    ) {
        Pageable pageable = PageableUtils.of(
            page,
            size,
            sortString,
            PageableUtils.USER_ALLOWED_FIELDS
        );
        Page<User> pageObj = userRepository.findByIsDeletedFalse(pageable);

        // 2クエリ戦略を適用
        Page<User> updatedPageObj = PageableUtils.applyTwoQueryStrategy(
            pageObj,
            userRepository::findAllByIdInWithRelations,
            User::getId
        );

        return userMapper.toPageResponse(updatedPageObj);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('user:manage:all')")
    public UserProfileResponse getUserById(@NonNull String id) {
        User user = userRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
        return userMapper.toUserProfileResponse(user);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('user:manage:all')")
    public void deleteUser(@NonNull String id) {
        User user = userRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("User not found"));

        user.setIsDeleted(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('user:manage:all', 'user:read:own')")
    public UserProfileResponse getUserProfile() {
        String userId = jwtClaimExtractor.getUserId();

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
            .orElseGet(() -> {
                // 表示名はJWTクレームからusernameを取得して設定
                String username = jwtClaimExtractor.getUsername();

                // 存在しない場合は自動作成する（デフォルトロールはROLE_USER）
                User newUser = new User();
                newUser.setId(userId);
                newUser.setDisplayName(username != null ? username : DEFAULT_DISPLAY_NAME);
                newUser.setAvatarPath(DEFAULT_AVATAR_PATH);
                newUser.setSubscriptionPlan(DEFAULT_SUBSCRIPTION_PLAN);
                return userRepository.save(newUser);
            });

        // レスポンス作成（UserエンティティにないものはJWTクレームから設定）
        UserProfileResponse response = userMapper.toUserProfileResponse(user);
        response.setUsername(jwtClaimExtractor.getUsername());
        response.setEmail(jwtClaimExtractor.getEmail());
        response.setFamilyName(jwtClaimExtractor.getFamilyName());
        response.setGivenName(jwtClaimExtractor.getGivenName());
        response.setRoles(jwtClaimExtractor.getRoles());
        response.setGroups(jwtClaimExtractor.getGroups());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('user:manage:all', 'user:read:own')")
    public UserProfileCountsResponse getUserProfileCounts() {
        String userId = jwtClaimExtractor.getUserId();
        return userRepository.getUserProfileCountsResponse(userId);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('user:manage:all', 'user:update:own')")
    public UserProfileResponse updateUserProfile(UpdateUserProfileRequest request) {
        String userId = jwtClaimExtractor.getUserId();

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getAvatarPath() != null) {
            user.setAvatarPath(request.getAvatarPath());
        }

        User savedUser = userRepository.save(user);

        // レスポンス作成（UserエンティティにないものはJWTクレームから設定）
        UserProfileResponse response = userMapper.toUserProfileResponse(savedUser);
        response.setUsername(jwtClaimExtractor.getUsername());
        response.setEmail(jwtClaimExtractor.getEmail());
        response.setFamilyName(jwtClaimExtractor.getFamilyName());
        response.setGivenName(jwtClaimExtractor.getGivenName());
        response.setRoles(jwtClaimExtractor.getRoles());
        response.setGroups(jwtClaimExtractor.getGroups());

        return response;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('user:manage:all', 'user:update:own')")
    public UserProfileResponse updateSubscriptionPlan(UpdateSubscriptionPlanRequest request) {
        String userId = jwtClaimExtractor.getUserId();

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        user.setSubscriptionPlan(request.getSubscriptionPlan());
        User savedUser = userRepository.save(user);
        subscriptionService.evictSubscriptionPlanCache(userId);

        // レスポンス作成（UserエンティティにないものはJWTクレームから設定）
        UserProfileResponse response = userMapper.toUserProfileResponse(savedUser);
        response.setUsername(jwtClaimExtractor.getUsername());
        response.setEmail(jwtClaimExtractor.getEmail());
        response.setFamilyName(jwtClaimExtractor.getFamilyName());
        response.setGivenName(jwtClaimExtractor.getGivenName());
        response.setRoles(jwtClaimExtractor.getRoles());
        response.setGroups(jwtClaimExtractor.getGroups());

        return response;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('user:manage:all')")
    public UserProfileResponse updateUserSubscriptionPlan(@NonNull String id, UpdateSubscriptionPlanRequest request) {
        User user = userRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("User not found"));

        user.setSubscriptionPlan(request.getSubscriptionPlan());
        User savedUser = userRepository.save(user);
        subscriptionService.evictSubscriptionPlanCache(id);

        return userMapper.toUserProfileResponse(savedUser);
    }
}