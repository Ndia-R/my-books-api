package com.example.my_books_backend.service.impl;

import com.example.my_books_backend.dto.user.UserProfileCountsResponse;
import com.example.my_books_backend.dto.user.UserProfileResponse;
import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.user.UpdateUserProfileRequest;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.mapper.UserMapper;
import com.example.my_books_backend.repository.UserRepository;
import com.example.my_books_backend.service.UserService;
import com.example.my_books_backend.util.JwtClaimExtractor;
import com.example.my_books_backend.util.PageableUtils;

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

    private final String DEFAULT_DISPLAY_NAME = "User";
    private final String DEFAULT_AVATAR_PATH = "/avatar00.png";

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('user:manage')")
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
    @PreAuthorize("hasAuthority('user:manage')")
    public UserProfileResponse getUserById(@NonNull String id) {
        User user = userRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
        return userMapper.toUserProfileResponse(user);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('user:manage')")
    public void deleteUser(@NonNull String id) {
        User user = userRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("User not found"));

        user.setIsDeleted(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('user:read:own')")
    public UserProfileResponse getUserProfile() {
        String userId = jwtClaimExtractor.getCurrentUserId();

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
            .orElseGet(() -> {
                // 表示名はJWTクレームからusernameを取得して設定
                String username = jwtClaimExtractor.getCurrentUsername();

                // 存在しない場合は自動作成する（デフォルトロールはgeneral-user）
                User newUser = new User();
                newUser.setId(userId);
                newUser.setDisplayName(username != null ? username : DEFAULT_DISPLAY_NAME);
                newUser.setAvatarPath(DEFAULT_AVATAR_PATH);
                return userRepository.save(newUser);
            });

        // レスポンス作成（UserエンティティにないものはJWTクレームから設定）
        UserProfileResponse response = userMapper.toUserProfileResponse(user);
        response.setUsername(jwtClaimExtractor.getCurrentUsername());
        response.setEmail(jwtClaimExtractor.getCurrentUserEmail());
        response.setFamilyName(jwtClaimExtractor.getCurrentFamilyName());
        response.setGivenName(jwtClaimExtractor.getCurrentGivenName());
        response.setRoles(jwtClaimExtractor.getCurrentUserUiRoles());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('user:read:own')")
    public UserProfileCountsResponse getUserProfileCounts() {
        String userId = jwtClaimExtractor.getCurrentUserId();
        return userRepository.getUserProfileCountsResponse(userId);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('user:update:own')")
    public UserProfileResponse updateUserProfile(UpdateUserProfileRequest request) {
        String userId = jwtClaimExtractor.getCurrentUserId();

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
        response.setUsername(jwtClaimExtractor.getCurrentUsername());
        response.setEmail(jwtClaimExtractor.getCurrentUserEmail());
        response.setFamilyName(jwtClaimExtractor.getCurrentFamilyName());
        response.setGivenName(jwtClaimExtractor.getCurrentGivenName());
        response.setRoles(jwtClaimExtractor.getCurrentUserUiRoles());

        return response;
    }
}