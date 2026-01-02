package com.example.my_books_backend.service.impl;

import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.my_books_backend.dto.user.UserProfileCountsResponse;
import com.example.my_books_backend.dto.user.UserProfileResponse;
import com.example.my_books_backend.dto.user.UserResponse;
import com.example.my_books_backend.dto.user.UpdateUserProfileRequest;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.mapper.UserMapper;
import com.example.my_books_backend.repository.UserRepository;
import com.example.my_books_backend.service.UserService;
import com.example.my_books_backend.util.JwtClaimExtractor;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtClaimExtractor jwtClaimExtractor;

    private final String DEFAULT_DISPLAY_NAME = "User";
    private final String DEFAULT_AVATAR_PATH = "/avatar00.png";

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return userMapper.toUserResponseList(users);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserResponse getUserById(@NonNull String id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
        return userMapper.toUserResponse(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteUser(@NonNull String id) {
        userRepository.deleteById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserProfileResponse getUserProfile(@NonNull String userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
            .orElseGet(() -> {
                // 表示名はJWTクレームからusernameを取得して設定
                String username = jwtClaimExtractor.getCurrentUsername();

                // 存在しない場合は自動作成する
                User newUser = new User();
                newUser.setId(userId);
                newUser.setDisplayName(username != null ? username : DEFAULT_DISPLAY_NAME);
                newUser.setAvatarPath(DEFAULT_AVATAR_PATH);
                return userRepository.save(newUser);
            });

        // レスポンス作成（JWTクレームからname関連/emailを設定）
        UserProfileResponse response = userMapper.toUserProfileResponse(user);
        response.setUsername(jwtClaimExtractor.getCurrentUsername());
        response.setEmail(jwtClaimExtractor.getCurrentUserEmail());
        response.setFamilyName(jwtClaimExtractor.getCurrentFamilyName());
        response.setGivenName(jwtClaimExtractor.getCurrentGivenName());

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserProfileCountsResponse getUserProfileCounts(@NonNull String userId) {
        return userRepository.getUserProfileCountsResponse(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserProfileResponse updateUserProfile(UpdateUserProfileRequest request, @NonNull String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        // displayName（アプリ内表示名）の更新
        String displayName = request.getDisplayName();
        if (displayName != null) {
            user.setDisplayName(displayName);
        }

        // avatarPathの更新
        String avatarPath = request.getAvatarPath();
        if (avatarPath != null) {
            user.setAvatarPath(avatarPath);
        }

        User savedUser = userRepository.save(user);

        // レスポンス作成（JWTクレームからname関連/emailを設定）
        UserProfileResponse response = userMapper.toUserProfileResponse(savedUser);
        response.setUsername(jwtClaimExtractor.getCurrentUsername());
        response.setEmail(jwtClaimExtractor.getCurrentUserEmail());
        response.setFamilyName(jwtClaimExtractor.getCurrentFamilyName());
        response.setGivenName(jwtClaimExtractor.getCurrentGivenName());

        return response;
    }
}
