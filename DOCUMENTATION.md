# Explore With Me — Полная документация проекта

## Содержание
1. [Обзор проекта](#1-обзор-проекта)
2. [Архитектура](#2-архитектура)
3. [Структура модулей](#3-структура-модулей)
4. [Сервис статистики (stat)](#4-сервис-статистики-stat)
   - 4.1 [stat-dto — общие DTO](#41-stat-dto--общие-dto)
   - 4.2 [stat-server — HTTP-сервер](#42-stat-server--http-сервер)
   - 4.3 [stat-client — HTTP-клиент](#43-stat-client--http-клиент)
5. [Main Service](#5-main-service)
6. [База данных](#6-база-данных)
7. [REST API сервиса статистики](#7-rest-api-сервиса-статистики)
8. [Поток данных (Data Flow)](#8-поток-данных-data-flow)
9. [Конфигурация и запуск](#9-конфигурация-и-запуск)
10. [Тестирование](#10-тестирование)
11. [Качество кода](#11-качество-кода)
12. [Технологический стек](#12-технологический-стек)

---

## 1. Обзор проекта

**Explore With Me** — это обучающий проект на Java/Spring Boot, реализующий микросервисную архитектуру для платформы совместных мероприятий. Пользователи могут создавать события и находить компанию для их посещения.

На текущей стадии разработки (`stat_svc` ветка) реализован **сервис статистики** — компонент, который отслеживает количество просмотров каждого URI и предоставляет агрегированную аналитику. Main-сервис пока является заглушкой.

### Зачем нужен сервис статистики?
Каждый раз, когда пользователь просматривает событие, main-service отправляет «hit» (обращение) в stat-server. Впоследствии main-service может запросить, сколько раз и сколькими уникальными пользователями просматривалось то или иное событие.

---

## 2. Архитектура

```
┌─────────────────────────────────────────────────────────────┐
│                        Docker Compose                       │
│                                                             │
│  ┌──────────────────┐         ┌──────────────────────────┐  │
│  │   main-service   │──HTTP──▶│      stats-server        │  │
│  │   (порт 8080)    │◀──HTTP──│      (порт 9090)         │  │
│  └────────┬─────────┘         └──────────┬───────────────┘  │
│           │                              │                   │
│           ▼                              ▼                   │
│  ┌─────────────────┐         ┌──────────────────────────┐  │
│  │  main-service-  │         │    stat-server-db         │  │
│  │       db        │         │  PostgreSQL (порт 6542)   │  │
│  │  PostgreSQL      │         │  БД: statdb              │  │
│  │  (порт 6541)    │         └──────────────────────────┘  │
│  │  БД: mainservice│                                        │
│  └─────────────────┘                                        │
└─────────────────────────────────────────────────────────────┘
```

**Взаимодействие сервисов:**
- `main-service` при каждом просмотре события вызывает `POST /hit` на stat-server через `StatClient`
- `main-service` запрашивает статистику просмотров через `GET /stats`
- Оба сервиса имеют изолированные базы данных PostgreSQL

---

## 3. Структура модулей

```
ExploreWithMe/                        ← корневой Maven-модуль (POM)
├── pom.xml                           ← родительский POM (Spring Boot 3.3, Java 21)
├── docker-compose.yml                ← описание всех 4 контейнеров
├── checkstyle.xml                    ← правила стиля кода
├── suppressions.xml                  ← исключения для SpotBugs
├── lombok.config                     ← конфигурация Lombok
│
├── main-service/                     ← модуль основного сервиса (заглушка)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/.../MainApplication.java   ← пустая точка входа
│       ├── resources/application.properties
│       └── resources/schema.sql            ← пустая схема БД
│
└── stat/                             ← родительский POM для статистики
    ├── pom.xml
    ├── stat-dto/                     ← общие DTO (библиотека)
    │   ├── pom.xml
    │   └── src/main/java/.../dto/
    │       ├── EndpointHit.java
    │       └── ViewStats.java
    │
    ├── stat-server/                  ← HTTP-сервер статистики
    │   ├── pom.xml
    │   ├── Dockerfile
    │   └── src/
    │       ├── main/java/.../
    │       │   ├── StatApplication.java
    │       │   ├── controller/StatsController.java
    │       │   ├── service/StatsService.java
    │       │   ├── repository/
    │       │   │   ├── HitRepository.java        ← интерфейс
    │       │   │   └── JdbcHitRepository.java    ← реализация на JDBC
    │       │   ├── model/HitEntity.java
    │       │   └── exception/
    │       │       ├── BadRequestException.java
    │       │       ├── ErrorResponse.java
    │       │       └── GlobalExceptionHandler.java
    │       └── test/java/.../
    │           ├── controller/StatsControllerIntegrationTest.java
    │           └── repository/JdbcHitRepositoryIntegrationTest.java
    │
    └── stat-client/                  ← HTTP-клиент для main-service
        ├── pom.xml
        └── src/main/java/.../client/
            ├── StatClient.java
            └── StatClientConfig.java
```

---

## 4. Сервис статистики (stat)

### 4.1 stat-dto — общие DTO

Библиотека без Spring-зависимостей. Используется как зависимость в `stat-server` и `stat-client`.

#### `EndpointHit` — входящее обращение

```java
public record EndpointHit(
    Long id,              // ID (может быть null при создании)
    @NotBlank String app, // название приложения, н-р "ewm-main-service"
    @NotBlank String uri, // путь запроса, н-р "/events/1"
    @NotBlank String ip,  // IP-адрес клиента, н-р "192.168.0.1"
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime timestamp // дата и время обращения
)
```

Формат JSON при сериализации/десериализации даты: `"2026-03-01 12:00:00"`.
Аннотации `@NotBlank` и `@NotNull` используются при валидации входящих запросов на сервере.

#### `ViewStats` — агрегированный результат

```java
public record ViewStats(
    String app,   // название приложения
    String uri,   // URI
    Long hits     // количество обращений
)
```

---

### 4.2 stat-server — HTTP-сервер

Spring Boot приложение, порт **9090**.

#### Слой контроллера — `StatsController`

```
StatsController
├── POST /hit       → void          (HTTP 201 Created)
└── GET  /stats     → List<ViewStats> (HTTP 200 OK)
```

Контроллер принимает HTTP-запросы, делегирует всю логику в `StatsService`.

#### Слой бизнес-логики — `StatsService`

Отвечает за:
1. **Сохранение hit**: конвертирует `EndpointHit` DTO → `HitEntity` → передаёт в репозиторий
2. **Получение статистики**:
   - Парсит строки `start` и `end` в `LocalDateTime` по шаблону `yyyy-MM-dd HH:mm:ss`
   - Валидирует, что `start` ≤ `end` (иначе `BadRequestException`)
   - Делегирует в репозиторий

```java
// Формат дат строго фиксирован:
private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
```

#### Слой данных — `HitRepository` / `JdbcHitRepository`

Используется **чистый JDBC** (через `JdbcTemplate`), без JPA/Hibernate.

**Интерфейс:**
```java
public interface HitRepository {
    HitEntity save(HitEntity hit);
    List<ViewStats> getStats(LocalDateTime start, LocalDateTime end,
                             List<String> uris, boolean unique);
}
```

**Реализация `JdbcHitRepository`:**

Метод `save`:
```sql
INSERT INTO stat(app, uri, ip, created) VALUES (?, ?, ?, ?)
```

Метод `getStats` динамически строит SQL в зависимости от параметров:

| `unique` | SQL агрегация |
|----------|---------------|
| `false`  | `COUNT(*)` — все обращения |
| `true`   | `COUNT(DISTINCT ip)` — только уникальные IP |

Если передан список `uris`, добавляется условие `AND uri IN (?, ?, ...)`.

Итоговый запрос сортирует результаты по `hits DESC, app ASC, uri ASC`.

#### Модель — `HitEntity`

```java
public record HitEntity(
    Long id,            // ID (null при создании — генерирует БД)
    String app,
    String uri,
    String ip,
    LocalDateTime created
)
```

#### Обработка ошибок — `GlobalExceptionHandler`

| Исключение | HTTP статус | Описание |
|------------|-------------|----------|
| `BadRequestException` | 400 | неверные параметры запроса (даты, порядок start/end) |
| `MethodArgumentNotValidException` | 400 | провал Bean Validation (`@NotBlank`, `@NotNull`) |
| `HttpMessageNotReadableException` | 400 | некорректный JSON в теле запроса |

Все ошибки возвращаются в формате:
```json
{ "error": "описание ошибки" }
```

---

### 4.3 stat-client — HTTP-клиент

Библиотека (без `main`-класса). Подключается в `main-service` как зависимость.

#### `StatClientConfig`

Создаёт бин `RestTemplate`, настроенный на адрес stat-server из свойства `stats-server.url`:

```java
@Bean
public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
        .build();
}
```

#### `StatClient`

Предоставляет два метода для main-service:

**Отправить hit:**
```java
public void saveHit(EndpointHit hitDto)
// → POST /hit (тело: JSON EndpointHit)
// Логирует: "Сохранено обращение: ..."
```

**Получить статистику:**
```java
public List<ViewStats> getStat(
    LocalDateTime start,
    LocalDateTime end,
    List<String> uris,   // nullable — если null, возвращает по всем URI
    boolean unique       // true = только уникальные IP
)
// → GET /stats?start=...&end=...&uris=...&unique=...
// Возвращает пустой список если ответ null
```

Даты форматируются как `yyyy-MM-dd HH:mm:ss`.

---

## 5. Main Service

На данной стадии — **заглушка**. `MainApplication.java` при запуске только выводит:
```
Main service is not implemented on stat_svc stage.
```

В `application.properties` уже предусмотрены настройки:
- `server.port=8080`
- `stats-server.url=http://localhost:9090` — адрес stat-server
- Подключение к PostgreSQL `mainservicedb`

`schema.sql` — пустой файл (схема будет добавлена при реализации main-service).

---

## 6. База данных

### stat-server: таблица `stat`

```sql
CREATE TABLE IF NOT EXISTS stat (
    id      BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    app     TEXT NOT NULL,     -- имя приложения
    uri     TEXT NOT NULL,     -- запрошенный URI
    ip      TEXT NOT NULL,     -- IP клиента
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL  -- время обращения
);
```

Индексы не заданы явно — при нагрузке стоит добавить индекс по `created` и `uri`.

**Параметры подключения (прод):**
- URL: `jdbc:postgresql://stat-server-db:5432/statdb`
- User: `stat` / Password: `stat`

**Параметры подключения (локально):**
- URL: `jdbc:postgresql://localhost:6542/statdb`
- User: `stat` / Password: `stat`

### main-service: база `mainservicedb`

- URL: `jdbc:postgresql://main-service-db:5432/mainservicedb`
- User: `mainservice` / Password: `mainservice`
- Схема: пока не определена

---

## 7. REST API сервиса статистики

Базовый URL: `http://localhost:9090`

### `POST /hit` — Сохранить обращение

Записывает факт обращения к URI.

**Тело запроса:**
```json
{
  "app": "ewm-main-service",
  "uri": "/events/1",
  "ip": "192.168.0.1",
  "timestamp": "2026-03-01 12:00:00"
}
```

| Поле | Тип | Обязательно | Описание |
|------|-----|-------------|----------|
| `app` | String | Да | Название приложения |
| `uri` | String | Да | Запрошенный URI |
| `ip` | String | Да | IP пользователя |
| `timestamp` | String | Да | Время в формате `yyyy-MM-dd HH:mm:ss` |

**Ответы:**
- `201 Created` — успешно сохранено (тело пустое)
- `400 Bad Request` — невалидные данные

---

### `GET /stats` — Получить статистику

Возвращает агрегированные данные о просмотрах.

**Query-параметры:**

| Параметр | Тип | Обязательно | По умолчанию | Описание |
|----------|-----|-------------|--------------|----------|
| `start` | String | Да | — | Начало периода: `yyyy-MM-dd HH:mm:ss` |
| `end` | String | Да | — | Конец периода: `yyyy-MM-dd HH:mm:ss` |
| `uris` | String[] | Нет | null | Список URI для фильтрации |
| `unique` | boolean | Нет | `false` | `true` = считать только уникальные IP |

**Пример запроса:**
```
GET /stats?start=2026-03-01 10:00:00&end=2026-03-01 12:00:00&uris=/events/1&uris=/events/2&unique=false
```

**Ответ `200 OK`:**
```json
[
  {
    "app": "ewm-main-service",
    "uri": "/events/1",
    "hits": 42
  },
  {
    "app": "ewm-main-service",
    "uri": "/events/2",
    "hits": 17
  }
]
```

Сортировка: по `hits DESC`, затем `app ASC`, затем `uri ASC`.

**Ответы:**
- `200 OK` — список статистики (может быть пустым)
- `400 Bad Request` — неверный формат дат, или `start` > `end`

---

## 8. Поток данных (Data Flow)

### Сохранение hit (запись просмотра)

```
Пользователь
    │  HTTP GET /events/1
    ▼
main-service
    │  StatClient.saveHit(EndpointHit)
    │  POST http://stats-server:9090/hit
    ▼
StatsController.saveHit(@Valid EndpointHit)
    │  валидация Bean Validation
    ▼
StatsService.saveHit(EndpointHit)
    │  EndpointHit → HitEntity
    ▼
JdbcHitRepository.save(HitEntity)
    │  INSERT INTO stat(app, uri, ip, created)
    ▼
PostgreSQL (statdb)
```

### Получение статистики

```
main-service
    │  StatClient.getStat(start, end, uris, unique)
    │  GET http://stats-server:9090/stats?start=...&end=...
    ▼
StatsController.getStats(start, end, uris, unique)
    ▼
StatsService.getStats(...)
    │  1. Парсинг строк в LocalDateTime
    │  2. Проверка start <= end
    ▼
JdbcHitRepository.getStats(start, end, uris, unique)
    │  SELECT app, uri, COUNT(*) [или COUNT(DISTINCT ip)]
    │  FROM stat WHERE created BETWEEN ? AND ?
    │  [AND uri IN (?,...)]
    │  GROUP BY app, uri ORDER BY hits DESC
    ▼
PostgreSQL → List<ViewStats> → main-service
```

---

## 9. Конфигурация и запуск

### Запуск через Docker Compose (рекомендуемый способ)

```bash
docker-compose up --build
```

Запускает 4 контейнера:

| Контейнер | Образ | Порт | Описание |
|-----------|-------|------|----------|
| `explore-with-me-main-service` | собирается из `main-service/` | 8080 | Main-сервис |
| `explore-with-me-stat-server` | собирается из `stat/stat-server/` | 9090 | Сервис статистики |
| `postgres-main-service` | `postgres:16.1` | 6541→5432 | БД main-service |
| `postgres-stat-server` | `postgres:16.1` | 6542→5432 | БД статистики |

**Порядок запуска:**
1. Сначала стартуют обе БД с healthcheck (`pg_isready`)
2. `stats-server` ждёт готовности `stat-server-db`
3. `main-service` ждёт готовности и `stats-server`, и `main-service-db`

### Переменные окружения

**stats-server:**
| Переменная | Значение в Docker | Локальное значение по умолчанию |
|------------|-------------------|--------------------------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://stat-server-db:5432/statdb` | `jdbc:postgresql://localhost:6542/statdb` |
| `SPRING_DATASOURCE_USERNAME` | `stat` | `stat` |
| `SPRING_DATASOURCE_PASSWORD` | `stat` | `stat` |

**main-service:**
| Переменная | Значение в Docker |
|------------|-------------------|
| `STATS_SERVER_URL` | `http://stats-server:9090` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://main-service-db:5432/mainservicedb` |
| `SPRING_DATASOURCE_USERNAME` | `mainservice` |
| `SPRING_DATASOURCE_PASSWORD` | `mainservice` |

### Локальный запуск stat-server (без Docker)

1. Поднять PostgreSQL на порту 6542, создать БД `statdb` (пользователь/пароль: `stat`)
2. Запустить:
```bash
cd stat/stat-server
mvn spring-boot:run
```
Сервер стартует на `http://localhost:9090`.

### Локальный запуск main-service (без Docker)

1. Поднять PostgreSQL на порту `6541`, создать БД `mainservicedb` (пользователь/пароль: `mainservice`)
2. Убедиться, что `stat-server` доступен на `http://localhost:9090`
3. Запустить `main-service` с профилем `local`:
```bash
cd main-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Профиль `local` переопределяет datasource на `jdbc:postgresql://localhost:6541/mainservicedb` и не влияет на тестовый профиль.

### Сборка проекта

```bash
# Собрать все модули
mvn clean package

# Собрать без тестов
mvn clean package -DskipTests

# Запустить тесты с покрытием (JaCoCo)
mvn clean verify -Pcoverage

# Запустить проверки стиля и SpotBugs
mvn clean verify -Pcheck
```

---

## 10. Тестирование

### Тестовый профиль

В тестах используется профиль `test` (`@ActiveProfiles("test")`).
`stat-server` и `main-service` используют **H2 in-memory** базу данных вместо PostgreSQL.

Тестовые настройки — [stat/stat-server/src/test/resources/application-test.properties](stat/stat-server/src/test/resources/application-test.properties).
Тестовая схема — [stat/stat-server/src/test/resources/schema.sql](stat/stat-server/src/test/resources/schema.sql).

### `StatsControllerIntegrationTest` — интеграционные тесты контроллера

Поднимает полный Spring-контекст с MockMvc. Перед каждым тестом очищает таблицу `stat`.

| Тест | Что проверяет |
|------|---------------|
| `postHitShouldReturnCreated` | POST /hit возвращает 201 и запись попадает в БД |
| `postHitShouldReturnBadRequestWhenBodyIsInvalid` | Пустой `app` → 400 |
| `getStatsShouldReturnAggregatedValues` | Суммарный COUNT(*) по URI в диапазоне дат |
| `getStatsShouldReturnUniqueValues` | COUNT(DISTINCT ip) с параметром `unique=true` |
| `getStatsShouldReturnBadRequestWhenStartAfterEnd` | start > end → 400 |
| `getStatsShouldReturnBadRequestWhenDateFormatIsInvalid` | ISO-формат даты → 400 |

### `JdbcHitRepositoryIntegrationTest` — интеграционные тесты репозитория

Использует `@JdbcTest` (только JDBC-слой, без web-контекста).

| Тест | Что проверяет |
|------|---------------|
| `saveShouldPersistRecord` | После save запись находится через getStats |
| `getStatsShouldReturnNonUniqueCount` | 2 hit с одним IP → hits=2 |
| `getStatsShouldReturnUniqueCount` | 2 hit с одним IP + 1 с другим → unique hits=2 |
| `getStatsShouldReturnOnlyRequestedUrisWithinRange` | Фильтрация по URI и диапазону дат |

---

## 11. Качество кода

Настроены три инструмента (активируются профилями Maven):

### Checkstyle (профиль `check`)
- Конфигурация: [checkstyle.xml](checkstyle.xml)
- Проверяет стиль кода на этапе `compile`
- Включает тестовые классы
- При нарушениях — `failOnViolation=true`

### SpotBugs (профиль `check`)
- Статический анализ байт-кода на наличие потенциальных багов
- Исключения: [suppressions.xml](suppressions.xml)
- Уровень: `effort=Max`, порог срабатывания: `High`

### JaCoCo (профиль `coverage`)
- Измерение покрытия кода тестами
- Минимальные пороги покрытия:
  - Инструкции: 1%
  - Строки: 20%
  - Ветви: 20%
  - Сложность: 20%
  - Методы: 20%
  - Классы: не более 1 непокрытого класса
- Отчёт генерируется в фазе `install`

---

## 12. Технологический стек

| Компонент | Технология | Версия |
|-----------|------------|--------|
| Язык | Java | 21 |
| Фреймворк | Spring Boot | 3.3.0 |
| Сборка | Maven | — |
| База данных (прод) | PostgreSQL | 16.1 |
| База данных (тесты) | H2 | — |
| Доступ к данным | Spring JDBC (JdbcTemplate) | — |
| HTTP-клиент | RestTemplate (Spring Web) | — |
| Валидация | Jakarta Bean Validation | — |
| Сериализация | Jackson | — |
| Логирование | Logback + SLF4J | — |
| Lombok | Lombok (только в stat-client) | — |
| Контейнеризация | Docker / Docker Compose | — |
| Тестирование | JUnit 5, MockMvc, AssertJ | — |
| Покрытие | JaCoCo | 0.8.12 |
| Стиль | Checkstyle | 10.3 |
| Анализ | SpotBugs | 4.8.5.0 |
