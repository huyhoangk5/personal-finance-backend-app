package com.finance.personal_finance_manager.controller;

import com.finance.personal_finance_manager.dto.TransactionResponse;
import com.finance.personal_finance_manager.model.Category;
import com.finance.personal_finance_manager.model.Transaction;
import com.finance.personal_finance_manager.service.BudgetService;
import com.finance.personal_finance_manager.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    // GET http://localhost:8080/api/transactions?userId=1
    @GetMapping
    public List<Transaction> getAll(@RequestParam Long userId) {
        return transactionService.getAllTransactionsByUser(userId);
    }

    // POST http://localhost:8080/api/transactions
    @PostMapping
    public ResponseEntity<?> createTransaction(@Valid @RequestBody Transaction transaction) {
        if (transaction.getAmount() != null && transaction.getAmount() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số tiền phải lớn hơn 0"));
        }
        // userId đã nằm trong object transaction gửi lên
        TransactionResponse response = transactionService.saveTransaction(transaction);
        return ResponseEntity.ok(response);
    }

    // GET http://localhost:8080/api/transactions/search?note=An+trua&userId=1
    @GetMapping("/search")
    public List<Transaction> search(@RequestParam String note, @RequestParam Long userId) {
        return transactionService.searchByNote(note, userId);
    }

    // GET http://localhost:8080/api/transactions/recent?userId=1
    @GetMapping("/recent")
    public List<Transaction> getRecent(@RequestParam Long userId) {
        return transactionService.getRecentTransactions(userId);
    }

    // DELETE http://localhost:8080/api/transactions/10?userId=1
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id, @RequestParam Long userId) {
        boolean deleted = transactionService.deleteTransaction(id, userId);
        if (deleted) {
            return ResponseEntity.ok("Xóa giao dịch thành công!");
        } else {
            return ResponseEntity.status(403).body("Không tìm thấy giao dịch hoặc bạn không có quyền xóa!");
        }
    }

    @Autowired
    private BudgetService budgetService;

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody Transaction transaction,
            @RequestParam Long userId) {

        if (transaction.getAmount() != null && transaction.getAmount() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số tiền phải lớn hơn 0"));
        }

        // Kiểm tra giao dịch có tồn tại và thuộc về user không
        Optional<Transaction> existingOpt = transactionService.getTransactionById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Transaction existing = existingOpt.get();
        if (!existing.getUser().getUserId().equals(userId)) {
            return ResponseEntity.status(403).body("Bạn không có quyền sửa giao dịch này");
        }

        // Cập nhật các trường
        existing.setAmount(transaction.getAmount());
        existing.setDate(transaction.getDate());
        existing.setNote(transaction.getNote());
        existing.setType(transaction.getType());
        if (transaction.getCategory() != null) {
            existing.setCategory(transaction.getCategory());
        }

        Transaction updated = transactionService.updateTransaction(existing); // bạn cần thêm method này trong service

        // Kiểm tra cảnh báo ngân sách nếu là chi tiêu
        String message = "Cập nhật thành công";
        if (updated.getType() == Category.TransactionType.CHI) {
            message = budgetService.checkBudgetExceeded(
                    userId,
                    updated.getCategory().getCategoryId(),
                    updated.getAmount()
            );
        }

        return ResponseEntity.ok(new TransactionResponse(updated, message));
    }

    @GetMapping("/spending-by-category-month")
    public Map<String, Double> getSpendingByCategoryAndMonth(@RequestParam Long userId, @RequestParam String month) {
        // month format: yyyy-MM
        return transactionService.getSpendingByCategoryAndMonth(userId, month);
    }
}