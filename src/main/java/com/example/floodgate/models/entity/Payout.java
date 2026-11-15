package com.example.floodgate.models.entity;


import com.example.floodgate.models.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payout_id", nullable = false, unique = true, length = 64)
    private String payoutId;

    /** Full account — mask before logging or returning in responses. */
    @Column(name = "source_account", nullable = false, length = 50)
    private String sourceAccount;

    /** Full account — mask before logging or returning in responses. */
    @Column(name = "beneficiary_account", nullable = false, length = 50)
    private String beneficiaryAccount;

    @Column(name = "beneficiary_ifsc", nullable = false, length = 11)
    private String beneficiaryIfsc;

    /** Full PAN — mask before logging or returning in responses. May be null. */
    @Column(name = "beneficiary_pan", length = 10)
    private String beneficiaryPan;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 10)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayoutStatus status;

    /**
     * Rule codes that fired during screening.
     * Stored as a Postgres TEXT[] array; H2 test schema uses ARRAY.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> reasons = new ArrayList<>();

    /** Event time from the caller — drives the fan-in rolling window. */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /** Wall-clock time the service made its decision. */
    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;
}

