package main.infrastructure.persistence.repository;

import main.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {
    @Query(value = """
            select *
            from outbox_events
            where published = false
            order by created_at
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEventJpaEntity> findPendingForPublish(@Param("limit") int limit);
}
