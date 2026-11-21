package com.inkwell.handwriting;

import com.inkwell.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A user's glyph pack (zip of cropped letters) built from their own handwriting sheets. */
@Entity
@Table(name = "handwriting_profile")
@Getter @Setter @NoArgsConstructor
public class HandwritingProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "covered_chars", nullable = false, columnDefinition = "text")
    private String coveredChars;

    @Column(name = "missing_chars", nullable = false, columnDefinition = "text")
    private String missingChars = "";

    @Column(name = "glyph_pack", nullable = false)
    private byte[] glyphPack;

    @Column(name = "consent_at", nullable = false)
    private Instant consentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
