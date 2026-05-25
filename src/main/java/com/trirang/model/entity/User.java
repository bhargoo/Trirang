package com.trirang.model.entity;

import com.trirang.model.enums.shared.Role;
import com.trirang.model.enums.shared.VerificationBadge;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(name = "full_name", nullable = false)
    private String name;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number")
    private String phone;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "trust_score")
    @Builder.Default
    private Integer trustScore = 100;

    @Column(precision = 12, scale = 9)
    private BigDecimal latitude;

    @Column(precision = 12, scale = 9)
    private BigDecimal longitude;

    @Column(columnDefinition = "geometry(Point,4326)")
    private Point location;

    @NotNull
    @Column(name = "is_banned", nullable = false)
    @Builder.Default
    private Boolean isBanned = false;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_badge", nullable = false)
    @Builder.Default
    private VerificationBadge verificationBadge = VerificationBadge.NONE;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    private String address;

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
        syncLocation();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        syncLocation();
    }

    private void syncLocation() {
        if (latitude != null && longitude != null) {
            GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
            this.location = geometryFactory.createPoint(new Coordinate(longitude.doubleValue(), latitude.doubleValue()));
        }
    }
}
