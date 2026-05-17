package com.finance.personal_finance_manager.service;

import com.finance.personal_finance_manager.model.Category;
import com.finance.personal_finance_manager.repository.CategoryRepository;
import com.finance.personal_finance_manager.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // Thống kê chi tiêu theo danh mục của từng User
    public Map<String, Double> getSpendingStats(Long userId) {
        Map<String, Double> stats = new HashMap<>();

        categoryRepository.findAll().stream()
                .filter(cat -> cat.getType() == Category.TransactionType.CHI)
                .forEach(cat -> stats.put(cat.getCategoryName(), 0.0));

        List<Object[]> results = transactionRepository.sumAmountByCategoryAndUser(userId);
        for (Object[] result : results) {
            stats.put((String) result[0], (Double) result[1]);
        }

        return stats;
    }

    // Tính số dư (Balance) của từng User
    public Map<String, Double> getBalanceStats(Long userId) {
        Double totalIncomes = transactionRepository.sumAmountByTypeAndUser(Category.TransactionType.THU, userId);
        Double totalExpenses = transactionRepository.sumAmountByTypeAndUser(Category.TransactionType.CHI, userId);

        totalIncomes = (totalIncomes != null) ? totalIncomes : 0.0;
        totalExpenses = (totalExpenses != null) ? totalExpenses : 0.0;

        Map<String, Double> stats = new HashMap<>();
        stats.put("totalIncomes", totalIncomes);
        stats.put("totalExpenses", totalExpenses);
        stats.put("balance", totalIncomes - totalExpenses);

        return stats;
    }

    public Map<String, Double> getSpendingByCategoryAndMonth(Long userId, String month) {
        Map<String, Double> stats = new HashMap<>();
        List<Category> categories = categoryRepository.findByType(Category.TransactionType.CHI);
        for (Category cat : categories) {
            stats.put(cat.getCategoryName(), 0.0);
        }
        List<Object[]> results = transactionRepository.sumAmountByCategoryAndUserAndMonth(userId, month);
        for (Object[] result : results) {
            stats.put((String) result[0], (Double) result[1]);
        }
        return stats;
    }

    public Map<String, Object> getMonthlySummary(Long userId, int months) {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        for (int i = 0; i < months; i++) {
            LocalDate monthDate = now.minusMonths(i);
            String monthStr = monthDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            // SỬA: thứ tự tham số đúng: type, userId, month
            Double income = transactionRepository.sumAmountByTypeAndUserAndMonth(Category.TransactionType.THU, userId, monthStr);
            Double expense = transactionRepository.sumAmountByTypeAndUserAndMonth(Category.TransactionType.CHI, userId, monthStr);
            income = income != null ? income : 0.0;
            expense = expense != null ? expense : 0.0;
            Map<String, Double> monthData = new HashMap<>();
            monthData.put("income", income);
            monthData.put("expense", expense);
            monthData.put("balance", income - expense);
            result.put(monthStr, monthData);
        }
        return result;
    }

    public List<Map<String, Object>> getTopSpendingCategories(Long userId, int limit) {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<Object[]> results = transactionRepository.sumAmountByCategoryAndUserAndMonth(userId, currentMonth);
        List<Map<String, Object>> top = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("category", row[0]);
            item.put("amount", row[1]);
            top.add(item);
        }
        top.sort((a, b) -> Double.compare((Double) b.get("amount"), (Double) a.get("amount")));
        return top.stream().limit(limit).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getTrend(Long userId, int months) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            String monthStr = monthDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            // SỬA: thứ tự tham số đúng: type, userId, month
            Double income = transactionRepository.sumAmountByTypeAndUserAndMonth(Category.TransactionType.THU, userId, monthStr);
            Double expense = transactionRepository.sumAmountByTypeAndUserAndMonth(Category.TransactionType.CHI, userId, monthStr);
            income = income != null ? income : 0.0;
            expense = expense != null ? expense : 0.0;
            Map<String, Object> point = new HashMap<>();
            point.put("month", monthStr);
            point.put("income", income);
            point.put("expense", expense);
            trend.add(point);
        }
        return trend;
    }

    public Map<String, Double> getBalanceStatsByMonth(Long userId, String month) {
        Double totalIncomes = transactionRepository.sumAmountByTypeAndUserAndMonth(Category.TransactionType.THU, userId, month);
        Double totalExpenses = transactionRepository.sumAmountByTypeAndUserAndMonth(Category.TransactionType.CHI, userId, month);
        totalIncomes = totalIncomes != null ? totalIncomes : 0.0;
        totalExpenses = totalExpenses != null ? totalExpenses : 0.0;
        Map<String, Double> stats = new HashMap<>();
        stats.put("totalIncomes", totalIncomes);
        stats.put("totalExpenses", totalExpenses);
        stats.put("balance", totalIncomes - totalExpenses);
        return stats;
    }

    public Map<String, Double> getIncomeByCategoryAndMonth(Long userId, String month) {
        Map<String, Double> stats = new HashMap<>();
        // Khởi tạo tất cả danh mục THU với giá trị 0
        List<Category> categories = categoryRepository.findByType(Category.TransactionType.THU);
        for (Category cat : categories) {
            stats.put(cat.getCategoryName(), 0.0);
        }
        List<Object[]> results = transactionRepository.sumIncomeByCategoryAndUserAndMonth(userId, month);
        for (Object[] result : results) {
            stats.put((String) result[0], (Double) result[1]);
        }
        return stats;
    }

    public Map<String, Map<String, Double>> getDailySummary(Long userId, String month) {
        // month format: yyyy-MM
        List<Object[]> incomes = transactionRepository.sumAmountByDayAndType(userId, Category.TransactionType.THU, month);
        List<Object[]> expenses = transactionRepository.sumAmountByDayAndType(userId, Category.TransactionType.CHI, month);

        Map<String, Map<String, Double>> dailyMap = new HashMap<>();
        // Khởi tạo tất cả các ngày trong tháng với giá trị 0
        YearMonth yearMonth = YearMonth.parse(month);
        int lastDay = yearMonth.lengthOfMonth();
        for (int day = 1; day <= lastDay; day++) {
            String dateStr = month + "-" + String.format("%02d", day);
            Map<String, Double> dayData = new HashMap<>();
            dayData.put("income", 0.0);
            dayData.put("expense", 0.0);
            dailyMap.put(dateStr, dayData);
        }
        // Đổ dữ liệu thu
        for (Object[] row : incomes) {
            java.sql.Date sqlDate = (java.sql.Date) row[0];
            Double amount = (Double) row[1];
            String dateStr = sqlDate.toLocalDate().toString();
            if (dailyMap.containsKey(dateStr)) {
                dailyMap.get(dateStr).put("income", amount);
            }
        }
        // Đổ dữ liệu chi
        for (Object[] row : expenses) {
            java.sql.Date sqlDate = (java.sql.Date) row[0];
            Double amount = (Double) row[1];
            String dateStr = sqlDate.toLocalDate().toString();
            if (dailyMap.containsKey(dateStr)) {
                dailyMap.get(dateStr).put("expense", amount);
            }
        }
        return dailyMap;
    }
}