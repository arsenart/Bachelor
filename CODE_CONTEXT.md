# CODE_CONTEXT — Medicton Approval System
> Реальное состояние кода. Обновлять при изменениях.

## Стек
- Spring Boot 4.0.3 / Java 17
- PostgreSQL 16 (Railway), Hibernate 7.2, ddl-auto: update
- Thymeleaf + thymeleaf-extras-springsecurity6
- Bootstrap 5.3 (CDN)
- Spring Security 6, BCrypt
- Spring Boot Starter Mail (Mailtrap sandbox)
- Bean Validation (jakarta.validation)

---

## Структура пакетов

```
bachelor.code
├── api/                  REST контроллеры (AdminUserController, AuthController)
├── config/               AppConfig, SecurityConfig, WebMvcConfig,
│                         CurrentUriInterceptor, DataInitializer
├── dto/                  DTO объекты
├── entity/               JPA сущности
├── enums/                Перечисления
├── exception/            Исключения + GlobalExceptionHandler
├── repository/           Spring Data JPA репозитории
├── security/             CustomUserDetailsService
├── service/              Интерфейсы сервисов
│   └── impl/             Реализации сервисов
└── web/                  MVC контроллеры (Thymeleaf)
```

---

## Сущности (Entity)

### ApprovalRequest — таблица `approval_requests`
| Поле | Тип | Описание |
|---|---|---|
| id | Long | PK |
| title | String | Название заявки |
| type | RequestType | EXPENSE / PURCHASE |
| amount | BigDecimal | Сумма |
| currency | String | Валюта (default CZK) |
| supplier | String | Поставщик |
| description | TEXT | Описание |
| justification | TEXT | Обоснование |
| department | String | Отдел |
| requestedBy | User | Кто создал (ManyToOne) |
| status | RequestStatus | Текущий статус |
| requestedDate | LocalDate | Желаемая дата |
| documentLink | String | Ссылка на документ |
| steps | List<ApprovalStep> | Шаги (OneToMany, cascade ALL) |
| createdAt | LocalDateTime | @PrePersist |
| updatedAt | LocalDateTime | @PreUpdate |

Методы:
- `getActiveStep()` — первый шаг со статусом PENDING (min stepOrder)
- `isEditable()` — NEW или RETURNED_FOR_REVISION
- `isSubmittable()` — NEW или RETURNED_FOR_REVISION

### ApprovalStep — таблица `approval_steps`
| Поле | Тип | Описание |
|---|---|---|
| id | Long | PK |
| request | ApprovalRequest | ManyToOne |
| approver | User | ManyToOne |
| stepOrder | int | Порядок шага (1, 2, 3...) |
| status | StepStatus | PENDING / APPROVED / REJECTED / RETURNED_FOR_REVISION |
| comment | TEXT | Комментарий апрувера |
| decidedAt | LocalDateTime | Время решения |
| createdAt | LocalDateTime | Время создания |

### ApprovalRule — таблица `approval_rules`
| Поле | Тип | Описание |
|---|---|---|
| requestType | RequestType | EXPENSE / PURCHASE |
| minAmount | BigDecimal | Минимальная сумма (null = без ограничения) |
| maxAmount | BigDecimal | Максимальная сумма (null = без ограничения) |
| approver | User | Конкретный пользователь (приоритет над ролью) |
| approverRole | RoleType | Роль (если approver = null) |
| stepOrder | int | Номер шага в маршруте |
| active | boolean | Активно ли правило |
| description | String | Описание правила |

### User — таблица `users`
Реализует `UserDetails`. Поля: email, passwordHash, firstName, lastName, department, roles (Set<RoleType>, @ElementCollection), active, passwordSet.

### AccountingDocument — таблица `accounting_documents`
Поля: type (DocumentType), supplierName, amountWithoutVat, currency, description, status (DocumentStatus), submittedBy (User), createdAt.

### AuditLog — таблица `audit_logs`
Поля: entityType, entityId, action, changedBy (User), oldValue, newValue, comment, createdAt.

### PasswordSetupToken — таблица `password_setup_tokens`
Поля: token (UUID), user (User), expiresAt, used.

---

## Перечисления (Enums)

```java
RequestStatus:   NEW, PENDING_APPROVAL, APPROVED, REJECTED, 
                 RETURNED_FOR_REVISION, REALIZED, CLOSED

RequestType:     EXPENSE, PURCHASE

StepStatus:      PENDING, APPROVED, REJECTED, RETURNED_FOR_REVISION

DocumentStatus:  NEW, SUBMITTED_TO_ACCOUNTING, RETURNED_FOR_COMPLETION, 
                 POSTED, PAID, CLOSED

DocumentType:    ADVANCE_INVOICE, INVOICE, CASH_RECEIPT, CASH_REIMBURSEMENT, 
                 OTHER_LIABILITY, OTHER

RoleType:        ADMIN, APPROVER, REQUESTER, ACCOUNTANT

ApprovalDecisionType: APPROVE, REJECT, RETURN_FOR_REVISION
```

---

## Правила согласования (ApprovalRule) — данные DataInitializer

| Тип | Min | Max | stepOrder | Апрувер |
|---|---|---|---|---|
| EXPENSE | 0 | 3 000 | 1 | роль APPROVER |
| EXPENSE | 3 001 | 50 000 | 1 | Dobiáš (конкретный) |
| EXPENSE | 50 001 | — | 1 | Dobiáš |
| EXPENSE | 50 001 | — | 2 | Fabián |
| PURCHASE | 0 | — | 1 | Matera |
| PURCHASE | 0 | — | 2 | Fabián |

---

## Пользователи (DataInitializer)

### Администраторы / Vedení
| Email | Имя | Роли |
|---|---|---|
| admin@company.com | Admin System | ADMIN, APPROVER, ACCOUNTANT |
| dobias@medicton.com | Martin Dobiáš | ADMIN, APPROVER, REQUESTER |
| fabian@medicton.com | Vratislav Fabián | ADMIN, APPROVER, REQUESTER |

### Менеджеры (APPROVER + REQUESTER)
| Email | Имя | Отдел |
|---|---|---|
| valentova@medicton.com | Tereza Valentová | management (dispečink) |
| korba@medicton.com | Matyáš Korba | management (servis) |
| vlcek@medicton.com | Tomáš Vlček | management (jakost) |
| matera@medicton.com | Lukáš Matera | management (nákup) |

### Бухгалтерия (ACCOUNTANT + REQUESTER)
| Email | Имя |
|---|---|
| rohova@medicton.com | Ivana Říhová |
| klimova@medicton.com | Ivana Klímová |

### Сотрудники (REQUESTER) — dispečink, servis, obchod, Back office, IT
vesela, cerna, klodnerova, scheibova, buresova, pyskaty, sida, masin, botos,
pyskaty.p, kalasova, jurencak, janis, slezak, miziova, chromcova, jagerova,
tajbl, bartovic, dvorak, furisova, navratil, erlebach — все @medicton.com

**Важно:** Только admin@company.com имеет предустановленный пароль (из переменной `ADMIN_PASSWORD`). Остальные получают ссылку для установки пароля через email.

---

## Сервисы

### ApprovalRequestService / Impl
- `createDraft(dto, requester)` → статус NEW, аудит CREATED
- `updateDraft(id, dto, requester)` → только NEW/RETURNED, только свои
- `submit(id, requester)` → очищает старые шаги, строит маршрут через RoutingService, статус PENDING_APPROVAL, email первому апруверу
- `getById(id)`, `getByIdWithDetails(id)`, `getByRequester(user)`, `getAll()`
- `markAsRealized(id, user)` → APPROVED → REALIZED
- `markAsClosed(id, user)` → REALIZED → CLOSED

### ApprovalRoutingService / Impl
- `buildStepsForRequest(request)` → из БД берёт ApprovalRule по типу + диапазон суммы → resolveApprover → создаёт ApprovalStep
- resolveApprover: если rule.approver != null → берёт его; иначе findFirstByRolesContainingAndActiveTrue(role)
- Защита: жадатель != апрувер (иначе BusinessRuleViolationException)

### ApprovalWorkflowService / Impl
- `approve(id, approver, comment)` → валидирует (PENDING_APPROVAL + правильный апрувер) → шаг APPROVED → если есть следующий шаг — email ему; если все одобрены → заявка APPROVED, email жадателю
- `reject(id, approver, comment)` → комментарий обязателен → шаг REJECTED → заявка REJECTED немедленно → email жадателю
- `returnForRevision(id, approver, comment)` → комментарий обязателен → шаг RETURNED_FOR_REVISION → заявка RETURNED_FOR_REVISION → email жадателю
- `getPendingForApprover(user)` → через StepRepository (PENDING шаги)
- `loadAndValidate(id, approver)` → приватный; проверяет статус заявки + что этот апрувер является активным шагом

### AccountingDocumentService / Impl
- `create(dto, user)` → статус NEW
- `submit(id, user)` → NEW → SUBMITTED_TO_ACCOUNTING
- `post(id, user)` → SUBMITTED_TO_ACCOUNTING → POSTED
- `markPaid(id, user)` → POSTED → PAID
- `returnForCompletion(id, user)` → комментарий → RETURNED_FOR_COMPLETION
- `close(id, user)` → PAID → CLOSED

### AuditLogService / Impl
- `log(entityType, entityId, action, changedBy, oldValue, newValue, comment)`
- `getAll()`, `getForEntity(entityType, entityId)`

### EmailService / Impl
- `sendApprovalNeededEmail(to, requesterName, title, amount, currency)`
- `sendRequestDecisionEmail(to, title, decision, comment)`
- `sendPasswordSetupEmail(to, setupLink)`

### UserService / Impl
- `createUser(dto)` → создаёт User + PasswordSetupToken + sendPasswordSetupEmail
- `findAll()`, `findById(id)`, `findByEmail(email)`
- `updateUser(id, dto)`, `toggleActive(id)`, `resendSetupLink(id)`

### PasswordSetupService / Impl
- `validateToken(token)` → проверяет существование, срок действия, использован ли
- `setupPassword(token, newPassword)` → хеширует BCrypt, помечает токен использованным

---

## Web контроллеры (Thymeleaf MVC)

| Контроллер | URL | Назначение |
|---|---|---|
| WebDashboardController | /dashboard | Статистика по ролям |
| WebRequestController | /requests | CRUD заявок + submit |
| WebApprovalController | /approvals | Список и решения апрувера |
| WebAccountingController | /accounting | Бухгалтерские доклады |
| WebAdminController | /admin | Пользователи + все заявки |
| WebAdminRulesController | /admin/rules | Управление правилами |
| WebAuditController | /audit | Аудитный лог |
| WebAuthController | /setup-password | Установка пароля |

Фильтрация на list-страницах: in-memory через Stream (status, type, search параметры).

### Атрибут currentUri
`CurrentUriInterceptor` добавляет `currentUri` в каждую модель → используется в navbar для подсветки активного пункта.

---

## Шаблоны (Thymeleaf)

```
templates/
├── login.html
├── setup-password.html
├── dashboard.html
├── fragments/navbar.html
├── requests/list.html, new.html, detail.html
├── approvals/list.html, detail.html
├── accounting/list.html, new.html, detail.html
├── admin/users.html, edit-user.html, requests.html, rules.html, audit.html
└── error/403.html, 404.html, 500.html
```

i18n динамические ключи: `#{status.request.__${req.status.name().toLowerCase()}__}`

Языки: cs (default), en — переключение через `?lang=cs` / `?lang=en`

---

## Безопасность (SecurityConfig)

- CSRF активен для всех форм; исключение: `/api/**`
- Thymeleaf автоматически вставляет CSRF токен в POST формы
- Публичные URL: `/login`, `/setup-password`, `/api/auth/**`
- `/admin/**` → ADMIN
- `/approvals/**` → APPROVER, ADMIN
- `/audit` → ADMIN
- Остальное → authenticated
- BCrypt strength 10
- После логина → /dashboard

---

## Конфигурация (application.yml)

Все чувствительные значения читаются из переменных окружения с дефолтами:

```yaml
DB_URL           → jdbc:postgresql://localhost:5432/bachelor
DB_USERNAME      → arsenart
DB_PASSWORD      → ""
MAIL_HOST        → sandbox.smtp.mailtrap.io
MAIL_PORT        → 2525
MAIL_USERNAME    → [mailtrap credentials]
MAIL_PASSWORD    → [mailtrap credentials]
APP_BASE_URL     → http://localhost:8080
ADMIN_EMAIL      → admin@company.com
ADMIN_PASSWORD   → admin123
```

---

## Деплой (Railway)

- PostgreSQL: shortline.proxy.rlwy.net:34693/railway
- Dockerfile: многоэтапная сборка (Maven → JRE alpine)
- Root Directory в Railway: `Code/`
- Переменные в Railway: SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD, ADMIN_PASSWORD

---

## Что НЕ реализовано (потенциальные улучшения)

- Тесты (JUnit 5 / Mockito) — не написаны
- Пагинация на list страницах (пока in-memory фильтрация)
- REST API полностью не задокументирован (нет Swagger)
- Email уведомления для бухгалтерских доkladů
- Управление ApprovalRule через UI (WebAdminRulesController есть, шаблон rules.html есть)
- Загрузка файлов (documentLink — только текстовая ссылка)
