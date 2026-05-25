package com.trirang.model.entity;

import com.trirang.model.enums.VerificationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "artisan_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtisanVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artisan_id", nullable = false)
    private User artisan;

    @NotBlank
    @Column(name = "government_id_image_url", nullable = false, length = 500)
    private String governmentIdImageUrl;

    @NotBlank
    @Column(name = "selfie_image_url", nullable = false, length = 500)
    private String selfieImageUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "artisan_verification_workspace_images",
        joinColumns = @JoinColumn(name = "verification_id")
    )
    @Column(name = "image_url", nullable = false, length = 500)
    private List<String> workspaceImageUrls;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @NotNull
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
