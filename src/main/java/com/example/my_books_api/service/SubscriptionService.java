package com.example.my_books_api.service;

import org.springframework.lang.NonNull;

public interface SubscriptionService {
    @NonNull
    String getSubscriptionPlan(@NonNull String userId);

    boolean isPremium(@NonNull String userId);

    void evictSubscriptionPlanCache(@NonNull String userId);
}
