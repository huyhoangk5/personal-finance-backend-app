package com.finance.personal_finance_manager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @NotNull(message = "Số tiền không được để trống")
    private Double amount;

    @NotNull(message = "Ngày giao dịch không được để trống")
    private LocalDateTime date;

    private String note;

    @NotNull(message = "Loại giao dịch không được để trống")
    @Enumerated(EnumType.STRING)
    private Category.TransactionType type;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}
