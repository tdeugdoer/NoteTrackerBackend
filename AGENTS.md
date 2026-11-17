# NoteTracker — учебный микросервисный проект

Репозиторий для возвращения в код на инфраструктурных задачах: два сервиса обмениваются
данными и событиями, используются разные БД, синхронный вызов (Feign) и асинхронный (Kafka).

## Текущее состояние

| Сервис              | Стек                                     | БД         | Статус                                                                        |
|:--------------------|:-----------------------------------------|:-----------|:------------------------------------------------------------------------------|
| **note-service**    | Java 25, Spring Boot 4.1.2, Gradle       | PostgreSQL | **готов**: CRUD, Kafka-продюсер, Liquibase, Swagger, Docker, тесты            |
| **history-service** | Kotlin 2.3.21, Spring Boot 4.1.2, Gradle | MongoDB    | **готов**: Kafka-консьюмер, Feign, `GET /api/history`, Swagger, Docker, тесты |

Схема: клиент → `note-service` (PostgreSQL) → событие в Kafka `note-events` →
`history-service` (MongoDB) → Feign-запрос `GET /api/notes/{id}` в `note-service` за заголовком
заметки → запись в `activity_logs` → чтение через `GET /api/history`.

## Структура репозитория

```
NoteTracker/
├── build.gradle                 # плагины, версии сторонних библиотек (ext), общие group/version
├── settings.gradle              # include 'note-service', 'history-service'
├── gradle/wrapper/              # Gradle 9.8.2
├── gradlew, gradlew.bat
├── docker-compose.yml           # postgres, kafka, mongo, note-service, history-service
├── .dockerignore
├── .env.example                 # шаблон; .env не коммитится (.gitignore)
├── .gitattributes               # gradlew — LF, *.bat — CRLF
├── AGENTS.md
├── note-service/
│   ├── build.gradle
│   ├── Dockerfile               # multi-stage: temurin 25-jdk → 25-jre, non-root
│   └── src/
│       ├── main/java/com/tereshkevich/noteservice/
│       │   ├── NoteServiceApplication.java
│       │   ├── config/          # KafkaTopicConfig, OpenApiConfig
│       │   ├── domain/          # Note (@Entity)
│       │   ├── dto/             # NoteRequest, NoteResponse (record)
│       │   ├── event/           # NoteEvent, NoteEventPublisher
│       │   ├── mapper/          # NoteMapper (MapStruct)
│       │   ├── repository/      # NoteRepository (JpaRepository)
│       │   ├── service/         # NoteService, NoteNotFoundException
│       │   ├── util/            # константы: KafkaConstants, LogMessages, NoteMessages, OpenApiDocs
│       │   └── web/             # NoteController, ApiExceptionHandler
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/changelog/    # db.changelog-master.yaml + changes/001…003
│       └── test/java/…/service/NoteServiceTest.java
└── history-service/
    ├── build.gradle
    ├── Dockerfile               # multi-stage, по образцу note-service
    └── src/
        ├── main/kotlin/com/tereshkevich/historyservice/
        │   ├── HistoryServiceApplication.kt   # @EnableFeignClients
        │   ├── client/          # NotesClient (Feign)
        │   ├── config/          # OpenApiConfig
        │   ├── domain/          # ActivityLog (@Document)
        │   ├── dto/             # ActivityLogResponse, NoteResponse (data class)
        │   ├── event/           # NoteEvent, NoteEventListener (@KafkaListener)
        │   ├── mapper/          # ActivityLogMapper (extension-функция)
        │   ├── repository/      # ActivityLogRepository (MongoRepository)
        │   ├── service/         # HistoryService
        │   ├── util/            # ApiPaths, KafkaConstants, LogMessages, OpenApiDocs, ServiceInfo
        │   └── web/             # HistoryController
        ├── main/resources/application.yml
        └── test/kotlin/…/service/HistoryServiceTest.kt
```

## Стек и версии

- Java **25** (toolchain в обоих модулях), Gradle **9.8.2** (wrapper), Spring Boot **4.1.2**,
  `io.spring.dependency-management` **1.1.7**.
- **Kotlin 2.3.21** (плагины `org.jetbrains.kotlin.jvm` и `org.jetbrains.kotlin.plugin.spring`); версия совпадает с
  `kotlin.version` из BOM Spring Boot, поэтому задана литералом в корневом `build.gradle` вместе с плагинами.
- PostgreSQL **17** (alpine), Kafka **4.1.0** (KRaft, без ZooKeeper), MongoDB **8.0**.
- **Spring Cloud 2025.1.4** (`spring-cloud-starter-openfeign`). Трейн собран под Boot 4.0.9, но с 4.1.2 работает —
  проверено запуском (контекст поднимается, Feign-бин создаётся, вызовы идут).
- MapStruct **1.6.3**, `lombok-mapstruct-binding` **0.2.0** (только note-service), Lombok **1.18.48** (из BOM),
  springdoc-openapi **3.2.0**, Liquibase (из BOM), **MockK 1.14.12**, **Kotest 6.2.7**.
- Версии сторонних библиотек задаются **только** в корневом `build.gradle` (`ext`: `mapstructVersion`,
  `lombokMapstructBindingVersion`, `springdocVersion`, `springCloudVersion`, `mockkVersion`, `kotestVersion`), в
  модуле — `${...}`. Остальное приходит из BOM Spring Boot.

## REST API

### note-service (порт 8081)

| Метод    | Путь              | Ответ                  | Событие   |
|:---------|:------------------|:-----------------------|:----------|
| `POST`   | `/api/notes`      | `201` + `NoteResponse` | `CREATED` |
| `GET`    | `/api/notes/{id}` | `200` + `NoteResponse` | —         |
| `GET`    | `/api/notes`      | `200` + массив         | —         |
| `PUT`    | `/api/notes/{id}` | `200` + `NoteResponse` | `UPDATED` |
| `DELETE` | `/api/notes/{id}` | `204`                  | `DELETED` |

- `NoteRequest`: `title` (`@NotBlank`, `@Size(max = 255)`), `content` (`@Size(max = 10_000)`).
- `NoteResponse`: `id`, `title`, `content`, `createdAt`.
- Ошибки — `ProblemDetail` (`web/ApiExceptionHandler`): неизвестный UUID → `404` с `title = "Note not found"`;
  невалидный UUID в пути → `400` от Spring; невалидное тело → `400` с сообщениями из `NoteMessages`.
- Swagger UI: `http://localhost:8081/swagger-ui.html`, спецификация: `/v3/api-docs` и `/v3/api-docs.yaml`.

### history-service (порт 8082)

| Метод | Путь                         | Ответ                                |
|:------|:-----------------------------|:-------------------------------------|
| `GET` | `/api/history`               | `200` + массив `ActivityLogResponse` |
| `GET` | `/api/history?noteId={uuid}` | `200` + массив по заметке            |

- `ActivityLogResponse`: `id` (строка ObjectId), `eventType` (`CREATED | UPDATED | DELETED`), `noteId` (`UUID`),
  `noteTitle` (может быть `null`), `eventTimestamp` (`Instant`), `serviceName` (всегда `history-service`).
- Сортировка — по `eventTimestamp` по убыванию (и в Mongo, и в ответе).
- Невалидный `noteId` → `400` от Spring (своего handler'а в сервисе нет).
- Swagger UI: `http://localhost:8082/swagger-ui.html`, спецификация: `/v3/api-docs`.

## Модель и события

- `Note`: `id` — `UUID` (`@GeneratedValue(strategy = GenerationType.UUID)`, генерируется Hibernate до INSERT), `title` —
  `varchar(255) not null`, `content` — `text`, `created_at` — `timestamptz not null` (заполняется в `@PrePersist`).
- `id` и `createdAt` — read-only (`@Setter(AccessLevel.NONE)`).
- Kafka: топик **`note-events`** (создаётся бином `config/KafkaTopicConfig` в note-service: 1 партиция, 1 реплика), ключ
  сообщения — `noteId.toString()`, значение — JSON:
  ```json
  {"eventType":"CREATED","noteId":"<uuid>","timestamp":"2026-11-16T09:45:02.767961834Z"}
  ```
  `eventType` — `CREATED | UPDATED | DELETED`. Отправка идёт в `NoteEventPublisher` через `KafkaTemplate` с логом
  результата: при недоступном брокере `send` бросает исключение, транзакция откатывается, клиент получает `500`.
- Консьюмер (`event/NoteEventListener`): `@KafkaListener` на `note-events`, тип значения — `event/NoteEvent`
  (`data class`: `eventType`, `noteId: UUID`, `timestamp: Instant`).
    - group-id **`history-service`**, `auto-offset-reset: earliest` — при пустом group-offset читает топик с начала;
    - десериализатор — `JacksonJsonDeserializer` (Jackson 2, см. ниже), `spring.json.use.type.headers: false`
      (note-service не пишет type headers),
      `spring.json.value.default.type: com.tereshkevich.historyservice.event.NoteEvent`;
    - офсет коммитится автоматически; чтобы перечитать топик заново, удали группу:
      `docker exec notetracker-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --delete --group history-service`.
- Документ `ActivityLog` (коллекция **`activity_logs`**): `id` (ObjectId, `String?`), `eventType`, `noteId` (`UUID`,
  в Mongo — BinData subtype 4), `noteTitle` (nullable), `eventTimestamp`, `serviceName`. Имена коллекции и полей заданы
  литералами в аннотациях `@Document`/`@Field`.
- Обогащение заголовком: `HistoryService.record()` вызывает Feign `GET /api/notes/{id}`. `FeignException.NotFound`
  (заметка уже удалена) → `noteTitle = null` + `warn`; любая другая `FeignException` → `noteTitle = null` + `error`.
  Запись в Mongo сохраняется в любом случае.

## БД и миграции

- Схема note-service меняется **только** новым changeset в `note-service/src/main/resources/db/changelog/changes/`,
  подключённым в `db.changelog-master.yaml`. `ddl-auto: validate` (не `update`).
- `001-create-notes-table` — таблица `notes` (`id bigint identity`, `title`, `content`, `created_at`).
- `002-seed-notes` — три демо-заметки.
- `003-notes-id-uuid` — `id` переведён на `uuid` с сохранением данных (добавляется колонка, заполняется
  `gen_random_uuid()`, меняется PK); rollback возвращает `bigint identity` с новыми id.
- PostgreSQL: `notes/notes/notes` (db/user/password), хост-порт **5433** (нативный PostgreSQL занимает 5432).
- MongoDB: миграций нет — схему задаёт сам документ, коллекция `activity_logs` создаётся при первой записи. БД
  `history`, хост-порт **27018** (нативный `mongod` занимает 27017). В `application.yml` задано
  `spring.mongodb.representation.uuid: standard` — без него драйвер не умеет кодировать `UUID`-поля
  (`CodecConfigurationException: The uuidRepresentation has not been specified`), а `noteId` в Mongo хранится как
  BinData subtype 4.

## Конвенции кода

### note-service (Java)

- Инъекция зависимостей — конструкторная, через Lombok `@RequiredArgsConstructor`; поля `private final`.
- DTO — `record`; ошибки — `ProblemDetail`; время — `Instant`.
- Маппинг DTO ↔ entity — **MapStruct** (`mapper/NoteMapper`): `toEntity`, `updateEntity(@MappingTarget)`, `toResponse`.
  Ручных маппингов в сервисах не писать.
- Boilerplate — **Lombok** (`@Getter/@Setter/@NoArgsConstructor/@Slf4j/@UtilityClass`). В Gradle обязателен
  `lombok-mapstruct-binding` в `annotationProcessor`, иначе MapStruct не видит сгенерированные аксессоры. В IDE нужен
  плагин Lombok.
- Строковые константы — в `util/`, классы помечены `@UtilityClass` (поля объявляются как обычные `public … String`,
  Lombok делает их `static`):
    - `KafkaConstants` — имя топика;
    - `LogMessages` — шаблоны логов;
    - `NoteMessages` — тексты ошибок и сообщения валидации;
    - `OpenApiDocs` — title/description/теги/суммы операций.
      Пути эндпоинтов (`"/api/notes"`, `"/{id}"`) и имена таблицы/колонок сейчас заданы литералами в аннотациях —
      отдельного класса для них нет.
- Логирование — SLF4J через `@Slf4j`: контроллер — `debug` на входящие запросы, сервис — `info` на изменения и `debug`
  на чтение, `ApiExceptionHandler` — `warn` на 404, публикация в Kafka — `info`/`error`. Тексты — только из
  `LogMessages`.

### history-service (Kotlin)

- Вместо Java-инструментов — идиоматичный Kotlin:
    - DTO, события и документ — **`data class`** с `val`-полями (без Lombok, `record`, MapStruct);
    - инъекция — в конструкторе класса: `class HistoryService(private val repository: …, private val notesClient: …)`;
      Spring подставляет зависимости без `@Autowired`;
    - константы — **`object`** с `const val` в `util/`: `ApiPaths` (пути, в т.ч. `NOTES`/`NOTE_BY_ID` для Feign),
      `KafkaConstants`, `LogMessages`, `OpenApiDocs`, `ServiceInfo` (`history-service`); имя коллекции и полей Mongo
      задано литералами в аннотациях `ActivityLog`;
    - маппинг — **extension-функция** `ActivityLog.toResponse()` (`mapper/ActivityLogMapper.kt`); nullable-поля вместо
      `Optional`;
    - логирование — `LoggerFactory.getLogger(X::class.java)`, тексты только из `LogMessages`.
- Feign — `client/NotesClient`: интерфейс с `@FeignClient(name = NotesClient.NAME, url = "\${notes-service.url}")` и
  `@GetMapping(ApiPaths.NOTE_BY_ID)`; включается `@EnableFeignClients` на `HistoryServiceApplication`.
- Строгих null-гарантий у внешних данных нет: `noteTitle` — nullable, ошибки Feign ловятся в сервисе, а не
  пробрасываются в листенер Kafka.

### Общее для обоих модулей (Spring Boot 4)

- Автоконфигурации разнесены по модулям: для Liquibase нужен `org.springframework.boot:spring-boot-liquibase`, для
  Kafka — `org.springframework.boot:spring-boot-kafka` (одних `liquibase-core`/`spring-kafka` мало).
- JSON: Boot 4.1.2 использует **Jackson 3** (`tools.jackson`, BOM 3.1.7) для web/Feign, а `spring-kafka` — **Jackson 2**
  (`com.fasterxml`, BOM 2.21.7). Поэтому в history-service подключены оба Kotlin-модуля:
  `tools.jackson.module:jackson-module-kotlin` и `com.fasterxml.jackson.module:jackson-module-kotlin` (версии из BOM). В
  note-service в Kafka используется `JacksonJsonSerializer` с отключёнными type headers.
- **Spring Boot 4 переименовал свойства Mongo**: `spring.mongodb.*` вместо `spring.data.mongodb.*`, а
  `auto-index-creation` больше нет. Если написать по-старому, свойство молча игнорируется и приложение подключается к
  `mongodb://localhost:27017/test` — на этой машине это нативный `mongod`.

## Конфигурация

Оба `application.yml` читают `.env` из корня репозитория и из родительской папки (`spring.config.import`, формат — Java
properties). Шаблон — `.env.example`; сам `.env` в git не попадает. Проверено: запуск jar'а с изменённым
`HISTORY_SERVER_PORT` в `.env` поднимает сервис на новом порту.

| Переменная                    | Значение по умолчанию в yml              | Что задаёт                                  |
|:------------------------------|:-----------------------------------------|:--------------------------------------------|
| `DB_URL`                      | `jdbc:postgresql://localhost:5433/notes` | JDBC URL (note-service)                     |
| `DB_USERNAME` / `DB_PASSWORD` | `notes` / `notes`                        | доступ к PostgreSQL                         |
| `KAFKA_BOOTSTRAP_SERVERS`     | `localhost:19092`                        | брокер (в `.env` — `localhost:19092`)       |
| `SERVER_PORT`                 | `8081`                                   | порт note-service                           |
| `HISTORY_SERVER_PORT`         | `8082`                                   | порт history-service                        |
| `MONGODB_URI`                 | `mongodb://localhost:27018/history`      | Mongo для history-service                   |
| `NOTES_SERVICE_URL`           | `http://localhost:8081`                  | адрес note-service для Feign                |
| `LOG_LEVEL`                   | `DEBUG`                                  | уровень `com.tereshkevich.*`, root — `INFO` |
| `SPRINGDOC_ENABLED`           | `true`                                   | выключает и Swagger UI, и `/v3/api-docs`    |

## Docker и порты

| Сервис            | Контейнер                     | Порты (хост → контейнер)                                         |
|:------------------|:------------------------------|:-----------------------------------------------------------------|
| PostgreSQL 17     | `notetracker-postgres`        | `5433 → 5432`                                                    |
| Kafka 4.1 (KRaft) | `notetracker-kafka`           | `19092 → 9094` (EXTERNAL), внутри сети — `kafka:9092` (INTERNAL) |
| MongoDB 8.0       | `notetracker-mongo`           | `27018 → 27017`                                                  |
| note-service      | `notetracker-note-service`    | `8081 → 8081`                                                    |
| history-service   | `notetracker-history-service` | `8082 → 8082`                                                    |

- **Почему 19092, а не 9092:** на этой машине `9092` попадает в зарезервированный Windows-диапазон
  (`netsh int ipv4 show excludedportrange protocol=tcp`), Docker не может его забиндить (`ports are not available`).
  Kafka слушает два листенера: `INTERNAL://kafka:9092` (для контейнеров) и `EXTERNAL://localhost:19092` (для приложений
  на хосте). Если порт снова окажется занятым — поменяй хост-порт в `docker-compose.yml`, `KAFKA_ADVERTISED_LISTENERS`
  (EXTERNAL) и `KAFKA_BOOTSTRAP_SERVERS` в `.env`.
- **Почему 27018:** нативный `mongod` слушает `127.0.0.1:27017`, поэтому Mongo из compose опубликован на `27018`. Внутри
  сети сервис ходит на `mongo:27017`.
- Образы `notetracker/note-service:0.0.1-SNAPSHOT` и `notetracker/history-service:0.0.1-SNAPSHOT` собираются из
  `note-service/Dockerfile` и `history-service/Dockerfile` (Gradle-сборка внутри образа, запуск от non-root `app`; оба
  Dockerfile копируют `build.gradle` **обоих** модулей, потому что `settings.gradle` их включает).
- В compose: `note-service` получает `DB_URL=jdbc:postgresql://postgres:5432/notes`,
  `KAFKA_BOOTSTRAP_SERVERS=kafka:9092`; `history-service` — `MONGODB_URI=mongodb://mongo:27017/history`,
  `KAFKA_BOOTSTRAP_SERVERS=kafka:9092`, `NOTES_SERVICE_URL=http://note-service:8081`; ждёт `mongo` и `postgres` healthy.
- Порт 8081 занимает либо контейнер, либо `bootRun`; то же для 8082 — одновременно не запустить.

## Команды

```powershell
Copy-Item .env.example .env          # один раз, при первом запуске

./gradlew :note-service:build        # компиляция + тесты note-service
./gradlew :history-service:build     # компиляция + тесты history-service
./gradlew :note-service:test         # только тесты (БД и брокер не нужны)
./gradlew :history-service:test

docker compose up -d                 # postgres + kafka + mongo + оба сервиса (образы из кеша)
docker compose up -d --build         # пересобрать образы и поднять всё
docker compose logs -f history-service
docker compose down                  # остановить (данные остаются в volumes)

./gradlew :note-service:bootRun      # на хосте; контейнер note-service должен быть остановлен
./gradlew :history-service:bootRun   # на хосте; контейнер history-service должен быть остановлен
```

Проверка:

```powershell
curl.exe -s localhost:8081/api/notes
curl.exe -s -X POST localhost:8081/api/notes -H "Content-Type: application/json" -d '{\"title\":\"t\",\"content\":\"c\"}'
curl.exe -s localhost:8082/api/history
curl.exe -s "localhost:8082/api/history?noteId=<uuid>"

docker exec notetracker-mongo mongosh history --quiet --eval "print(db.activity_logs.countDocuments())"
docker exec notetracker-kafka /opt/kafka/bin/kafka-console-consumer.sh `
  --bootstrap-server localhost:9092 --topic note-events --from-beginning --max-messages 5
```

Типичные проблемы:

- `docker compose up -d` → `ports are not available` — см. раздел про 19092 / `excludedportrange`.
- Docker Desktop выключен — сначала запустить его, затем повторить команду.
- `Port 8081/8082 was already in use` — остановить контейнер (`docker compose stop note-service` / `history-service`)
  или `bootRun`.
- `TimeoutException: Topic note-events not present in metadata` — брокер недоступен: note-service поднимется, но запросы
  на изменение вернут `500`.
- history-service поднимается, но в Mongo пусто — проверь `MONGODB_URI` и имя свойства (`spring.mongodb.uri`, см. раздел
  про Boot 4) и что консьюмер получил партицию (`partitions assigned: [note-events-0]` в логе).
- `noteTitle` в истории `null` — либо заметка уже удалена (`warn: … is already deleted`), либо note-service недоступен
  (в логах `error: Failed to fetch note …`); сама запись при этом сохраняется.

## Тесты

- `NoteServiceTest` (JUnit 5 + Mockito + AssertJ, `@InjectMocks`) — 4 юнит-теста сервиса: создание с маппингом и
  событием `CREATED`, обновление через `updateEntity` и событие `UPDATED`, `404` для отсутствующей заметки, удаление с
  событием `DELETED`.
- `HistoryServiceTest` — **Kotest** (`FunSpec`: `context`/`test`, `beforeTest`) + **MockK**, матчеры Kotest (`shouldBe`,
  `shouldBeNull`, `shouldContainExactly`). 4 теста: запись с заголовком из Feign, запись без заголовка при
  `FeignException.NotFound` (удалённая заметка), выборка без фильтра через `findAll(Sort)`, выборка с фильтром по
  `noteId`. Моки пересоздаются в `beforeTest` — строгий MockK не переносит заглушки между тестами.
- Kotest подключается как движок JUnit Platform (`kotest-runner-junit5`), поэтому `useJUnitPlatform()` и
  `junit-platform-launcher` остаются как были; с JUnit 6 из Boot 4.1.2 работает.
- Spring-контекст, PostgreSQL, Mongo и Kafka в тестах не поднимаются. В note-service тесты остаются на JUnit 5 +
  Mockito: Kotest — Kotlin-only.

## Что дальше

1. Обработка ошибок в history-service: свой `ProblemDetail`-handler (сейчас 400 отдаёт Spring), ретраи/
   `DeadLetterPublishingRecoverer` для неудачных событий.
2. Коллекция Postman (сейчас её в репозитории нет) и CI (сборка обоих модулей + тесты).
3. `./gradlew build` в корне как единая сборка; сейчас в документации — по модулям.
4. При необходимости — индексы в Mongo (`@Indexed` / `IndexOperations`), поскольку `auto-index-creation` в Boot 4 больше
   нет.
