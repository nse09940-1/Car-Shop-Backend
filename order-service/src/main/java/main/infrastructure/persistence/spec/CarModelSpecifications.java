package main.infrastructure.persistence.spec;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import main.domain.configuration.ConfigType;
import main.infrastructure.persistence.entity.CarModelJpaEntity;
import main.infrastructure.persistence.entity.InteriorOptionJpaEntity;
import main.infrastructure.persistence.entity.SteeringOptionJpaEntity;
import main.infrastructure.persistence.entity.TransmissionOptionJpaEntity;
import main.infrastructure.persistence.entity.WheelOptionJpaEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class CarModelSpecifications {

    public static Specification<CarModelJpaEntity> notRemoved() {
        return (root, query, builder) -> builder.isFalse(root.get("removed"));
    }

    public static Specification<CarModelJpaEntity> hasBrand(String brand) {
        if (brand == null || brand.isBlank()) return Specification.where(null);
        return (root, query, builder) -> builder.equal(builder.lower(root.get("brand")), brand.toLowerCase());
    }

    public static Specification<CarModelJpaEntity> hasBaseComponent(ConfigType type, UUID optionId) {
        if (type == null || optionId == null) return Specification.where(null);
        return switch (type) {
            case WHEELS -> hasWheelComponent(optionId);
            case TRANSMISSION -> hasTransmissionComponent(optionId);
            case STEERING -> hasSteeringComponent(optionId);
            case INTERIOR -> hasInteriorComponent(optionId);
        };
    }

    private static Specification<CarModelJpaEntity> hasWheelComponent(UUID optionId) {
        return (root, query, builder) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<WheelOptionJpaEntity> optionRoot = subquery.from(WheelOptionJpaEntity.class);
            var codes = optionRoot.join("compatibleModelCodes");
            subquery.select(optionRoot.get("id")).where(
                    builder.equal(optionRoot.get("id"), optionId),
                    builder.isTrue(optionRoot.get("baseOption")),
                    builder.isFalse(optionRoot.get("removed")),
                    builder.equal(codes, root.get("modelCode"))
            );
            return builder.exists(subquery);
        };
    }

    private static Specification<CarModelJpaEntity> hasTransmissionComponent(UUID optionId) {
        return (root, query, builder) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<TransmissionOptionJpaEntity> optionRoot = subquery.from(TransmissionOptionJpaEntity.class);
            var codes = optionRoot.join("compatibleModelCodes");
            subquery.select(optionRoot.get("id")).where(
                    builder.equal(optionRoot.get("id"), optionId),
                    builder.isTrue(optionRoot.get("baseOption")),
                    builder.isFalse(optionRoot.get("removed")),
                    builder.equal(codes, root.get("modelCode"))
            );
            return builder.exists(subquery);
        };
    }

    private static Specification<CarModelJpaEntity> hasSteeringComponent(UUID optionId) {
        return (root, query, builder) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<SteeringOptionJpaEntity> optionRoot = subquery.from(SteeringOptionJpaEntity.class);
            var codes = optionRoot.join("compatibleModelCodes");
            subquery.select(optionRoot.get("id")).where(
                    builder.equal(optionRoot.get("id"), optionId),
                    builder.isTrue(optionRoot.get("baseOption")),
                    builder.isFalse(optionRoot.get("removed")),
                    builder.equal(codes, root.get("modelCode"))
            );
            return builder.exists(subquery);
        };
    }

    private static Specification<CarModelJpaEntity> hasInteriorComponent(UUID optionId) {
        return (root, query, builder) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<InteriorOptionJpaEntity> optionRoot = subquery.from(InteriorOptionJpaEntity.class);
            var codes = optionRoot.join("compatibleModelCodes");
            subquery.select(optionRoot.get("id")).where(
                    builder.equal(optionRoot.get("id"), optionId),
                    builder.isTrue(optionRoot.get("baseOption")),
                    builder.isFalse(optionRoot.get("removed")),
                    builder.equal(codes, root.get("modelCode"))
            );
            return builder.exists(subquery);
        };
    }
}

