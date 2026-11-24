package com.whtngml.subscription_cancel_guide.service;

import com.whtngml.subscription_cancel_guide.domain.Category;
import com.whtngml.subscription_cancel_guide.dto.CategoryForm;
import com.whtngml.subscription_cancel_guide.repository.CategoryRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 카테고리 추가·이름 수정·삭제 규칙
@SpringBootTest
@Transactional
class CategoryServiceTest {

    @Autowired
    CategoryService categoryService;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    Validator validator;

    @Test
    void 만들면_이름은_strip해서_저장() {
        Category c = categoryService.create("  배달 ", "delivery");
        assertEquals("배달", c.getName());
        assertTrue(categoryService.isSlugTaken("delivery"));
    }

    @Test
    void 이름만_바뀌고_주소는_그대로() {
        Category c = categoryService.create("배달", "delivery");
        categoryService.rename(c.getId(), " 배달·생활 ");
        assertEquals("배달·생활", categoryService.get(c.getId()).getName());
        assertEquals("delivery", categoryService.get(c.getId()).getSlug());
    }

    @Test
    void 빈_카테고리는_지운다() {
        Category c = categoryService.create("배달", "delivery");
        assertTrue(categoryService.delete(c.getId()));
        assertFalse(categoryService.isSlugTaken("delivery"));
    }

    @Test
    void 서비스가_있으면_지우지_않는다() {
        Long ott = categoryRepository.findBySlug("ott").orElseThrow().getId();
        assertFalse(categoryService.delete(ott));
        assertTrue(categoryService.isSlugTaken("ott"));
    }

    @Test
    void 목록에_서비스_개수와_기본_6개() {
        List<CategoryService.CategoryRow> rows = categoryService.getRows();
        assertEquals(List.of("ott", "music", "shopping", "ai", "life", "etc"),
                rows.stream().map(r -> r.category().getSlug()).toList());
        assertTrue(rows.get(0).serviceCount() > 0);
        assertEquals(0, rows.get(5).serviceCount());
    }

    @Test
    void 주소_형식_검사() {
        assertFalse(validate("배달", " Delivery ").isEmpty());
        assertFalse(validate("배달", "a--b").isEmpty());
        assertFalse(validate("배달", "a".repeat(31)).isEmpty());
        assertTrue(validate("배달", "food-life2").isEmpty());
    }

    private Set<ConstraintViolation<CategoryForm>> validate(String name, String slug) {
        CategoryForm form = new CategoryForm();
        form.setName(name);
        form.setSlug(slug);
        return validator.validate(form);
    }
}
