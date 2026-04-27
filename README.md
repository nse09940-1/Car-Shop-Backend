# Car-Shop-Backend

## Стек

- `Java 21`
- `Spring Boot 3.3`
- `Spring Web`
- `Spring Data JPA`
- `Spring Security`
- `JWT`
- `Keycloak`
- `PostgreSQL`
- `Liquibase`
- `Apache Kafka`
- `MapStruct`
- `Gradle Multi-Module`
- `JUnit 5`
- `Testcontainers`
- `OpenAPI`
- `Docker`

## О проекте

`Car-Shop-Backend` — микросервисный backend для автосалона. Сервисы взаимодействуют через `Kafka`, построены на гексагональной архитектуре , используют `JWT`-аутентификацию и ролевую `Security`, а межсервисные изменения состояний публикуются с помощью `outbox pattern`.

Система разделена на два сервиса с отдельными бд:

- `order-service` — заказы, статусы, конфигуратор, тест-драйвы, менеджерский контур.
- `storage-service` — автомобили, детали, резервирование, списание, сборочные задания.
- `shared-contracts` — общие Kafka-контракты: `EventEnvelope`, `EventType`, `OrderType`, payload-объекты.

У обоих сервисов гексагональная архитектура:

- `application` — сервисы , порты;
- `domain` — бизнес-логика;
- `infrastructure` — JPA, Kafka, Security, HTTP-клиенты;
- `presentation` — REST API.

## Взаимодействие сервисов

В проекте используются два типа взаимодействия:

- HTTP — для синхронных `read-only` проверок;
- Kafka — для для асинхронной обработки событий.

### Заказ автомобиля из наличия

1. Клиент создаёт заказ в `order-service`.
2. Сервис сохраняет `StockCarOrder` со статусом `CREATED`.
3. В той же транзакции пишет в `outbox_events` событие `STOCK_CAR_RESERVATION_REQUESTED`.
4. `OrderOutboxPublisher` публикует событие в `order.events`.
5. `storage-service` получает событие, проверяет наличие автомобиля и резервирует его: `available=false`.
6. После этого `storage-service` публикует `STOCK_CAR_RESERVED` или `STOCK_CAR_RESERVATION_REJECTED`.
7. `order-service` обрабатывает ответ склада и переводит заказ либо в `MANAGER_APPROVED`, либо в `CANCELLED`.

После оплаты поток продолжается аналогично:

1. При переходе в `PAID` `order-service` пишет `STOCK_CAR_WRITE_OFF_REQUESTED`.
2. `storage-service` завершает списание автомобиля и публикует `STOCK_CAR_WRITTEN_OFF` или `STOCK_CAR_WRITE_OFF_REJECTED`.
3. `order-service` переводит заказ в `READY_FOR_HANDOVER` либо отменяет его.

### Кастомный заказ

1. Клиент создаёт заказ с набором опций.
2. `order-service` валидирует совместимость через `CompatibilityPolicy`, считает цену и собирает `requiredParts`.
3. Заказ сохраняется со статусом `CREATED`, затем в outbox пишется `ORDER_SENT_FOR_APPROVAL`.
4. `storage-service` получает событие и проверяет, хватает ли деталей.
5. Если деталей хватает, они резервируются, а `assembly_order` переводится в `RESERVED`.
6. В ответ публикуется `ORDER_APPROVED`; если деталей не хватает — `ORDER_REJECTED`.
7. `order-service` переводит заказ в `WAREHOUSE_APPROVED` либо `CANCELLED`.

После оплаты:

1. `order-service` публикует `ORDER_EXECUTION_REQUESTED`.
2. `storage-service` не резервирует детали повторно, а списывает уже зарезервированные и переводит сборку в `IN_PROGRESS`.
3. В ответ публикуется `ORDER_EXECUTION_STARTED`.
4. `order-service` переводит заказ в `AWAITING_DELIVERY`.

Для отмены до запуска исполнения используется отдельный поток:

- `order-service` публикует `ORDER_RESERVATION_RELEASE_REQUESTED`;
- `storage-service` снимает резерв деталей;
- повторная обработка безопасна за счёт идемпотентности.

### Тест-драйв

Сценарий тест-драйва построен через синхронный вызов:

1. Клиент создаёт заявку в `order-service`.
2. `order-service` вызывает `storage-service` по `/internal/cars/{id}`.
3. Проверяется, существует ли автомобиль, доступен ли он и включён ли в тест-драйвный пул.
4. Только после этого заявка сохраняется.

## Messaging и консистентность

### Outbox Pattern

В проекте реализован `outbox pattern`: доменное изменение и запись в `outbox_events` происходят в одной транзакции, а публикация в Kafka вынесена в отдельный scheduled publisher. Это исключает расхождение между зафиксированным состоянием в БД и отправкой события в брокер.

### Идемпотентность

Kafka-consumer'ы сначала проверяют `processed_events` по `eventId`. Если событие уже обработано, оно игнорируется. Это делает повторную доставку безопасной.

### Trace ID

Оба сервиса используют `TraceIdFilter` и `TraceIdProvider`:

- принимают `X-Trace-Id` из входящего запроса или генерируют новый;
- кладут его в `MDC`;
- передают тот же `traceId` в `EventEnvelope`.

Это позволяет связать HTTP-запрос и Kafka-события одним `traceId`.

## Security

Оба сервиса используют Spring Security для валидации JWT-токенов, выпущенных `Keycloak`.

### Роли

В realm `auto-shop` используются роли:

- `USER`
- `MANAGER`
- `WAREHOUSE_ADMIN`
- `ADMIN`

Роли извлекаются из `realm_access.roles` через `KeycloakRealmRoleConverter`.

### Идентификация пользователя

Для бизнес-логики используется claim `app_user_id`:

- `SecurityCurrentUserProvider` читает его из JWT;
- значение должно быть `UUID`;
- этот `UUID` используется в проверках владельца заказа и при работе с доменными пользователями.

Это разделяет identity-провайдер и доменную модель пользователей.

### Авторизация в `order-service`

На уровне HTTP:

- `/api/admin/**` — `ADMIN`;
- создание заказов — `USER` или `ADMIN`;
- просмотр и изменение заказов — `USER`, `MANAGER`, `ADMIN`;
- создание тест-драйва — `USER` или `ADMIN`;
- просмотр заявок на тест-драйв — `MANAGER` или `ADMIN`.

На уровне application-сервисов:

- `order-service` дополнительно проверяет, что пользователь существует в app_users; если у него нет роли `ADMIN`, то для создания заказа и заявки на тест-драйв его доменная роль должна быть `CLIENT`.
- допустимые переходы статусов проверяются отдельно через `DefaultOrderTransitionPolicy`.

На уровне объекта:

- `OrderAccessEvaluator` проверяет владельца заказа;
- клиент может читать и оплачивать только свои заказы;
- клиент может отменять заказ только на разрешённых стадиях;
- менеджер может менять только разрешённые бизнес-статусы;
- администратор имеет полный доступ.

Авторизация сочетает ролевые проверки, ownership и контроль допустимого состояния заказа.

### Авторизация в `storage-service`

- `/internal/**` открыт для внутреннего синхронного чтения;
- `/api/assembly-orders/**` — `WAREHOUSE_ADMIN` или `ADMIN`;
- `/api/parts/**` — `WAREHOUSE_ADMIN` или `ADMIN`;
- остальной `/api/**` контур также ограничен ролями склада и администратора.

### Ошибки

Для `401` и `403` используются отдельные JSON handlers:

- `RestAuthenticationEntryPoint`
- `RestAccessDeniedHandler`

API возвращает JSON-ответы вместо стандартных HTML-страниц Spring Security.


## Тестирование

Есть два уровня тестов:

- unit-тесты для domain, policies, services, security components и repository-адаптеров;
- integration tests через `Testcontainers` для `PostgreSQL`, `Kafka`, миграций, security-ограничений и outbox-сценариев.

Покрыты в том числе:

- права владельца заказа;
- права менеджера, склада и администратора;
- резервирование и списание деталей;
- обработка Kafka-событий;
- применение Liquibase-миграций;
- security-поведение на HTTP-уровне.
