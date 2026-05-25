package com.trirang.model.entity;

import com.trirang.model.enums.shared.FabricType;
import com.trirang.model.enums.shared.ItemCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "artisan_requirements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtisanRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artisan_id", nullable = false)
    private User artisan;

    @Column
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column
    private ItemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "fabric_type")
    private FabricType fabricType;

    @Column
    @Builder.Default
    private String status = "OPEN";

    // Phase 5A required fields
    @NotBlank
    @Column(nullable = false)
    private String material;

    @NotNull
    @Min(1)
    @Column(name = "quantity_needed", nullable = false)
    private Integer quantity;

    @NotBlank
    @Column(nullable = false)
    private String purpose;

    @NotNull
    @Column(nullable = false)
    private Integer urgency;

    @NotNull
    @Column(name = "radius_km", nullable = false)
    private Double radiusKm;

    @NotNull
    @Column(precision = 12, scale = 9, nullable = false)
    private BigDecimal latitude;

    @NotNull
    @Column(precision = 12, scale = 9, nullable = false)
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
        if (status == null) {
            status = "OPEN";
        }
        if (title == null) {
            title = material + " for " + purpose;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

