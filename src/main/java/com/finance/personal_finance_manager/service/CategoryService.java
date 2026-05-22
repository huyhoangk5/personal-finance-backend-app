package com.finance.personal_finance_manager.service;

import com.finance.personal_finance_manager.model.Category;
import com.finance.personal_finance_manager.repository.BudgetRepository;
import com.finance.personal_finance_manager.repository.CategoryRepository;
import com.finance.personal_finance_manager.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Transactional
    public void deleteCategory(Long categoryId, Long userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
        // Kiểm tra quyền sở hữu
        if (category.getUser() == null || !category.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa danh mục này");
        }
        // Xóa tất cả giao dịch liên quan
        transactionRepository.deleteByCategory(category);
        // Xóa tất cả ngân sách liên quan
        budgetRepository.deleteByCategory(category);
        // Xóa category
        categoryRepository.delete(category);
    }
}
