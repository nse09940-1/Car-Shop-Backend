package main;

import main.infrastructure.persistence.entity.BaseJpaEntity;
import main.infrastructure.persistence.entity.CarModelJpaEntity;
import main.infrastructure.persistence.entity.CustomOrderJpaEntity;
import main.infrastructure.persistence.entity.ProcessedEventJpaEntity;
import main.infrastructure.persistence.entity.InteriorOptionJpaEntity;
import main.infrastructure.persistence.entity.OutboxEventJpaEntity;
import main.infrastructure.persistence.entity.SteeringOptionJpaEntity;
import main.infrastructure.persistence.entity.StockOrderJpaEntity;
import main.infrastructure.persistence.entity.TestDriveJpaEntity;
import main.infrastructure.persistence.entity.TransmissionOptionJpaEntity;
import main.infrastructure.persistence.entity.UserJpaEntity;
import main.infrastructure.persistence.entity.WheelOptionJpaEntity;
import main.infrastructure.persistence.repository.*;
import main.infrastructure.persistence.repository.ProcessedEventJpaRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackageClasses = {
        BaseJpaEntity.class,
        CarModelJpaEntity.class,
        UserJpaEntity.class,
        WheelOptionJpaEntity.class,
        TransmissionOptionJpaEntity.class,
        SteeringOptionJpaEntity.class,
        InteriorOptionJpaEntity.class,
        StockOrderJpaEntity.class,
        CustomOrderJpaEntity.class,
        TestDriveJpaEntity.class,
        OutboxEventJpaEntity.class,
        ProcessedEventJpaEntity.class
})
@EnableJpaRepositories(basePackageClasses = {
        CarModelJpaRepository.class,
        UserJpaRepository.class,
        WheelOptionJpaRepository.class,
        TransmissionOptionJpaRepository.class,
        SteeringOptionJpaRepository.class,
        InteriorOptionJpaRepository.class,
        StockOrderJpaRepository.class,
        CustomOrderJpaRepository.class,
        TestDriveJpaRepository.class,
        OutboxEventJpaRepository.class,
        ProcessedEventJpaRepository.class
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
