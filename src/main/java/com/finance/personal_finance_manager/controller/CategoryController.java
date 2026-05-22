package com.finance.personal_finance_manager.controller;

import com.finance.personal_finance_manager.model.Category;
import com.finance.personal_finance_manager.model.User;
import com.finance.personal_finance_manager.repository.CategoryRepository;
import com.finance.personal_finance_manager.repository.UserRepository;
import com.finance.personal_finance_manager.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserRepository userRepository;

    // GET /api/categories?userId=1&type=CHI
    @GetMapping
    public ResponseEntity<?> getCategories(
            @RequestParam Long userId,
            @RequestParam(required = false) Category.TransactionType type) {
        List<Category> categories;
        if (type != null) {
            categories = categoryRepository.findByUser_UserIdAndType(userId, type);
        } else {
            categories = categoryRepository.findByUser_UserId(userId);
        }
        return ResponseEntity.ok(categories);
    }

    // POST /api/categories  Body: { categoryName, type, userId }
    @PostMapping
    public ResponseEntity<?> createCategory(@Valid @RequestBody Category category) {
        if (category.getUser() == null || category.getUser().getUserId() == null) {
            return ResponseEntity.badRequest().body("userId là bắt buộc");
        }
        Optional<User> userOpt = userRepository.findById(category.getUser().getUserId());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User không tồn tại");
        }
        category.setUser(userOpt.get());
        return ResponseEntity.ok(categoryRepository.save(category));
    }

    // PUT /api/categories/{id}?userId=1
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(
            @PathVariable Long id,
            @RequestParam Long userId,
            @Valid @RequestBody Category details) {
        Optional<Category> catOpt = categoryRepository.findByCategoryIdAndUser_UserId(id, userId);
        if (catOpt.isEmpty()) {
            return ResponseEntity.status(403).body("Không tìm thấy danh mục hoặc bạn không có quyền sửa");
        }
        Category cat = catOpt.get();
        cat.setCategoryName(details.getCategoryName());
        cat.setType(details.getType());
        return ResponseEntity.ok(categoryRepository.save(cat));
    }

    // DELETE /api/categories/{id}?userId=1
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id, @RequestParam Long userId) {
        try {
            categoryService.deleteCategory(id, userId);
            return ResponseEntity.ok("Đã xóa danh mục");
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
}