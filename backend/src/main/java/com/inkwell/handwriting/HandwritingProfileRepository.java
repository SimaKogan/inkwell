package com.inkwell.handwriting;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HandwritingProfileRepository extends JpaRepository<HandwritingProfile, Long> {

    @Query("""
            select new com.inkwell.handwriting.ProfileDto(p.id, p.name, p.coveredChars, p.missingChars, p.createdAt)
            from HandwritingProfile p where p.owner.id = :uid order by p.createdAt desc""")
    List<ProfileDto> findProfiles(@Param("uid") Long ownerId);

    Optional<HandwritingProfile> findByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerId(Long ownerId);
}
