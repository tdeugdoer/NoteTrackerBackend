# Микросервисное приложение для возвращения в код: «Сервис заметок» и «История действий»

Отличная идея для возвращения в код. Чтобы не увязнуть в бизнес-логике, а сосредоточиться на инфраструктуре, предлагаю простую и наглядную концепцию: **«Сервис заметок» (Notes Service)** и **«Сервис истории действий» (Activity History Service)**.

Первый сервис будет управлять самими заметками (CRUD), а второй — асинхронно логировать все, что с ними происходит. Это позволит чисто отработать оба типа взаимодействия: синхронный (Feign) для получения данных и асинхронный (Kafka) для событий.

## 🏗️ Архитектура и технологии

Вот как будут распределены технологии по твоим требованиям:

| Сервис | Язык / Стек | База данных | Роль |
| :--- | :--- | :--- | :--- |
| **Notes Service** | Java, Spring Boot | **PostgreSQL** | Основной сервис. Хранит заметки, управляет ими через REST API. Выступает **продюсером** событий в Kafka. |
| **Activity History Service** | Kotlin, Spring Boot | **MongoDB** | Вспомогательный сервис. Слушает Kafka, сохраняет логи действий. Выступает **консьюмером** Kafka и **клиентом Feign** для получения деталей заметки. |

Такая архитектура соответствует паттерну, где один сервис владеет реляционными данными, а другой — документными, и они общаются через комбинацию REST и очередей.

---

## ⚙️ Как это будет работать

### 1. Notes Service (Java + PostgreSQL)

Этот сервис — «источник истины» для заметок.

*   **Модель данных (`Note`)**: `id` (Long), `title` (String), `content` (Text), `createdAt` (Timestamp).
*   **REST API (CRUD)**:
    *   `POST /api/notes` — создать заметку.
    *   `GET /api/notes/{id}` — получить заметку по ID (используется Feign'ом).
    *   `GET /api/notes` — список всех заметок.
*   **Kafka (Продюсер)**: При каждом создании, обновлении или удалении заметки сервис отправляет событие в топик `note-events`. Событие содержит `eventType` (CREATED, UPDATED, DELETED), `noteId` и `timestamp`.

### 2. Activity History Service (Kotlin + MongoDB)

Этот сервис — «журнал аудита».

*   **Модель данных (`ActivityLog`)**: `id` (String, MongoDB ObjectId), `eventType` (String), `noteId` (Long), `noteTitle` (String, получается через Feign), `eventTimestamp` (Instant), `serviceName` (String, например "notes-service").
*   **Kafka (Консьюмер)**: Слушает топик `note-events`. При получении события:
    1.  Сохраняет базовую информацию о событии в MongoDB.
    2.  Вызывает **Feign-клиент** к `Notes Service`, чтобы получить `title` заметки по `noteId` и обогатить лог.
*   **REST API**:
    *   `GET /api/history` — получить весь лог действий (опционально с фильтрацией по `noteId`).

### 🔄 Схема взаимодействия (Feign + Kafka)

1.  Клиент создает заметку через `POST /api/notes` в **Notes Service**.
2.  **Notes Service** сохраняет заметку в PostgreSQL и отправляет событие `note-created` в **Kafka**.
3.  **Activity History Service** получает событие из Kafka.
4.  **Activity History Service** синхронно вызывает **Notes Service** через **Feign** (`GET /api/notes/{id}`), чтобы получить заголовок заметки.
5.  **Activity History Service** сохраняет полную запись лога в **MongoDB**.

Этот сценарий покрывает оба требуемых типа взаимодействия и использует обе базы данных по назначению.

---

## 📝 План реализации (чек-лист)

1.  **Инфраструктура**: Подними `PostgreSQL`, `MongoDB` и `Kafka` (Kraft) через `docker-compose.yml`. Это самый быстрый способ начать.
2.  **Notes Service (Java)**:
    *   Создай Spring Boot проект (Java, Maven/Gradle) с зависимостями: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `postgresql`, `spring-kafka`.
    *   Опиши сущность, репозиторий, сервис и контроллер для CRUD.
    *   Настрой `KafkaTemplate` и добавь отправку событий в сервисном слое.
3.  **Activity History Service (Kotlin)**:
    *   Создай Spring Boot проект (Kotlin, Maven/Gradle) с зависимостями: `spring-boot-starter-web`, `spring-boot-starter-data-mongodb`, `spring-kafka`, `spring-cloud-starter-openfeign`.
    *   Опиши документ, репозиторий и контроллер для чтения логов.
    *   Настрой `@KafkaListener` для обработки событий из топика `note-events`.
    *   Создай Feign-клиент для вызова `Notes Service` и используй его внутри листенера.
4.  **Проверка**: Запусти оба сервиса, создай заметку через REST-клиент (например, Postman или `curl`) и проверь, появилась ли запись в MongoDB.

## 💡 Полезные ресурсы для вдохновения

*   **`emirhanusta/spring-boot-microservice`**: Отличный пример проекта, где используются и Java, и Kotlin, а также Feign для синхронных вызовов и Kafka для асинхронных.
*   **`PashaShulmin/Microservices`**: Проект, который показывает взаимодействие через Kafka и REST с сохранением данных в разные БД (MongoDB, PostgreSQL).
*   **`rafaelswr/ultimatespringmicroservices`**: Пример архитектуры с PostgreSQL, MongoDB, Kafka и OpenFeign.

Эта идея должна помочь тебе освежить в памяти все нужные технологии в комплексе, не отвлекаясь на сложную предметную область. Если в процессе реализации возникнут вопросы по конкретным настройкам, спрашивай.
