package com.finance.personal_finance_manager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    @NotBlank(message = "Tên danh mục không được để trống")
    @Column(nullable = false)
    private String categoryName;

    @NotNull(message = "Loại danh mục không được để trống")
    @Enumerated(EnumType.STRING)
    private TransactionType type; // THU hoặc CHI

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public enum TransactionType {
        THU, CHI
    }
}
