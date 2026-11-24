package com.whtngml.subscription_cancel_guide.service;

import com.whtngml.subscription_cancel_guide.domain.Category;
import com.whtngml.subscription_cancel_guide.repository.CategoryRepository;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// 카테고리 추가·이름 수정·삭제 규칙을 한 곳에 둔다
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SubscriptionRepository subscriptionRepository;

    // 관리 페이지 한 줄: 카테고리와 그 안의 서비스 개수
    public record CategoryRow(Category category, long serviceCount) {
    }

    public List<CategoryRow> getRows() {
        return categoryRepository.findAll(Sort.by("id")).stream()
                .map(c -> new CategoryRow(c, subscriptionRepository.countByCategory(c)))
                .toList();
    }

    public Category get(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public boolean isSlugTaken(String slug) {
        return categoryRepository.existsBySlug(slug);
    }

    @Transactional
    public Category create(String name, String slug) {
        return categoryRepository.save(new Category(name.strip(), slug));
    }

    @Transactional
    public void rename(Long id, String name) {
        get(id).rename(name.strip());
    }

    // 서비스가 있으면 지우지 않고 false를 돌려준다 (다른 탭에서 서비스를 넣었을 수 있어 서버에서 다시 확인)
    @Transactional
    public boolean delete(Long id) {
        Category category = get(id);
        if (subscriptionRepository.countByCategory(category) > 0) {
            return false;
        }
        categoryRepository.delete(category);
        return true;
    }
}
