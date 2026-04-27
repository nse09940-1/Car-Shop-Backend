package main.application.dto;

import main.domain.car.CarType;
import main.domain.car.DriveType;
import main.domain.car.FuelType;
import main.domain.car.TransmissionType;
import main.domain.Money;

public record CarFilterRequest(
        Money priceMin,
        Money priceMax,
        String brand,
        String model,
        CarType carType,
        FuelType fuelType,
        Integer horsepowerMin,
        Integer horsepowerMax,
        Integer engineVolumeMin,
        Integer engineVolumeMax,
        TransmissionType transmissionType,
        DriveType driveType,
        String color) {

    public static Builder builder() {
        return new Builder();
    }

    public static CarFilterRequest byBrand(String brand) {
        return builder().brand(brand).build();
    }

    public static class Builder {
        private Money priceMin, priceMax;
        private String brand, model, color;
        private CarType carType;
        private FuelType fuelType;
        private Integer horsepowerMin, horsepowerMax, engineVolumeMin, engineVolumeMax;
        private TransmissionType transmissionType;
        private DriveType driveType;

        public Builder priceMin(Money v) { this.priceMin = v; return this; }
        public Builder priceMax(Money v) { this.priceMax = v; return this; }
        public Builder brand(String v) { this.brand = v; return this; }
        public Builder model(String v) { this.model = v; return this; }
        public Builder color(String v) { this.color = v; return this; }
        public Builder bodyType(CarType v) { this.carType = v; return this; }
        public Builder fuelType(FuelType v) { this.fuelType = v; return this; }
        public Builder horsepowerMin(Integer v) { this.horsepowerMin = v; return this; }
        public Builder horsepowerMax(Integer v) { this.horsepowerMax = v; return this; }
        public Builder engineVolumeMin(Integer v) { this.engineVolumeMin = v; return this; }
        public Builder engineVolumeMax(Integer v) { this.engineVolumeMax = v; return this; }
        public Builder transmissionType(TransmissionType v) { this.transmissionType = v; return this; }
        public Builder driveType(DriveType v) { this.driveType = v; return this; }

        public CarFilterRequest build() {
            return new CarFilterRequest(priceMin, priceMax, brand, model, carType,
                    fuelType, horsepowerMin, horsepowerMax, engineVolumeMin,
                    engineVolumeMax, transmissionType, driveType, color);
        }
    }
}

