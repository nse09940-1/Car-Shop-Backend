package main.infrastructure.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import main.domain.configuration.ConfigType;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "parts")
public class PartJpaEntity extends BaseJpaEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "part_number", nullable = false)
    private String partNumber;

    @Column(name = "price", nullable = false)
    private long price;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "part_compatible_model_codes", joinColumns = @JoinColumn(name = "part_id"))
    @Column(name = "model_code", nullable = false)
    private Set<String> compatibleModelCodes = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "part_type", nullable = false)
    private ConfigType partType;

    @Column(name = "in_stock", nullable = false)
    private int inStock;

    @Column(name = "reserved", nullable = false)
    private int reserved;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public Set<String> getCompatibleModelCodes() {
        return compatibleModelCodes;
    }

    public void setCompatibleModelCodes(Set<String> compatibleModelCodes) {
        this.compatibleModelCodes = compatibleModelCodes;
    }

    public ConfigType getPartType() {
        return partType;
    }

    public void setPartType(ConfigType partType) {
        this.partType = partType;
    }

    public int getInStock() {
        return inStock;
    }

    public void setInStock(int inStock) {
        this.inStock = inStock;
    }

    public int getReserved() {
        return reserved;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }
}

