# Blog

Полнофункциональное блог-приложение: Spring Boot 3.4 бэкенд + React фронтенд (Nginx).

## Требования

- JDK 21+
- Gradle (или используйте обёртку `./gradlew` из корня проекта)
- Docker + Docker Compose (для запуска полного стека)

## Сборка бэкенда

Собрать исполняемый JAR-файл:

```bash
./gradlew :my-blog-backend-app:bootJar
```

Артефакт будет в `my-blog-backend-app/build/libs/blog-0.0.1-SNAPSHOT.jar`.

## Запуск тестов

```bash
./gradlew :my-blog-backend-app:test
```

Отчёт: `my-blog-backend-app/build/reports/tests/test/index.html`.

## Запуск бэкенда локально

```bash
./gradlew :my-blog-backend-app:bootRun
```

Или запустить собранный JAR напрямую:

```bash
java -jar my-blog-backend-app/build/libs/blog-0.0.1-SNAPSHOT.jar
```

Сервер стартует на `http://localhost:8080`.

H2-консоль доступна по адресу `http://localhost:8080/h2-console`
(JDBC URL: `jdbc:h2:mem:blogdb`).

Директория для загрузки изображений по умолчанию: `./uploads/` (относительно рабочей директории).
Переопределить можно через свойство:

```bash
java -jar my-blog-backend-app/build/libs/blog-0.0.1-SNAPSHOT.jar --app.upload.dir=/path/to/uploads
```

## Запуск полного стека (Docker Compose)

Поднять бэкенд + фронтенд одной командой из корня проекта:

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
| GET    | /api/posts/{id}/image                       | Получить изображение поста  |
| PUT    | /api/posts/{id}/image                       | Загрузить изображение поста |
| GET    | /api/posts/{id}/comments                    | Список комментариев поста   |
| POST   | /api/posts/{id}/comments                    | Добавить комментарий        |
| PUT    | /api/posts/{postId}/comments/{id}           | Обновить комментарий        |
| DELETE | /api/posts/{postId}/comments/{id}           | Удалить комментарий         |

### Примеры запросов

Создать пост:
```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{"title":"Заголовок","text":"Текст поста","tags":["java","spring"]}'
```

Получить список постов:
```bash
curl "http://localhost:8080/api/posts?search=&pageNumber=1&pageSize=10"
```

Добавить комментарий:
```bash
curl -X POST http://localhost:8080/api/posts/1/comments \
  -H "Content-Type: application/json" \
  -d '{"id":0,"text":"Мой комментарий","postId":1}'
```

## Структура проекта

```
blog/
  my-blog-backend-app/               # Spring Boot бэкенд
    src/
      main/
        java/com/boot/blog/
          BlogApplication.java        # Точка входа Spring Boot
          aop/                        # AOP-аспект логирования контроллеров
          configuration/              # CorsConfig
          controller/                 # REST-контроллер
          dto/                        # NewPostDto, UpdatePostDto, CommentDto
          model/                      # Post, Page
          repository/                 # PostRepository (интерфейс + JDBC-реализация)
          service/                    # PostService
        resources/
          application.properties      # Настройки (H2, schema init, upload dir)
          schema.sql                  # DDL-схема БД (создаётся при старте)
          static/default.jpeg         # Изображение-заглушка для поста
      test/
        java/com/boot/blog/
          BlogApplicationTests.java   # Smoke-тест загрузки контекста
          controller/PostControllerIntegrationTest.java  # @SpringBootTest + MockMvc
          repository/PostRepositoryTest.java             # @JdbcTest
          service/PostServiceTest.java                   # @SpringBootTest + @MockitoBean
        resources/
          application.properties      # Переопределение настроек для тестов
    build.gradle
    settings.gradle
    Dockerfile                        # Multi-stage build бэкенда
  my-blog-front-app/                 # Nginx + pre-built React SPA
    dist/                            # Собранный фронтенд
    nginx.conf                       # Nginx: прокси /api/ → бэкенд
    Dockerfile
  docker-compose.yaml                # Оркестрация обоих сервисов
  README.md
```
