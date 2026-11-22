package com.whtngml.subscription_cancel_guide.dto;

import com.whtngml.subscription_cancel_guide.domain.Category;
import com.whtngml.subscription_cancel_guide.domain.Subscription;

import java.util.List;

// 홈 목록의 카테고리 묶음 하나 (카테고리 + 그 안의 서비스들, 이름 순)
public record CategoryGroup(Category category, List<Subscription> subscriptions) {
}
