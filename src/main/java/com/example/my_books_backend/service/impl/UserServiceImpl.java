package com.example.my_books_backend.service.impl;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.my_books_backend.dto.user.UpdateUserEmailRequest;
import com.example.my_books_backend.dto.user.UpdateUserPasswordRequest;
import com.example.my_books_backend.dto.user.CreateUserRequest;
import com.example.my_books_backend.dto.user.UserProfileCountsResponse;
import com.example.my_books_backend.dto.user.UserProfileResponse;
import com.example.my_books_backend.dto.user.UserResponse;
import com.example.my_books_backend.dto.user.UpdateUserProfileRequest;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.exception.ConflictException;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.mapper.UserMapper;
import com.example.my_books_backend.repository.UserRepository;
import com.example.my_books_backend.service.UserService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private final String DEFAULT_AVATAR_PATH = "";

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
    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
        return userMapper.toUserResponse(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public User createUser(CreateUserRequest request) {
        User user = new User();
        user.setId(request.getId()); // Keycloak UUID
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setAvatarPath(request.getAvatarPath());

        if (user.getName() == null) {
            String name = "USER_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            user.setName(name);
        }

        if (user.getAvatarPath() == null) {
            String avatarPath = DEFAULT_AVATAR_PATH;
            user.setAvatarPath(avatarPath);
        }

        User savedUser = userRepository.save(user);
        return savedUser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserProfileResponse getUserProfile(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
        return userMapper.toUserProfileResponse(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserProfileCountsResponse getUserProfileCounts(String userId) {
        return userRepository.getUserProfileCountsResponse(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void updateUserProfile(UpdateUserProfileRequest request, String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        String name = request.getName();
        String avatarPath = request.getAvatarPath();

        if (name != null) {
            user.setName(name);
        }
        if (avatarPath != null) {
            user.setAvatarPath(avatarPath);
        }
        userRepository.save(user);
    }

    /**
     * {@inheritDoc}
     * Note: Keycloak認証では、メールアドレス変更はKeycloak側で管理されます。
     * このメソッドはアプリケーション内のユーザー情報を同期するためのものです。
     */
    @Override
    @Transactional
    public void updateUserEmail(UpdateUserEmailRequest request, String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        String email = request.getEmail();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("このメールアドレスは既に登録されています。: " + email);
        }

        // Keycloak側でメールアドレス変更が完了した後、
        // アプリケーション側のユーザー情報を同期
        user.setEmail(email);
        userRepository.save(user);
    }

    /**
     * {@inheritDoc}
     * Note: Keycloak認証では、パスワード変更はKeycloak側で管理されます。
     * このメソッドは将来的に削除される予定です。
     */
    @Override
    @Transactional
    public void updateUserPassword(UpdateUserPasswordRequest request, String userId) {
        // Keycloak認証では、パスワード管理はKeycloak側で行われるため、
        // このメソッドは使用されません
        throw new UnsupportedOperationException(
            "パスワード変更はKeycloakの管理画面で行ってください。"
        );
    }
}
