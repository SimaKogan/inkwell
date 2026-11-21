package com.inkwell.handwriting;

import java.time.Instant;

/** Listing projection: deliberately excludes the glyph pack blob. */
public record ProfileDto(Long id, String name, String coveredChars, String missingChars, Instant createdAt) {}
