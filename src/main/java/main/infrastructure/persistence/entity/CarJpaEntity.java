package main.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "cars")
public class CarJpaEntity extends BaseJpaEntity {

    @Column(name = "vin", nullable = false)
    private String vin;

    @Column(name = "car_model_id", nullable = false)
    private UUID carModelId;

    @Column(name = "color", nullable = false)
    private String color;

    @Column(name = "price", nullable = false)
    private long price;

    @Column(name = "available", nullable = false)
    private boolean available;

    @Column(name = "available_for_test_drive", nullable = false)
    private boolean availableForTestDrive;

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public UUID getCarModelId() {
        return carModelId;
    }

    public void setCarModelId(UUID carModelId) {
        this.carModelId = carModelId;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isAvailableForTestDrive() {
        return availableForTestDrive;
    }

    public void setAvailableForTestDrive(boolean availableForTestDrive) {
        this.availableForTestDrive = availableForTestDrive;
    }
}

