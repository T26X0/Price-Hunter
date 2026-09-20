# Production-развёртывание через Cloudflare Tunnel

Этот проект — Spring Boot-приложение, поэтому Cloudflare используется как защищённый входной слой, а Java и PostgreSQL запускаются на сервере в Docker. Публичный порт PostgreSQL и порт `8080` наружу не открываются.

## До первого запуска

Нужны:

- сервер с Docker Engine и Docker Compose plugin;
- домен в активной Cloudflare Zone;
- Cloudflare Zero Trust;
- точный production-origin frontend Димы, например `https://app.example.com`;
- отдельный почтовый список операторов Admin API.

Не используйте локальную базу разработки как production-базу и не переносите пароли из `application.yaml` в серверное окружение.

## 1. Развернуть приложение на сервере

```bash
git clone https://github.com/T26X0/Price-Hunter.git
cd Price-Hunter
cp .env.production.example .env.production
```

В `.env.production` замените пароль PostgreSQL, укажите точный `CORS_ALLOWED_ORIGINS` и добавьте token туннеля из Cloudflare Dashboard. Затем:

```bash
docker compose --env-file .env.production -f compose.production.yaml up -d --build
docker compose --env-file .env.production -f compose.production.yaml ps
```

Первый старт автоматически применит Flyway-миграции. `postgres` и `app` не имеют published ports: к API может обращаться только контейнер `cloudflared` внутри private Docker-сети.

## 2. Настроить Cloudflare Tunnel

В Cloudflare Dashboard откройте **Networking → Tunnels**, создайте туннель, скопируйте token в `.env.production` и добавьте Published application:

```text
Public hostname: api.<ваш-домен>
Service: http://app:8080
```

Перезапустите только tunnel-контейнер после добавления token:

```bash
docker compose --env-file .env.production -f compose.production.yaml up -d cloudflared
```

Cloudflare сам создаёт DNS-запись для hostname при full DNS setup. Для partial CNAME setup создайте CNAME по инструкции Cloudflare.

## 3. Открыть public API и закрыть Admin API

Public routes, которые нужны frontend, остаются доступны на `https://api.<ваш-домен>/api/...`.

До передачи адреса Диме обязательно создайте Cloudflare Access Self-hosted application:

```text
Hostname: api.<ваш-домен>
Path: /api/admin/*
Policy: Allow только e-mail операторов Price Hunter
```

В Tunnel включите **Protect with Access** для этого published application. Не защищайте весь `api` hostname: это закроет публичный каталог от frontend. Не делайте `/api/admin/*` публичным, пока нет прикладной авторизации и пользовательских ролей.

## 4. Проверить

После публикации:

```bash
curl -i https://api.<ваш-домен>/api/cities
curl -i 'https://api.<ваш-домен>/api/products?q=iPhone%2016%20Pro&page=0&size=20'
```

Обычный запрос к `/api/admin/parser-sources` должен быть остановлен Cloudflare Access, а не возвращать данные backend. После входа оператором через Access Admin API доступен для ручного запуска parser source и очереди review candidates.

## 5. Первый production-импорт 941

Через закрытый Admin API создайте город, сеть 941, рынок и `ParserSource`, затем выполните `PRODUCT_DISCOVERY`. Не включайте планировщик до успешной ручной проверки job. Данные production не добавляются в Git и не должны создаваться через Java hardcode.

## Ограничения Admin-панели

Сейчас backend умеет управлять городами, сетями, рынками, источниками, jobs и review candidates. Модели пользователей, регистрации, ролей и удаления пользователей **в проекте ещё нет**. Их нужно проектировать отдельным этапом после выбора способа входа пользователей; не добавляйте фиктивную кнопку удаления.

## Откат и резервные копии

- перед обновлением сделайте дамп PostgreSQL на сервере;
- не выполняйте `docker compose down -v` в production: это удалит volume базы;
- Flyway-миграции применяются только вперёд; для исправления схемы создавайте новую миграцию.
