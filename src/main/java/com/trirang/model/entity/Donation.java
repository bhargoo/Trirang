package com.trirang.model.entity;

import com.trirang.model.enums.DonationStatus;
import com.trirang.model.enums.shared.Classification;
import com.trirang.model.enums.shared.FabricType;
import com.trirang.model.enums.shared.ItemCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "donations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false)
    private User donor;

    @NotBlank
    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "item_condition")
    private String condition;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemCategory category;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "fabric_type", nullable = false)
    private FabricType fabricType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Classification classification;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DonationStatus status;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> aiAnalysisJson;

    @Column(name = "qr_code_path")
    private String qrCodePath;

    @Column(name = "image_path")
    private String imagePath;


    @Column(precision = 12, scale = 9)
    private BigDecimal latitude;

    @Column(precision = 12, scale = 9)
    private BigDecimal longitude;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
