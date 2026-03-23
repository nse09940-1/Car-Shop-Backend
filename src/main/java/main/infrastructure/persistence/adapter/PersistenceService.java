package main.infrastructure.persistence.adapter;

import main.infrastructure.persistence.entity.BaseJpaEntity;

final class PersistenceService {

    static <T extends BaseJpaEntity> void preserve(T source, T target) {
        if (source == null || target == null) {
            return;
        }
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        target.setRemoved(source.isRemoved());
    }
}

