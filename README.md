# Blog

Полнофункциональное блог-приложение: Spring Boot 3.4 бэкенд + React фронтенд (Nginx).

## Стек

| Слой | Технологии |
|------|-----------|
| Бэкенд | Java 21, Spring Boot 3.4, Spring MVC, Spring Data JDBC (`JdbcTemplate`) |
| База данных | H2 in-memory, схема инициализируется из `schema.sql` |
| Фронтенд | React SPA (pre-built), раздаётся Nginx |
| Контейнеры | Docker (multi-stage build), Docker Compose |

## Требования

- JDK 21+
- Gradle (или используйте обёртку `./gradlew` из корня проекта)
- Docker + Docker Compose (для запуска полного стека)

## Сборка бэкенда

```bash
./gradlew :my-blog-backend-app:bootJar
```

Артефакт: `my-blog-backend-app/build/libs/blog-0.0.1-SNAPSHOT.jar`.

## Запуск тестов

```bash
./gradlew :my-blog-backend-app:test
```

Отчёт: `my-blog-backend-app/build/reports/tests/test/index.html`.

Проект покрыт тремя уровнями тестов:

| Класс | Аннотация | Что проверяет |
|-------|-----------|---------------|
| `PostServiceTest` | `@ExtendWith(MockitoExtension.class)` | Бизнес-логика сервиса, без Spring-контекста |
| `PostRepositoryTest` | `@JdbcTest` | SQL-запросы и маппинг через реальную H2 |
| `PostControllerIntegrationTest` | `@SpringBootTest + @AutoConfigureMockMvc` | HTTP-слой от запроса до базы |

## Запуск бэкенда локально

```bash
./gradlew :my-blog-backend-app:bootRun
```

Или собранным JAR:

```bash
java -jar my-blog-backend-app/build/libs/blog-0.0.1-SNAPSHOT.jar
```

Сервер стартует на `http://localhost:8080`.

H2-консоль: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:blogdb`).

Директория для изображений по умолчанию: `./uploads/`. Переопределить:

```bash
java -jar my-blog-backend-app/build/libs/blog-0.0.1-SNAPSHOT.jar --app.upload.dir=/path/to/uploads
```

## Запуск полного стека (Docker Compose)

```bash
docker compose up --build
```

- Фронтенд: `http://localhost`
- Бэкенд API: `http://localhost:8080/api`
- Nginx проксирует `/api/` → `http://blog-back-app:8080/api/`

Изображения хранятся в Docker volume `uploads_data`.

## API

| Метод  | Путь                                        | Описание                    |
|--------|---------------------------------------------|-----------------------------|
| GET    | /api/posts?search=&pageNumber=1&pageSize=10 | Список постов с пагинацией  |
| POST   | /api/posts                                  | Создать пост                |
| GET    | /api/posts/{id}                             | Получить пост по ID         |
| PUT    | /api/posts/{id}                             | Обновить пост               |
| DELETE | /api/posts/{id}                             | Удалить пост                |
| POST   | /api/posts/{id}/likes                       | Поставить лайк посту        |
| GET    | /api/posts/{id}/image                       | Получить изображение поста  |
| PUT    | /api/posts/{id}/image                       | Загрузить изображение поста |
| GET    | /api/posts/{id}/comments                    | Список комментариев поста   |
| POST   | /api/posts/{id}/comments                    | Добавить комментарий        |
| PUT    | /api/posts/{postId}/comments/{id}           | Обновить комментарий        |
| DELETE | /api/posts/{postId}/comments/{id}           | Удалить комментарий         |

Ошибки 404 возвращаются в формате [RFC 7807 Problem Details](https://www.rfc-editor.org/rfc/rfc7807):

```json
{ "type": "about:blank", "title": "Post Not Found", "status": 404, "detail": "Post not found: id=99" }
```

### Примеры запросов

Создать пост:
```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{"title":"Заголовок","text":"Текст поста","tags":["java","spring"]}'
```

Получить список постов (фильтр по тегу):
```bash
curl "http://localhost:8080/api/posts?search=java&pageNumber=1&pageSize=5"
```

Поставить лайк:
```bash
curl -X POST http://localhost:8080/api/posts/1/likes
```

Добавить комментарий:
```bash
curl -X POST http://localhost:8080/api/posts/1/comments \
  -H "Content-Type: application/json" \
  -d '{"id":0,"text":"Мой комментарий","postId":1}'
```

## Схема базы данных

```
post          tag           post_tag         comment
──────────    ──────────    ──────────────   ──────────────
id (PK)       id (PK)       post_id (FK) ──► id (PK)
title         name UNIQUE   tag_id  (FK)     text
text                                         post_id (FK)
likes_count
comments_count
```

Теги хранятся нормализованно: таблица `tag` — словарь уникальных тегов, `post_tag` — связь многие-ко-многим.
Поиск по тегу выполняется через JOIN с `LOWER(t.name) = LOWER(?)` — точное совпадение, без LIKE по JSON.

## Структура проекта

```
blog/
  my-blog-backend-app/               # Spring Boot бэкенд
    src/
      main/
        java/com/boot/blog/
          BlogApplication.java        # Точка входа (@SpringBootApplication)
          aop/                        # AOP-аспект: логирование всех @RestController
          configuration/              # CorsConfig (разрешает * origins)
          controller/                 # PostController — все REST-эндпоинты
          dto/                        # NewPostDto, UpdatePostDto, CommentDto
          exception/                  # PostNotFoundException + GlobalExceptionHandler
          model/                      # Post, Page
          repository/                 # PostRepository (интерфейс) + JdbcNativePostRepository
          service/                    # PostService
        resources/
          application.properties      # H2, schema init, upload dir
          schema.sql                  # DDL + seed-данные (MERGE INTO ... KEY)
          static/default.jpeg         # Изображение-заглушка
      test/
        java/com/boot/blog/
          controller/PostControllerIntegrationTest.java  # @SpringBootTest + MockMvc
          repository/PostRepositoryTest.java             # @JdbcTest
          service/PostServiceTest.java                   # Чистый Mockito (без Spring)
        resources/
          application.properties      # Переопределение для тестов (отдельная БД)
    build.gradle
    Dockerfile                        # Multi-stage: eclipse-temurin:21-jdk → jre
  my-blog-front-app/                 # Nginx + pre-built React SPA
    dist/                            # Собранный фронтенд
    nginx.conf                       # SPA-роутинг + прокси /api/ → бэкенд
    Dockerfile
  settings.gradle                    # Gradle multi-project (include 'my-blog-backend-app')
  docker-compose.yaml                # blog-back-app + my-blog-front-app + volume
  README.md
```
