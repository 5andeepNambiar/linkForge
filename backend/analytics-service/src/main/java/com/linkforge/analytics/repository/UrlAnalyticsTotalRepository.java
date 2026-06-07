package com.linkforge.analytics.repository;

import com.linkforge.analytics.domain.UrlAnalyticsTotal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlAnalyticsTotalRepository extends JpaRepository<UrlAnalyticsTotal, UUID> {
  Optional<UrlAnalyticsTotal> findByShortCode(String shortCode);
}
