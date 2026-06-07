package com.linkforge.redirect.repository;

import com.linkforge.redirect.domain.ShortUrlProjection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortUrlLookupRepository extends JpaRepository<ShortUrlProjection, UUID> {
  Optional<ShortUrlProjection> findByShortCode(String shortCode);
}
