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
import main.domain.car.CarType;
import main.domain.car.DriveType;
import main.domain.car.FuelType;
import main.domain.car.TransmissionType;
import main.domain.configuration.ConfigType;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "car_models")
public class CarModelJpaEntity extends BaseJpaEntity {

    @Column(name = "brand", nullable = false)
    private String brand;

    @Column(name = "model_code", nullable = false)
    private String modelCode;

    @Column(name = "base_price", nullable = false)
    private long basePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "car_type", nullable = false)
    private CarType carType;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    private FuelType fuelType;

    @Column(name = "horsepower", nullable = false)
    private int horsepower;

    @Column(name = "engine_volume", nullable = false)
    private int engineVolume;

    @Enumerated(EnumType.STRING)
    @Column(name = "transmission_type", nullable = false)
    private TransmissionType transmissionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "drive_type", nullable = false)
    private DriveType driveType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "car_model_required_types", joinColumns = @JoinColumn(name = "model_id"))
    @Column(name = "required_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<ConfigType> requiredTypes = new HashSet<>();

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public long getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(long basePrice) {
        this.basePrice = basePrice;
    }

    public CarType getCarType() {
        return carType;
    }

    public void setCarType(CarType carType) {
        this.carType = carType;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    public int getHorsepower() {
        return horsepower;
    }

    public void setHorsepower(int horsepower) {
        this.horsepower = horsepower;
    }

    public int getEngineVolume() {
        return engineVolume;
    }

    public void setEngineVolume(int engineVolume) {
        this.engineVolume = engineVolume;
    }

    public TransmissionType getTransmissionType() {
        return transmissionType;
    }

    public void setTransmissionType(TransmissionType transmissionType) {
        this.transmissionType = transmissionType;
    }

    public DriveType getDriveType() {
        return driveType;
    }

    public void setDriveType(DriveType driveType) {
        this.driveType = driveType;
    }

    public Set<ConfigType> getRequiredTypes() {
        return requiredTypes;
    }

    public void setRequiredTypes(Set<ConfigType> requiredTypes) {
        this.requiredTypes = requiredTypes;
    }
}

