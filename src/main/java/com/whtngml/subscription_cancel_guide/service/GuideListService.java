package com.whtngml.subscription_cancel_guide.service;

import com.whtngml.subscription_cancel_guide.domain.Category;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import com.whtngml.subscription_cancel_guide.dto.CategoryGroup;
import com.whtngml.subscription_cancel_guide.repository.CategoryRepository;
import com.whtngml.subscription_cancel_guide.repository.GuideStepRepository;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 사용자 화면 목록(홈·카테고리·검색)에 필요한 데이터
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuideListService {

    private final CategoryRepository categoryRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final GuideStepRepository guideStepRepository;

    // 카테고리 id 순으로 묶는다. 서비스가 하나도 없는 카테고리는 뺀다.
    public List<CategoryGroup> groupedByCategory() {
        List<Subscription> subscriptions = subscriptionRepository.findAllByOrderByNameAsc();
        List<CategoryGroup> groups = new ArrayList<>();
        for (Category category : categoryRepository.findAll(Sort.by("id"))) {
            // Category에 equals가 없으므로 id로 비교한다
            List<Subscription> inCategory = subscriptions.stream()
                    .filter(s -> s.getCategory().getId().equals(category.getId()))
                    .toList();
            if (!inCategory.isEmpty()) {
                groups.add(new CategoryGroup(category, inCategory));
            }
        }
        return groups;
    }

    // key: 서비스 id, value: 단계 수. 단계가 없는 서비스는 Map에 없다.
    public Map<Long, Long> stepCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : guideStepRepository.countStepsBySubscription()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }
}
