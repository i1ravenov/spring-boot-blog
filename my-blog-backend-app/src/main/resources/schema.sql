CREATE TABLE IF NOT EXISTS post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    text TEXT NOT NULL,
    tags TEXT,
    likes_count INT DEFAULT 0,
    comments_count INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    text TEXT NOT NULL,
    post_id BIGINT NOT NULL,
    CONSTRAINT fk_comment_post
        FOREIGN KEY (post_id)
        REFERENCES post(id)
        ON DELETE CASCADE
);

MERGE INTO post (id, title, text, tags, likes_count, comments_count) KEY(id) VALUES
(1,
 'Введение в Spring Boot',
 'Spring Boot — это фреймворк, который упрощает создание Spring-приложений. Он предоставляет автоконфигурацию, встроенный сервер и набор стартеров, которые берут на себя большую часть инфраструктурного кода. В этой статье разберём, как создать своё первое приложение с нуля.',
 '["java","spring","spring-boot"]',
 15, 3),
(2,
 'Spring Data JDBC vs JPA',
 'Spring Data JDBC — более простая альтернатива JPA без ленивой загрузки и кэширования первого уровня. Она отлично подходит, когда вы хотите полного контроля над SQL-запросами и предсказуемого поведения без "магии" Hibernate. Сравниваем подходы на практических примерах.',
 '["java","spring","jdbc","jpa"]',
 8, 2),
(3,
 'REST API с Spring MVC',
 'Spring MVC предоставляет мощный инструментарий для построения REST API: аннотации @RestController, @RequestMapping, @PathVariable, @RequestBody и многие другие. В статье рассмотрим построение полноценного CRUD API с обработкой ошибок и валидацией данных.',
 '["java","spring","rest","api"]',
 22, 4),
(4,
 'Тестирование с JUnit 5 и Spring Boot Test',
 'Spring Boot Test предоставляет аннотации @SpringBootTest, @WebMvcTest и @JdbcTest для удобного написания тестов на разных уровнях приложения. JUnit 5 добавляет параметризованные тесты, вложенные классы и гибкие расширения. Разберём все ключевые подходы.',
 '["java","testing","junit","spring-boot"]',
 12, 2),
(5,
 'Docker для Java-разработчиков',
 'Контейнеризация Java-приложений с Docker стала стандартом в современной разработке. Разберём multi-stage сборку для минимизации размера образа, настройку JVM-флагов для работы в контейнере и оркестрацию через Docker Compose.',
 '["docker","java","devops"]',
 30, 3);

MERGE INTO comment (id, text, post_id) KEY(id) VALUES
-- Комментарии к посту 1 (Введение в Spring Boot)
(1,  'Отличное введение! Особенно понравилась часть про автоконфигурацию.', 1),
(2,  'А как настроить профили для разных окружений?', 1),
(3,  'Спасибо, наконец-то разобрался с разницей между @Bean и @Component.', 1),
-- Комментарии к посту 2 (Spring Data JDBC vs JPA)
(4,  'Давно искал такое сравнение. Spring Data JDBC действительно проще для понимания.', 2),
(5,  'Не хватает примера с маппингом один-ко-многим в JDBC.', 2),
-- Комментарии к посту 3 (REST API с Spring MVC)
(6,  'Очень полезно! Добавьте, пожалуйста, раздел про @ExceptionHandler.', 3),
(7,  'А что насчёт версионирования API?', 3),
(8,  'Хорошая статья, но хотелось бы увидеть примеры с pagination.', 3),
(9,  'Используете ли вы OpenAPI/Swagger для документации?', 3),
-- Комментарии к посту 4 (Тестирование)
(10, 'Наконец-то понял разницу между @MockBean и @Mock!', 4),
(11, 'Было бы здорово добавить примеры с Testcontainers.', 4),
-- Комментарии к посту 5 (Docker)
(12, 'Multi-stage build сократил размер образа с 800MB до 180MB — спасибо!', 5),
(13, 'Как правильно задать -Xmx для контейнера с ограниченной памятью?', 5),
(14, 'Добавьте, пожалуйста, пример с healthcheck в docker-compose.', 5);

ALTER TABLE post    ALTER COLUMN id RESTART WITH 6;
ALTER TABLE comment ALTER COLUMN id RESTART WITH 15;
