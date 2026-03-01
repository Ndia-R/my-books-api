package com.example.my_books_api.service.impl;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.my_books_api.repository.UserRepository;
import com.example.my_books_api.service.SubscriptionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {
    private final UserRepository userRepository;

    private static final String DEFAULT_PLAN = "FREE";

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "subscriptionPlan", key = "#userId")
    @SuppressWarnings("null")
    public @NonNull String getSubscriptionPlan(@NonNull String userId) {
        return userRepository.findSubscriptionPlanByUserId(userId)
            .orElse(DEFAULT_PLAN);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPremium(@NonNull String userId) {
        return "PREMIUM".equals(getSubscriptionPlan(userId));
    }

    @Override
    @CacheEvict(value = "subscriptionPlan", key = "#userId")
    public void evictSubscriptionPlanCache(@NonNull String userId) {
        // キャッシュ削除のみ（アノテーションで実行）
    }
}
