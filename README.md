# Car Shop Backend

`Car Shop Backend` - серверная часть автосалона на Spring Boot.
Сервис покрывает каталог автомобилей, конфигуратор комплектаций, заказы (из наличия и под заказ), учет деталей на складе и работу с тест-драйвами.

Проект построен вокруг доменной модели и явных бизнес-правил: совместимость опций с моделью, валидные переходы статусов заказов, резервирование/списание деталей и ролевые ограничения для пользователей.

## Возможности

- Каталог автомобилей с фильтрацией по цене, бренду, модели и техническим параметрам
- Конфигуратор: базовая комплектация модели
- Конфигуратор: доступные опции по модели
- Конфигуратор: расчет итоговой цены
- Конфигуратор: проверка совместимости выбранных опций
- Оформление заказа автомобиля из наличия
- Оформление кастомного заказа по выбранной конфигурации
- Управление жизненным циклом заказов через policy-слой
- Автоматическое резервирование, освобождение и списание деталей для кастомных заказов
- CRUD API для деталей
- Управление списком автомобилей для тест-драйва и заявками клиентов
- Административные API для моделей, автомобилей, пользователей, опций, заказов и тест-драйвов
- Swagger UI для интерактивного просмотра и тестирования API

## Технологии и стек

- Java 21
- Spring Boot 3.3.5
- Spring Web
- Spring Data JPA
- PostgreSQL 16
- Liquibase
- MapStruct
- SpringDoc OpenAPI (`swagger-ui`)
- Gradle
- JUnit 5
- Spring Boot Test

## Архитектура

Архитектура гексагональная:

- `domain`: доменные сущности, value objects, инварианты, исключения
- `application`: use-case сервисы, DTO, порты (`repository`, `policy`), маппинг
- `infrastructure`: JPA-адаптеры, persistence entity, Spring Data репозитории, policy-реализации
- `presentation`: REST-контроллеры и глобальная обработка ошибок


## Быстрый старт

### 1) Поднять PostgreSQL

```bash
docker compose -f docker/docker-compose.yml up -d
```

Параметры БД по умолчанию:

- database: `auto_shop`
- username: `postgres`
- password: `postgres`
- port: `5432`

### 2) Запустить приложение

Linux/macOS:

```bash
./gradlew bootRun
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

Приложение стартует на `http://localhost:8080`.

### 3) Открыть Swagger UI

- `http://localhost:8080/swagger-ui.html`

## Конфигурация

Переменные окружения:

- `APP_DB_URL` (по умолчанию: `jdbc:postgresql://localhost:5432/auto_shop`)
- `APP_DB_USER` (по умолчанию: `postgres`)
- `APP_DB_PASSWORD` (по умолчанию: `postgres`)
- `APP_HTTP_PORT` (по умолчанию: `8080`)

Liquibase применяется автоматически при запуске:

- создание схемы
- индексы и ограничения
- seed-данные (пользователи, модели, автомобили, детали, опции)

## Тестирование

Unit и сервисные тесты:

```bash
./gradlew test
```

Интеграционные тесты (Testcontainers + PostgreSQL):

```bash
./gradlew integrationTest
```


## Основные API-группы

### Каталог

- `GET /api/cars`
- `GET /api/cars/{id}`

### Конфигуратор

- `GET /api/configurator/{modelId}/base`
- `GET /api/configurator/{modelId}/options`
- `POST /api/configurator/{modelId}/build`
- `GET /api/configurator/base-configurations`

### Заказы

- `POST /api/orders/stock`
- `PATCH /api/orders/stock/{id}/status`
- `GET /api/orders/stock/{id}`
- `GET /api/orders/stock`
- `POST /api/orders/custom`
- `PATCH /api/orders/custom/{id}/status`
- `GET /api/orders/custom/{id}`
- `GET /api/orders/custom`

### Детали

- `POST /api/parts`
- `GET /api/parts/{id}`
- `GET /api/parts`
- `PUT /api/parts/{id}`
- `DELETE /api/parts/{id}`

### Тест-драйв

- `POST /api/test-drives/requests`
- `GET /api/test-drives/requests`
- `POST /api/test-drives/cars/{carId}`
- `DELETE /api/test-drives/cars/{carId}`
- `GET /api/test-drives/cars`

### Администрирование

Все административные методы доступны под префиксом `/api/admin/*`:

- модели автомобилей
- автомобили
- пользователи
- детали
- опции (`wheels`, `transmissions`, `steerings`, `interiors`)
- заказы (stock/custom)
- заявки на тест-драйв

## Сборка и Docker

Сборка jar:

```bash
./gradlew clean bootJar
```

Сборка Docker-образа:

```bash
docker build -f docker/Dockerfile -t car-shop-backend .
```

Запуск контейнера приложения:

```bash
docker run --rm -p 8080:8080 ^
  -e APP_DB_URL=jdbc:postgresql://host.docker.internal:5432/auto_shop ^
  -e APP_DB_USER=postgres ^
  -e APP_DB_PASSWORD=postgres ^
  car-shop-backend
```



