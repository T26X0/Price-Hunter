# Price Hunter

Price Hunter — backend сервиса сравнения цен. Он хранит одну каноническую модель товара, её конфигурации и отдельные предложения сети в каждом городе, включая цену, наличие, условия покупки и историю изменений.

Сейчас готов первый проверенный вертикальный сценарий:

```text
941store.ru → HTML-парсер → нормализация → PostgreSQL → REST API → frontend
```

Первая поддержанная страница — публичный каталог `iPhone 16 Pro` сети 941. Полный crawler сайта, поиск категорий и пагинация — следующий отдельный этап, а не заявленная готовая возможность.

## Требования

- Java 21 или новее (проект компилируется с target Java 21);
- Maven;
- PostgreSQL 15+.

Для production в Cloudflare дополнительно нужны Node.js, Docker Desktop и тариф Workers Paid. Полная инструкция находится в [`DEPLOYMENT_CLOUDFLARE.md`](DEPLOYMENT_CLOUDFLARE.md).

Для контейнерного PostgreSQL можно использовать `docker compose up -d postgres`. На macOS также подойдёт локально установленный PostgreSQL.

По умолчанию приложение подключается к:

```text
jdbc:postgresql://localhost:5432/price_hunter
пользователь: price_hunter
пароль: local_dev_only
```

Это только значения для локальной разработки. Для другой среды передайте `DB_URL`, `DB_USERNAME` и `DB_PASSWORD` через переменные окружения.

## Запуск

```bash
cd '/Users/t26x0/Documents/ChatGPT/Price Hunter'
mvn test
mvn spring-boot:run
```

После запуска:

- API: `http://localhost:8080/api`;
- Swagger UI: `http://localhost:8080/swagger-ui.html`;
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`.

Flyway применяет миграции автоматически при старте. Не меняйте уже применённые миграции `V1–V6`: для изменения схемы добавляется новая миграция.

## Публичный API

Единственное место, где перечислены пути в коде: `src/main/java/com/pricehunter/shared/ApiRoutes.java`. DTO отделены от JPA-сущностей; списки используют постраничный ответ `{ items, page, size, hasNext }`.

| Задача | Метод и путь |
| --- | --- |
| Города | `GET /api/cities` |
| Поиск моделей | `GET /api/products?q=&cityId=&category=&brand=&page=&size=` |
| Карточка модели | `GET /api/products/{catalogKey}?cityId=` |
| Конфигурации модели | `GET /api/products/{catalogKey}/variants?cityId=` |
| Предложения варианта | `GET /api/variants/{variantId}/offers?cityId=&page=&size=` |
| Предложения с доставкой | `GET /api/variants/{variantId}/delivered-offers?cityId=&page=&size=` |
| То же получение предложений | `GET /api/offers?variantId=&cityId=&page=&size=` |
| Карточка предложения | `GET /api/offers/{offerId}` |
| График цены и наличия | `GET /api/offers/{offerId}/history?from=&to=` |
| Наличие по филиалам | `GET /api/offers/{offerId}/inventory?page=&size=` |

Публичный идентификатор модели — только `catalogKey`, например `apple-iphone-16-pro`; UUID товара не подменяет его в URL.

## Административный API

Admin API нужен для настройки источников и ручных запусков. В локальном MVP авторизация ещё не добавлена — не публикуйте эти маршруты в интернет без защиты доступа.

| Задача | Метод и путь |
| --- | --- |
| Создать город | `POST /api/admin/cities` |
| Создать сеть | `POST /api/admin/chains` |
| Создать рынок сети в городе | `POST /api/admin/markets` |
| Создать и просмотреть источник | `POST`, `GET /api/admin/parser-sources` |
| Включить или выключить источник | `PATCH /api/admin/parser-sources/{sourceId}/enabled` |
| Запустить сохранённый источник | `POST /api/admin/parser-sources/{sourceId}/runs` |
| Прочитать job | `GET /api/admin/parser-jobs/{jobId}` |
| История job источника | `GET /api/admin/parser-sources/{sourceId}/jobs` |
| Плановый проход просроченных источников | `POST /api/admin/parser-scheduler/run` |
| Очередь ручной проверки | `GET /api/admin/review-candidates?status=PENDING` |
| Карточка кандидата | `GET /api/admin/review-candidates/{candidateId}` |
| Применить решение | `POST /api/admin/review-candidates/{candidateId}/resolve` |

Ручной запуск не принимает URL: адрес берётся только из заранее сохранённого `ParserSource`. Это не позволяет подменить домен через HTTP-запрос.

В production маршруты `/api/admin/**` закрывает Cloudflare Worker и требует `Authorization: Bearer ...`; публичные каталожные маршруты остаются открытыми для frontend.

Пример запуска:

```json
POST /api/admin/parser-sources/{sourceId}/runs
{
  "type": "PRICE_REFRESH"
}
```

Допустимы `PRICE_REFRESH` и `PRODUCT_DISCOVERY`. Ответ содержит итоговый job: статус, количество найденных позиций, созданных/изменённых записей, кандидатов проверки и ошибок.

Очередь ручной проверки не выдаёт фиктивных «одобрений». Сейчас полностью реализованы:

- `REJECTED` — отклонение ошибочного кандидата;
- `MERGED` для `STORE_MATCH` с `targetEntityId` существующего филиала той же сети. Решение создаёт `source_entity_link`, поэтому следующий импорт действительно использует выбранный филиал.

`APPROVED` для товара или категории пока возвращает понятную ошибку: до него нужно добавить логику создания либо классификации канонической сущности.

## Первый запуск 941

1. Через Admin API создайте город, сеть, рынок и `ParserSource`.
2. Для 941 используйте:

```json
{
  "sourceType": "WEBSITE",
  "connectorMode": "HTML",
  "connectorKey": "941",
  "baseUrl": "https://941store.ru/catalog/iphone-16-pro",
  "configuration": { "cityKey": "ekaterinburg" },
  "enabled": true
}
```

3. Вызовите `POST /api/admin/parser-sources/{sourceId}/runs`.
4. Найдите товар через `GET /api/products?q=iPhone%2016%20Pro&cityId={cityId}` и пройдите к варианту, предложению и истории.

HTTP-клиент парсера принимает только HTTPS, сверяет `robots.txt`, соблюдает crawl delay, не следует редиректам и ограничивает размер ответа. CAPTCHA не обходится. Защита от localhost есть уже сейчас; полная DNS/IP-защита private и link-local сетей нужна перед production.

## Модель данных

- `product_models` — одна модель, например iPhone 16 Pro;
- `product_variants` и `product_variant_attributes` — память, цвет, SIM и другие характеристики;
- `retail_chains`, `chain_city_markets`, `store_locations` — сеть, её рынок в городе и физические филиалы;
- `offers` — актуальное предложение конкретной сети/города/варианта;
- `offer_terms` — скидка, подарок, рассрочка, кредит и прочие условия;
- `store_inventory` — наличие по филиалам;
- `offer_state_history` — интервальная история, которая добавляется только при изменении состояния;
- `offer_monthly_stats` — агрегированная история после retention;
- `parser_sources`, `parser_jobs`, `collection_runs`, `review_candidates`, `source_entity_links` — управляемый контур сбора и проверки.

Повторный одинаковый импорт не дублирует модель, вариант, предложение или точку графика. Детальная история хранится шесть месяцев; старые закрытые интервалы могут быть сведены в месячную статистику сервисом retention.

## Проверки

Быстрые unit-тесты и тесты без PostgreSQL:

```bash
mvn test
```

Полная проверка с Flyway и PostgreSQL должна всегда выполняться в отдельной базе, а не в базе с реальными импортами:

```bash
RUN_DB_INTEGRATION_TESTS=true \
DB_URL='jdbc:postgresql://localhost:5432/price_hunter_integration' \
DB_USERNAME='price_hunter' \
DB_PASSWORD='local_dev_only' \
mvn test
```

Живой smoke-test конкретной страницы 941 сделан opt-in:

```bash
RUN_PARSER_LIVE_TESTS=true \
mvn -Dtest=NineFortyOneCatalogConnectorLiveTest test
```

## Следующий этап

После первого vertical slice следует вынести обход сайта в `NineFortyOneSiteDiscovery`: он будет находить разрешённые категории и страницы пагинации, а существующий `NineFortyOneCatalogConnector` останется ответственным только за разбор одной страницы каталога.
