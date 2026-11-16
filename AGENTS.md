# NoteTracker — учебный микросервисный проект

Репозиторий для возвращения в код на инфраструктурных задачах: два сервиса обмениваются
данными и событиями, используются разные БД, синхронный вызов (Feign) и асинхронный (Kafka).

## Текущее состояние

| Сервис | Стек | БД | Статус |
| :--- | :--- | :--- | :--- |
| **note-service** | Java 25, Spring Boot 4.1.2, Gradle | PostgreSQL | **готов**: CRUD, Kafka-продюсер, Liquibase, Swagger, Docker, тесты |
| **history-service** | Kotlin, Spring Boot | MongoDB | **не создан** |

Итоговая схема, к которой идём: клиент → `note-service` (PostgreSQL) → событие в Kafka →
`history-service` (MongoDB) → Feign-запрос в `note-service` за заголовком заметки → запись аудита в Mongo.

## Структура репозитория

```
NoteTracker/
├── build.gradle                 # версии сторонних библиотек (ext), общие group/version
├── settings.gradle              # include 'note-service'
├── gradle/wrapper/              # Gradle 9.8.2
├── gradlew, gradlew.bat
├── docker-compose.yml           # postgres, kafka, note-service
├── .dockerignore
├── .env.example                 # шаблон; .env не коммитится (.gitignore)
├── .gitattributes               # gradlew — LF, *.bat — CRLF
├── AGENTS.md
└── note-service/
    ├── build.gradle
    ├── Dockerfile               # multi-stage: temurin 25-jdk → 25-jre, non-root
    └── src/
        ├── main/java/com/tereshkevich/noteservice/
        │   ├── NoteServiceApplication.java
        │   ├── config/          # KafkaTopicConfig, OpenApiConfig
        │   ├── domain/          # Note (@Entity)
        │   ├── dto/             # NoteRequest, NoteResponse (record)
        │   ├── event/           # NoteEvent, NoteEventPublisher
        │   ├── mapper/          # NoteMapper (MapStruct)
        │   ├── repository/      # NoteRepository (JpaRepository)
        │   ├── service/         # NoteService, NoteNotFoundException
        │   ├── util/            # константы: KafkaConstants, LogMessages, NoteMessages, OpenApiDocs
        │   └── web/             # NoteController, ApiExceptionHandler
        ├── main/resources/
        │   ├── application.yml
        │   └── db/changelog/    # db.changelog-master.yaml + changes/001…003
        └── test/java/…/service/NoteServiceTest.java
```

## Стек и версии

- Java **25** (toolchain), Gradle **9.8.2** (wrapper), Spring Boot **4.1.2**, `io.spring.dependency-management` **1.1.7**.
- PostgreSQL **17** (alpine), Kafka **4.1.0** (KRaft, без ZooKeeper).
- MapStruct **1.6.3**, Lombok **1.18.48** (из BOM Spring Boot), `lombok-mapstruct-binding` **0.2.0**, springdoc-openapi **3.2.0**, Liquibase (из BOM).
- Версии сторонних библиотек задаются **только** в корневом `build.gradle` (`ext`: `mapstructVersion`, `lombokMapstructBindingVersion`, `springdocVersion`), в модуле — `${...}`. Остальное приходит из BOM Spring Boot.

## REST API (`note-service`, порт 8081)

| Метод | Путь | Ответ | Событие |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/notes` | `201` + `NoteResponse` | `CREATED` |
| `GET` | `/api/notes/{id}` | `200` + `NoteResponse` | — |
| `GET` | `/api/notes` | `200` + массив | — |
| `PUT` | `/api/notes/{id}` | `200` + `NoteResponse` | `UPDATED` |
| `DELETE` | `/api/notes/{id}` | `204` | `DELETED` |

- `NoteRequest`: `title` (`@NotBlank`, `@Size(max = 255)`), `content` (`@Size(max = 10_000)`).
- `NoteResponse`: `id`, `title`, `content`, `createdAt`.
- Ошибки — `ProblemDetail` (`web/ApiExceptionHandler`): неизвестный UUID → `404` с `title = "Note not found"`; невалидный UUID в пути → `400` от Spring; невалидное тело → `400` с сообщениями из `NoteMessages`.
- Swagger UI: `http://localhost:8081/swagger-ui.html`, спецификация: `/v3/api-docs` и `/v3/api-docs.yaml`.

## Модель и события

- `Note`: `id` — `UUID` (`@GeneratedValue(strategy = GenerationType.UUID)`, генерируется Hibernate до INSERT), `title` — `varchar(255) not null`, `content` — `text`, `created_at` — `timestamptz not null` (заполняется в `@PrePersist`).
- `id` и `createdAt` — read-only (`@Setter(AccessLevel.NONE)`).
- Kafka: топик **`note-events`** (создаётся бином `config/KafkaTopicConfig`: 1 партиция, 1 реплика), ключ сообщения — `noteId.toString()`, значение — JSON:
  ```json
  {"eventType":"CREATED","noteId":"<uuid>","timestamp":"2026-11-16T09:45:02.767961834Z"}
  ```
  `eventType` — `CREATED | UPDATED | DELETED`. Отправка идёт в `NoteEventPublisher` через `KafkaTemplate` с логом результата: при недоступном брокере `send` бросает исключение, транзакция откатывается, клиент получает `500`.

## БД и миграции

- Схема меняется **только** новым changeset в `note-service/src/main/resources/db/changelog/changes/`, подключённым в `db.changelog-master.yaml`. `ddl-auto: validate` (не `update`).
- `001-create-notes-table` — таблица `notes` (`id bigint identity`, `title`, `content`, `created_at`).
- `002-seed-notes` — три демо-заметки.
- `003-notes-id-uuid` — `id` переведён на `uuid` с сохранением данных (добавляется колонка, заполняется `gen_random_uuid()`, меняется PK); rollback возвращает `bigint identity` с новыми id.
- БД: `notes/notes/notes` (db/user/password), хост-порт **5433** (нативный PostgreSQL занимает 5432).

## Конвенции кода

- Инъекция зависимостей — конструкторная, через Lombok `@RequiredArgsConstructor`; поля `private final`.
- DTO — `record`; ошибки — `ProblemDetail`; время — `Instant`.
- Маппинг DTO ↔ entity — **MapStruct** (`mapper/NoteMapper`): `toEntity`, `updateEntity(@MappingTarget)`, `toResponse`. Ручных маппингов в сервисах не писать.
- Boilerplate — **Lombok** (`@Getter/@Setter/@NoArgsConstructor/@Slf4j/@UtilityClass`). В Gradle обязателен `lombok-mapstruct-binding` в `annotationProcessor`, иначе MapStruct не видит сгенерированные аксессоры. В IDE нужен плагин Lombok.
- Строковые константы — в `util/`, классы помечены `@UtilityClass` (поля объявляются как обычные `public … String`, Lombok делает их `static`):
  - `KafkaConstants` — имя топика;
  - `LogMessages` — шаблоны логов;
  - `NoteMessages` — тексты ошибок и сообщения валидации;
  - `OpenApiDocs` — title/description/теги/суммы операций.
  Пути эндпоинтов (`"/api/notes"`, `"/{id}"`) и имена таблицы/колонок сейчас заданы литералами в аннотациях — отдельного класса для них нет.
- Логирование — SLF4J через `@Slf4j`: контроллер — `debug` на входящие запросы, сервис — `info` на изменения и `debug` на чтение, `ApiExceptionHandler` — `warn` на 404, публикация в Kafka — `info`/`error`. Тексты — только из `LogMessages`.
- Spring Boot 4: автоконфигурации разнесены по модулям. Для Liquibase нужен `org.springframework.boot:spring-boot-liquibase`, для Kafka — `org.springframework.boot:spring-boot-kafka` (одних `liquibase-core`/`spring-kafka` мало). JSON — Jackson 3 (`tools.jackson`), в Kafka используется `JacksonJsonSerializer` с отключёнными type headers.

## Конфигурация

`application.yml` читает `.env` из корня репозитория и из родительской папки (`spring.config.import`, формат — Java properties). Шаблон — `.env.example`; сам `.env` в git не попадает.

| Переменная | Значение по умолчанию в yml | Что задаёт |
| :--- | :--- | :--- |
| `DB_URL` | `jdbc:postgresql://localhost:5433/notes` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `notes` / `notes` | доступ к БД |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | брокер (в `.env` стоит `localhost:19092`) |
| `SERVER_PORT` | `8081` | порт приложения |
| `LOG_LEVEL` | `DEBUG` | уровень `com.tereshkevich.noteservice` (root — `INFO`) |
| `SPRINGDOC_ENABLED` | `true` | выключает и Swagger UI, и `/v3/api-docs` |

## Docker и порты

| Сервис | Контейнер | Порты (хост → контейнер) |
| :--- | :--- | :--- |
| PostgreSQL 17 | `notetracker-postgres` | `5433 → 5432` |
| Kafka 4.1 (KRaft) | `notetracker-kafka` | `19092 → 9094` (EXTERNAL), внутри сети — `kafka:9092` (INTERNAL) |
| note-service | `notetracker-note-service` | `8081 → 8081` |

- **Почему 19092, а не 9092:** на этой машине `9092` попадает в зарезервированный Windows-диапазон (`netsh int ipv4 show excludedportrange protocol=tcp`), Docker не может его забиндить (`ports are not available`). Kafka слушает два листенера: `INTERNAL://kafka:9092` (для контейнеров) и `EXTERNAL://localhost:19092` (для приложений на хосте). Если порт снова окажется занятым — поменяй хост-порт в `docker-compose.yml`, `KAFKA_ADVERTISED_LISTENERS` (EXTERNAL) и `KAFKA_BOOTSTRAP_SERVERS` в `.env`.
- Образ `notetracker/note-service:0.0.1-SNAPSHOT` собирается из `note-service/Dockerfile` (Gradle-сборка внутри образа, запуск от non-root `app`, ~420 МБ).
- `note-service` в compose получает `DB_URL=jdbc:postgresql://postgres:5432/notes` и `KAFKA_BOOTSTRAP_SERVERS=kafka:9092`, ждёт `postgres` healthy.
- Порт 8081 занимает либо контейнер, либо `bootRun` — одновременно не запустить.

## Команды

```powershell
Copy-Item .env.example .env          # один раз, при первом запуске

./gradlew :note-service:build        # компиляция + тесты
./gradlew :note-service:test         # только тесты (БД и брокер не нужны)

docker compose up -d                 # postgres + kafka + note-service (образ из кеша)
docker compose up -d --build         # пересобрать образ note-service и поднять всё
docker compose logs -f note-service
docker compose down                  # остановить (данные остаются в volume)

./gradlew :note-service:bootRun      # приложение на хосте; контейнер note-service должен быть остановлен
```

Проверка:

```powershell
curl.exe -s localhost:8081/api/notes
curl.exe -s -X POST localhost:8081/api/notes -H "Content-Type: application/json" -d '{\"title\":\"t\",\"content\":\"c\"}'
docker exec notetracker-kafka /opt/kafka/bin/kafka-console-consumer.sh `
  --bootstrap-server localhost:9092 --topic note-events --from-beginning --max-messages 5
```

Типичные проблемы:

- `docker compose up -d` → `ports are not available` — см. раздел про 19092 / `excludedportrange`.
- Docker Desktop выключен — сначала запустить его, затем повторить команду.
- `Port 8081 was already in use` — остановить контейнер (`docker compose stop note-service`) или `bootRun`.
- `TimeoutException: Topic note-events not present in metadata` — брокер недоступен: приложение поднимется, но запросы на изменение вернут `500`.

## Тесты

`NoteServiceTest` (JUnit 5 + Mockito + AssertJ, `@InjectMocks`) — 4 юнит-теста сервиса:
создание с маппингом и событием `CREATED`, обновление через `updateEntity` и событие `UPDATED`,
`404` для отсутствующей заметки, удаление с событием `DELETED`.
Spring-контекст, БД и Kafka в тестах не поднимаются.

## Что дальше

1. `history-service/` (Kotlin, Spring Boot): документ `ActivityLog` (`eventType`, `noteId`, `noteTitle`, `eventTimestamp`, `serviceName`), репозиторий Mongo, `@KafkaListener` на `note-events`, Feign-клиент на `GET /api/notes/{id}`, эндпоинт `GET /api/history` (с фильтром по `noteId`).
2. Добавить MongoDB в `docker-compose.yml` и конфигурацию `history-service` (свой `.env`, брокер `kafka:9092`).
3. Dockerfile/образ для `history-service` по образцу `note-service`.
4. Интеграционная проверка: `POST /api/notes` → событие → запись в Mongo → `GET /api/history`.
5. При необходимости — коллекция Postman (сейчас её в репозитории нет) и CI.
