package com.linkforge.analytics.repository;

import com.linkforge.analytics.domain.ClickEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickEventRepository extends JpaRepository<ClickEventEntity, UUID> {
  boolean existsByKafkaPartitionAndKafkaOffset(Integer kafkaPartition, Long kafkaOffset);

  List<ClickEventEntity> findTop50ByShortCodeOrderByOccurredAtDesc(String shortCode);
}
