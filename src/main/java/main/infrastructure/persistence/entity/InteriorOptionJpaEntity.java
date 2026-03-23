package main.infrastructure.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "interior_options")
public class InteriorOptionJpaEntity extends BaseJpaEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "surcharge", nullable = false)
    private long surcharge;

    @Column(name = "base_option", nullable = false)
    private boolean baseOption;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "interior_option_compatible_model_codes", joinColumns = @JoinColumn(name = "option_id"))
    @Column(name = "model_code", nullable = false)
    private Set<String> compatibleModelCodes = new HashSet<>();

    @Column(name = "car_part_id", nullable = false)
    private UUID carPartId;

    @Column(name = "color", nullable = false)
    private String color;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSurcharge() {
        return surcharge;
    }

    public void setSurcharge(long surcharge) {
        this.surcharge = surcharge;
    }

    public boolean isBaseOption() {
        return baseOption;
    }

    public void setBaseOption(boolean baseOption) {
        this.baseOption = baseOption;
    }

    public Set<String> getCompatibleModelCodes() {
        return compatibleModelCodes;
    }

    public void setCompatibleModelCodes(Set<String> compatibleModelCodes) {
        this.compatibleModelCodes = compatibleModelCodes;
    }

    public UUID getCarPartId() {
        return carPartId;
    }

    public void setCarPartId(UUID carPartId) {
        this.carPartId = carPartId;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}

