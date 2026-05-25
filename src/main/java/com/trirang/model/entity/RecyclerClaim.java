package com.trirang.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "recycler_claims",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"donation_id"}, name = "uc_recycler_claims_donation")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecyclerClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recycler_id", nullable = false)
    private User recycler;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id", nullable = false)
    private Donation donation;

    @NotNull
    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "CLAIMED";

    @PrePersist
    protected void onCreate() {
        if (claimedAt == null) {
            claimedAt = Instant.now();
        }
        if (status == null) {
            status = "CLAIMED";
        }
    }
}
