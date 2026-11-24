package com.whtngml.subscription_cancel_guide.controller;

import com.whtngml.subscription_cancel_guide.domain.Category;
import com.whtngml.subscription_cancel_guide.dto.CategoryForm;
import com.whtngml.subscription_cancel_guide.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    // 머리글에서 지금 보고 있는 메뉴 표시
    @ModelAttribute("adminMenu")
    public String adminMenu() {
        return "categories";
    }

    private final CategoryService categoryService;
    private final SmartValidator validator;

    // ---------- 목록 + 추가 ----------

    @GetMapping
    public String list(Model model) {
        model.addAttribute("form", new CategoryForm());
        return showList(model);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CategoryForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasFieldErrors("slug") && categoryService.isSlugTaken(form.getSlug())) {
            bindingResult.rejectValue("slug", "duplicate", "이미 있는 주소예요.");
        }
        if (bindingResult.hasErrors()) {
            return showList(model);
        }

        categoryService.create(form.getName(), form.getSlug());
        redirectAttributes.addFlashAttribute("message", "카테고리를 추가했어요.");
        return "redirect:/admin/categories";
    }

    private String showList(Model model) {
        model.addAttribute("rows", categoryService.getRows());
        return "admin/categories";
    }

    // ---------- 이름 수정 ----------

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Category category = categoryService.get(id);
        CategoryForm form = new CategoryForm();
        form.setName(category.getName());
        form.setSlug(category.getSlug());
        model.addAttribute("category", category);
        model.addAttribute("form", form);
        return "admin/category-form";
    }

    @PostMapping("/{id}/edit")
    public String rename(@PathVariable Long id,
                         @ModelAttribute("form") CategoryForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Category category = categoryService.get(id);
        // 주소는 바꾸지 않으므로 저장된 값을 넣고 검사한다
        form.setSlug(category.getSlug());
        validator.validate(form, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("category", category);
            return "admin/category-form";
        }

        categoryService.rename(id, form.getName());
        redirectAttributes.addFlashAttribute("message", "카테고리 이름을 고쳤어요.");
        return "redirect:/admin/categories";
    }

    // ---------- 삭제 ----------

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean deleted = categoryService.delete(id);
        redirectAttributes.addFlashAttribute("message",
                deleted ? "카테고리를 지웠어요." : "서비스가 있는 카테고리는 지울 수 없어요.");
        return "redirect:/admin/categories";
    }
}
