# Production-развёртывание Price Hunter на VPS REG.RU

Этот контур рассчитан на сервер `Std C1-M1-D10` с 1 ГБ RAM и 10 ГБ диска. Он предназначен для первого запуска: одно Spring Boot API, PostgreSQL, один последовательный парсер и Caddy для HTTPS. Сервер не открывает наружу PostgreSQL или порт приложения.

## Что уже оптимизировано

- `Dockerfile.vps` не собирает Java-проект на VPS: собранный JAR доставляется с рабочей машины, поэтому во время развёртывания не расходуются память и место на Maven;
- PostgreSQL ограничен 256 МБ и настроен для маленького числа подключений;
- backend ограничен 480 МБ контейнерной памяти и 320 МБ Java heap;
- Docker-логи ротируются и занимают максимум 10 МБ на сервис;
- `/api/admin/**` требует отдельный bearer-токен;
- Caddy принимает HTTPS и передаёт запросы backend только на loopback-интерфейсе.

## Подготовка домена

В REG.RU для `pricehunter.space` указываются DNS-серверы `ns1.reg.ru` и `ns2.reg.ru`. В зоне добавляется A-запись:

| Поле | Значение |
| --- | --- |
| Subdomain | `api` |
| Type | `A` |
| IP address | публичный IP VPS |

После появления DNS-записи Caddy автоматически получает и обновляет TLS-сертификат для `api.pricehunter.space`.

## Подготовка сервера

Используется Ubuntu 24.04. После получения IP-адреса на сервере устанавливаются Docker Engine и Docker Compose plugin. Для запуска нужен каталог `/opt/price-hunter`, доступный только пользователю, который управляет развёртыванием.

В firewall разрешаются только `22`, `80` и `443`. Порты `5432` и `8080` остаются закрытыми.

## Сборка и доставка

На рабочей машине из корня проекта собирается проверенный JAR:

```bash
mvn package -DskipTests
```

На VPS копируется текущий код из GitHub и файл `target/price-hunter-0.0.1-SNAPSHOT.jar`. Файл JAR не попадает в Git и не содержит секретов.

В `/opt/price-hunter/.env.production` заполняются значения по шаблону `.env.production.example`:

- `POSTGRES_PASSWORD` — отдельный длинный случайный пароль;
- `ADMIN_API_TOKEN` — отдельный случайный токен не короче 32 байт;
- `CORS_ALLOWED_ORIGINS` — домены реального frontend, например `https://pricehunter.space,https://www.pricehunter.space`;
- `PARSER_SCHEDULING_ENABLED=false` до первого ручного импорта.

Backend и PostgreSQL запускаются командой:

```bash
docker compose --env-file .env.production -f compose.production.yaml up -d --build
```

## Проверка после запуска

```bash
curl -i https://api.pricehunter.space/healthz
curl -i 'https://api.pricehunter.space/api/cities?page=0&size=20'
curl -i https://api.pricehunter.space/api/admin/parser-sources
```

Первые два запроса должны дать ответ API; третий без `Authorization` должен вернуть `401`. Для административного запроса используется:

```bash
curl -i \
  -H 'Authorization: Bearer <ADMIN_API_TOKEN>' \
  'https://api.pricehunter.space/api/admin/parser-sources?page=0&size=20'
```

## Эксплуатационные ограничения тарифа

На тарифе 1 ГБ запускаем не более одного процесса парсинга одновременно. Новые источники подключаются вручную и проверяются через очередь `review-candidates`; ежедневное расписание включается только после успешного первого импорта. При устойчивой нагрузке, ошибках нехватки памяти или занятом диске сервер увеличивается до 2 ГБ RAM и 40 ГБ NVMe без изменения домена или API.
