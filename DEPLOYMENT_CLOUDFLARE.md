# Production-развёртывание Price Hunter в Cloudflare

Основной production-контур использует Cloudflare Worker + Cloudflare Container и внешнюю PostgreSQL (на первом этапе — Neon). Публичный адрес API: `https://api.pricehunter.space`.

## Как устроен production

- Worker принимает весь внешний HTTP-трафик;
- `/api/admin/**` проверяется на границе Cloudflare по bearer-токену;
- один Spring Boot-контейнер запускается по запросу и засыпает после 10 минут бездействия;
- Flyway применяет миграции при старте контейнера;
- данные хранятся в PostgreSQL, а не на временном диске контейнера;
- Cloudflare Cron каждый день в `02:15 UTC` будит backend и запускает один проход по просроченным источникам;
- контейнер размещается в европейских регионах и имеет тип `basic` (1 GiB RAM).

Конфигурация Worker и контейнера находится в `wrangler.jsonc`, входной код — в `cloudflare/src/index.ts`.

## Обязательные секреты Cloudflare

Секреты нельзя добавлять в Git, `.env` или `wrangler.jsonc`. После создания PostgreSQL сохраните их командами:

```bash
npx wrangler secret put DB_URL
npx wrangler secret put DB_USERNAME
npx wrangler secret put DB_PASSWORD
npx wrangler secret put ADMIN_API_TOKEN
```

Формат `DB_URL` для Neon:

```text
jdbc:postgresql://<host>/<database>?sslmode=require
```

Пароль администратора должен быть случайным значением не короче 32 байт. Frontend передаёт его только для административных запросов:

```text
Authorization: Bearer <ADMIN_API_TOKEN>
```

## Проверка перед публикацией

```bash
mvn test
npm install
npx tsc --noEmit
npx wrangler deploy --dry-run
```

Для сборки Docker-образа локально должен быть запущен Docker Desktop. Предварительную проверку Worker без сборки контейнера можно выполнить так:

```bash
npx wrangler deploy --dry-run --containers-rollout=none
```

## Публикация

Cloudflare Containers требуют Workers Paid. После подключения тарифа и сохранения секретов:

```bash
npm run cf:deploy
```

Wrangler создаст Worker, Durable Object, соберёт Dockerfile, загрузит образ, настроит ежедневный Cron и привяжет custom domain `api.pricehunter.space`.

## Проверка production

```bash
curl -i https://api.pricehunter.space/healthz
curl -i 'https://api.pricehunter.space/api/cities?page=0&size=20'
curl -i 'https://api.pricehunter.space/api/products?q=iPhone%2016%20Pro&page=0&size=20'
curl -i https://api.pricehunter.space/api/admin/parser-sources
```

Первые три запроса должны вернуть ответ backend. Последний запрос без токена должен вернуть `401 Unauthorized` на уровне Worker.

Проверка закрытого API:

```bash
curl -i \
  -H 'Authorization: Bearer <ADMIN_API_TOKEN>' \
  'https://api.pricehunter.space/api/admin/parser-sources?page=0&size=20'
```

## Первый production-импорт 941

Через закрытый Admin API создайте город, сеть 941, рынок и `ParserSource`, затем выполните `PRODUCT_DISCOVERY`. Первый запуск проверяется вручную по job и очереди `review-candidates`; только после этого ежедневный Cron считается допущенным к штатной работе.

## Ограничения текущей админ-панели

Backend уже управляет городами, сетями, рынками, источниками, заданиями парсеров и очередью ручной проверки. Модели пользователей, регистрации, ролей и удаления пользователей в проекте пока нет — их нужно проектировать после выбора способа входа пользователей.

## Альтернативный вариант

`compose.production.yaml` оставлен как запасной путь для обычного VPS с Docker, PostgreSQL и Cloudflare Tunnel. Не запускайте одновременно VPS и Cloudflare Container против одной базы с включённым внутренним Spring Scheduler.
