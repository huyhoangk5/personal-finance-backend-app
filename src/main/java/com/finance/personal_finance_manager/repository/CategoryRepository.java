package com.finance.personal_finance_manager.repository;

import com.finance.personal_finance_manager.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Lấy danh mục theo loại (toàn bộ - fallback)
    List<Category> findByType(Category.TransactionType type);

    // Lấy danh mục theo userId
    List<Category> findByUser_UserId(Long userId);

    // Lấy danh mục theo userId và loại
    List<Category> findByUser_UserIdAndType(Long userId, Category.TransactionType type);

    // Tìm theo tên (có thể mở rộng sau)
    List<Category> findByCategoryNameContainingIgnoreCase(String name);

    // Tìm category theo id và userId (để kiểm tra ownership)
    Optional<Category> findByCategoryIdAndUser_UserId(Long categoryId, Long userId);
}