package com.trirang.model.entity;

import com.trirang.model.enums.shared.FabricType;
import com.trirang.model.enums.shared.ItemCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

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

    @NotBlank
    @Column(nullable = false)
    private String title;

    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemCategory category;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "fabric_type", nullable = false)
    private FabricType fabricType;

    @NotNull
    @Min(1)
    @Column(name = "quantity_needed", nullable = false)
    @Builder.Default
    private Integer quantityNeeded = 1;

    @NotBlank
    @Column(nullable = false)
    private String status;

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
