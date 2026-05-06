package com.finance.personal_finance_manager.controller;

import com.finance.personal_finance_manager.model.Budget;
import com.finance.personal_finance_manager.repository.BudgetRepository;
import com.finance.personal_finance_manager.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private BudgetRepository budgetRepository;

    @GetMapping
    public List<Budget> getBudgets(@RequestParam Long userId) {
        return budgetRepository.findByUser_UserId(userId);
    }

    @PostMapping("/copy-last-month")
    public ResponseEntity<String> copyBudget(@RequestParam Long userId) {
        String result = budgetService.copyLastMonthBudget(userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/set-limit")
    public ResponseEntity<?> setBudgetLimit(@RequestBody Budget budget) {
        Optional<Budget> existing = budgetRepository.findFirstByUser_UserIdAndCategory_CategoryIdAndMonth(
                budget.getUser().getUserId(),
                budget.getCategory().getCategoryId(),
                budget.getMonth());

        if (existing.isPresent()) {
            Budget b = existing.get();
            b.setCategoryLimit(budget.getCategoryLimit());
            return ResponseEntity.ok(budgetRepository.save(b));
        }
        return ResponseEntity.ok(budgetRepository.save(budget));
    }
}
