package com.linkforge.url.repository;

import com.linkforge.url.domain.ShortUrl;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, UUID> {
  Optional<ShortUrl> findByIdempotencyKey(String idempotencyKey);

  boolean existsByShortCode(String shortCode);

  List<ShortUrl> findTop100ByOwnerIdOrderByCreatedAtDesc(String ownerId);

  List<ShortUrl> findTop100ByOrderByCreatedAtDesc();
}
