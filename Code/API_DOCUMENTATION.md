# API Documentation & Project Status

## Структура проекта

### API Package
- **Путь**: `/src/main/java/bachelor/code/api/`
- **Контроллеры**:
  - `AuthController` — эндпоинты аутентификации и управления токенами
  - `AdminUserController` — эндпоинты администратора для управления пользователями

---

## Что уже реализовано

### 1. Entity слой (Модель данных)
- **User** — основная сущность пользователя
  - Поля: id, email, firstName, lastName, department, passwordHash, active, passwordSet
  - Роли хранятся как `Set<RoleType>` через `@ElementCollection`
  - RoleType enum: REQUESTER, APPROVER, ACCOUNTANT, ADMIN

- **PasswordSetupToken** — одноразовые токены для установки пароля
  - Поля: id, token, user (ManyToOne), createdAt, expiresAt, used, usedAt
  - Срок действия: 24 часа
  - Токен автоматически помечается как использованный после установки пароля

### 2. Repository слой
- `UserRepository` (Spring Data JPA)
  - `findByEmail(String email)` — поиск пользователя по email
  
- `PasswordSetupTokenRepository` (Spring Data JPA)
  - `findByToken(String token)` — поиск токена по строке
  - `findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(Long userId)` — получение активного токена пользователя

### 3. Service слой
- **UserService**
  - `createUser(CreateUserRequest)` — создание нового пользователя админом
    - Автоматически генерирует токен для установки пароля
    - Отправляет письмо с ссылкой активации
  - `resendSetupLink(Long userId)` — повторная отправка ссылки
  - `authenticate(String email, String password)` — аутентификация пользователя

- **PasswordSetupService**
  - `generateTokenForUser(User)` — генерирует новый UUID токен, инвалидирует старые
  - `validateToken(String)` — проверяет валидность токена (не использован, не истёк)
  - `setupPassword(SetupPasswordRequest)` — установка пароля пользователем
    - BCrypt хеширование пароля
    - Помечает токен как использованный

- **EmailService**
  - `sendPasswordSetupEmail(String to, String token)` — отправка письма со ссылкой активации
  - Ссылка: `{app.base-url}/setup-password?token=...`

### 4. DTO слой
- `CreateUserRequest` — запрос на создание пользователя (админ)
- `SetupPasswordRequest` — запрос на установку пароля
- `SetupPasswordTokenValidationResponse` — ответ проверки токена
- `LoginRequest` — запрос на логин
- `LoginResponse` — ответ логина

### 5. Security слой
- `SecurityConfig` — конфигурация Spring Security
  - `/api/auth/**` и `/actuator/**` — публичные (permitAll)
  - `/api/admin/**` — требует роль ADMIN
  - Остальные — требуют аутентификации (authenticated)
  - HTTP Basic Auth включён

- `AppConfig` — бины приложения
  - `PasswordEncoder` — BCryptPasswordEncoder для хеширования паролей

### 6. Exception handling
- `ResourceNotFoundException` — ресурс не найден
- `InvalidTokenException` — токен невалидный
- `TokenExpiredException` — токен истёк
- `BusinessRuleViolationException` — нарушение бизнес-логики
- `GlobalExceptionHandler` — обработчик всех исключений

---

## REST API Endpoints

### Authentication (`/api/auth`)

#### 1. Проверка токена
```
GET /api/auth/setup-password/validate?token=<TOKEN>
```
**Ответ:**
```json
{
  "valid": true,
  "reason": null
}
```
или
```json
{
  "valid": false,
  "reason": "EXPIRED"  // или "USED" или "NOT_FOUND"
}
```

#### 2. Установка пароля
```
POST /api/auth/setup-password
Content-Type: application/json

{
  "token": "uuid-token-here",
  "newPassword": "StrongPassword123!"
}
```
**Ответ:** 200 OK (пусто) или ошибка

#### 3. Логин
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "employee@company.com",
  "password": "StrongPassword123!"
}
```
**Ответ:**
```json
{
  "success": true,
  "message": "Login successful"
}
```
или
```json
{
  "success": false,
  "message": "Invalid credentials or account not ready"
}
```

### Admin Users (`/api/admin/users`)
*Требует Basic Auth с ролью ADMIN*

#### 1. Создание пользователя
```
POST /api/admin/users
Authorization: Basic admin:adminpass
Content-Type: application/json

{
  "email": "employee@company.com",
  "firstName": "Ivan",
  "lastName": "Ivanov",
  "department": "Finance",
  "roles": ["REQUESTER"]
}
```
**Ответ:** 200 OK с объектом User

#### 2. Повторная отправка ссылки
```
POST /api/admin/users/{id}/resend-setup-link
Authorization: Basic admin:adminpass
```
**Ответ:** 200 OK (пусто)

---

## Как запустить приложение

### 1. Собрать проект
```bash
./mvnw clean package -DskipTests
```

### 2. Запустить приложение
```bash
./mvnw spring-boot:run
```
или
```bash
java -jar target/Code-0.0.1-SNAPSHOT.jar
```

Приложение запустится на **http://localhost:8080**

### 3. Примеры curl команд

**Создание пользователя (требует admin:adminpass в dev)**
```bash
curl -u admin:adminpass -X POST http://localhost:8080/api/admin/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "employee@company.com",
    "firstName": "Ivan",
    "lastName": "Ivanov",
    "department": "Finance",
    "roles": ["REQUESTER"]
  }'
```

**Проверка токена**
```bash
curl http://localhost:8080/api/auth/setup-password/validate?token=<TOKEN>
```

**Установка пароля**
```bash
curl -X POST http://localhost:8080/api/auth/setup-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "<TOKEN>",
    "newPassword": "StrongPassword123!"
  }'
```

**Логин**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "employee@company.com",
    "password": "StrongPassword123!"
  }'
```

---

## Конфигурация

### src/main/resources/application.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/myappdb
    username: myappuser
    password: myapppassword
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  mail:
    host: smtp.mailtrap.io
    port: 2525
    username: your-username
    password: your-password
    properties:
      mail:
        smtp:
          auth: true
          starttls.enable: true

app:
  base-url: http://localhost:8080
```

### src/test/resources/application.yml
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  mail:
    host: localhost
    port: 1025
```

---

## Что происходит при запуске приложения

1. **Spring Boot инициализирует контекст**
   - Загружает конфиг из `application.yml`
   - Создаёт DataSource (подключается к PostgreSQL или H2 для тестов)
   - Инициализирует Hibernate/JPA
   - Сканирует пакеты и регистрирует бины (repositories, services, controllers)

2. **Spring Security конфигурируется**
   - Защищает `/api/admin/**` (требует ADMIN)
   - Разрешает публично `/api/auth/**`
   - Включает HTTP Basic Auth

3. **Приложение запускается на порте 8080**
   - Доступны все эндпоинты

4. **База данных инициализируется**
   - Создаются таблицы для User, PasswordSetupToken, user_roles
   - Благодаря `hibernate.ddl-auto=update` таблицы создаются автоматически

---

## Дальнейшее развитие

- [ ] JWT вместо Basic Auth
- [ ] Полноценный UserDetailsService с загрузкой из БД
- [ ] Эмейл отправка с реальным SMTP или логирование
- [ ] Unit/Integration тесты
- [ ] Frontend (React/Vue) для UI активации пароля
- [ ] Refresh tokens и истечение сессии
- [ ] Роль-based access control (RBAC) для остальных эндпоинтов
