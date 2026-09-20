# Задание для Codex: подключить frontend Price Hunter к реальному backend

## Роль и результат

Ты работаешь в отдельном репозитории frontend `price-hunter-front`. Твоя задача — заменить устаревшие mock-данные реальной интеграцией с backend Price Hunter и довести до рабочего пользовательского сценария:

```text
выбор города → поиск модели → карточка iPhone → выбор конфигурации
→ предложения 941 → условия покупки → история цены
```

Не переписывай приложение вслепую. Сначала изучи структуру frontend, текущие роуты, компоненты, state management, API-клиент, mock-данные, стили и тесты. Сохраняй удачные части текущего интерфейса; меняй только то, что необходимо для реального контракта.

Не делай commit, push, публикацию или изменения backend без отдельной команды пользователя.

## Проверенный backend

Backend запускается локально по адресу `http://localhost:8080`.

- Swagger: `http://localhost:8080/swagger-ui.html`;
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`;
- базовый API-префикс: `/api`.

Не придумывай API, поля или фильтры. Swagger и этот документ описывают источник правды. Если код frontend расходится с контрактом, адаптируй frontend, а не создавай временные «совместимые» пути на backend.

## Настройка окружения

Добавь или используй переменную окружения:

```text
VITE_API_BASE_URL=http://localhost:8080
```

Если текущий dev-server уже проксирует `/api` на `8080`, допустимо использовать пустой base URL в development. Не хардкодь полный адрес в компонентах: вынеси его в единый HTTP-клиент.

## API-контракт для пользовательского интерфейса

### Общие правила

- Идентификаторы `id`, `cityId`, `variantId`, `offerId` — UUID-строки.
- Публичный путь модели использует **только `catalogKey`**, например `apple-iphone-16-pro`. Не подменяй его UUID или slug из frontend.
- Все цены — числа в RUB. Форматируй через `Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 0 })`.
- Для постраничных списков backend возвращает:

```ts
type Slice<T> = {
  items: T[];
  page: number;
  size: number;
  hasNext: boolean;
};
```

- Ошибки, пустая выдача и загрузка — отдельные UI-состояния, а не пустой белый экран.

### 1. Выбор города

```http
GET /api/cities
```

Ответ:

```ts
type City = {
  id: string;
  name: string;
  countryCode: string;
  timezone: string | null;
};
```

Сделай город обязательным контекстом каталога: пользователь выбирает его до просмотра цен. Сохраняй выбранный `cityId` в URL и/или localStorage согласно существующему подходу frontend. Если город ещё не выбран, объясни это в интерфейсе и не показывай цену как универсальную.

### 2. Каталог и поиск

```http
GET /api/products?q=&cityId=&category=&brand=&page=&size=
```

Поддержанные сейчас фильтры: `q`, `cityId`, `category`, `brand`, `page`, `size`.

Не отправляй на backend пока несуществующие параметры (`storage`, `color`, `minPrice`, `inStock`, `sort`) как будто они работают. Их можно оставить только как локальные фильтры уже загруженных вариантов, если это действительно нужно интерфейсу и честно отражено в UX.

```ts
type ProductListItem = {
  id: string;
  catalogKey: string;
  brand: string | null;
  name: string;
  category: string | null;
  description: string | null;
  minEffectivePrice: number | null;
  currency: string | null;
  offerCount: number;
};
```

Карточка списка должна показывать название, бренд, минимальную цену **в выбранном городе** и число предложений. Если `minEffectivePrice === null`, отобрази «Нет актуальных предложений», а не `0 ₽`.

Ссылка на карточку товара: `/products/:catalogKey` (либо существующий эквивалентный роут, но с `catalogKey` в URL).

### 3. Карточка товара и конфигурации

```http
GET /api/products/{catalogKey}?cityId={cityId}
GET /api/products/{catalogKey}/variants?cityId={cityId}
```

Основной endpoint карточки уже возвращает `variants`; второй endpoint нужен для отдельного обновления списка конфигураций, если это полезно для текущей архитектуры.

```ts
type VariantAttribute = {
  code: string;           // storage | color | sim | ...
  name: string;
  dataType: 'TEXT' | 'NUMBER' | 'BOOLEAN';
  unit: string | null;
  value: string | null;
  numericValue: number | null;
  booleanValue: boolean | null;
};

type ProductVariant = {
  id: string;
  canonicalKey: string;
  displayName: string;
  manufacturerSku: string | null;
  attributes: VariantAttribute[];
  minEffectivePrice: number | null;
  currency: string | null;
  offerCount: number;
  available: boolean;
};

type ProductDetail = ProductListItem & {
  variants: ProductVariant[];
};
```

Собери селекторы памяти, цвета и SIM динамически из `attributes`, а не из заранее прописанного массива iPhone. Выбор конфигурации должен работать по `variant.id`.

Правила отображения:

- `available: false` — позиция существует, но сейчас недоступна; не удаляй её без явного фильтра пользователя;
- варианты с `minEffectivePrice: null` не должны выглядеть как бесплатные;
- используй `displayName` как заголовок, а `attributes` — как фильтры и характеристики;
- состояние выбранного варианта и города должно переживать обновление страницы через URL, если это не противоречит архитектуре приложения.

### 4. Предложения магазинов

Предпочтительный endpoint:

```http
GET /api/variants/{variantId}/offers?cityId={cityId}&page=0&size=20
```

Эквивалентный endpoint существует как `GET /api/offers?variantId=&cityId=`, но не используй оба для одной задачи.

```ts
type OfferTerm = {
  id: string;
  type: 'GIFT' | 'DISCOUNT' | 'CONDITIONAL_PRICE' | 'CASHBACK' |
        'TRADE_IN' | 'INSTALLMENT' | 'CREDIT' | 'DELIVERY' | 'OTHER';
  title: string;
  description: string | null;
  monetaryValue: number | null;
  annualRate: number | null;
  termMonths: number | null;
  monthlyPayment: number | null;
  initialPayment: number | null;
  validUntil: string | null;
  termsUrl: string | null;
};

type Store = {
  id: string;
  name: string;
  address: string | null;
};

type Offer = {
  id: string;
  marketId: string;
  chainId: string;
  chainName: string;
  cityId: string;
  cityName: string;
  store: Store | null;
  productId: string;
  productName: string;
  variantId: string;
  variantName: string;
  condition: 'NEW' | 'USED' | 'REFURBISHED' | 'DISPLAY';
  regularPrice: number | null;
  salePrice: number | null;
  conditionalPrice: number | null;
  effectivePrice: number | null;
  currency: string;
  availability: 'IN_STOCK' | 'OUT_OF_STOCK' | 'PREORDER' | 'UNKNOWN';
  quantity: number | null;
  productUrl: string | null;
  lastCheckedAt: string;
  terms: OfferTerm[];
};
```

Предложения уже отсортированы backend по текущей цене. Не меняй порядок без явного пользовательского выбора.

Для каждой строки предложения покажи:

- сеть, город и цену `effectivePrice`;
- `regularPrice` зачёркнутой только если есть более низкая `salePrice`;
- статус наличия и количество, если `quantity !== null`;
- состояние товара (`NEW`, `USED`, `REFURBISHED`, `DISPLAY`) понятным русским текстом;
- чипы/блок условий из `terms`: подарок, скидка, рассрочка и т.д.;
- дату `lastCheckedAt` в локальном понятном формате;
- переход на `productUrl` в новой вкладке с `rel="noreferrer noopener"`.

У 941 на первом этапе цена является городской: `store === null`, а остатки по отдельным филиалам могут отсутствовать. Это корректные данные. Показывай «Цена для города» и не рисуй ошибку или несуществующий адрес.

### 5. Детали предложения, история и наличие

```http
GET /api/offers/{offerId}
GET /api/offers/{offerId}/history?from={ISO-8601}&to={ISO-8601}
GET /api/offers/{offerId}/inventory?page=0&size=20
```

```ts
type PriceHistoryPoint = {
  id: number;
  regularPrice: number | null;
  salePrice: number | null;
  conditionalPrice: number | null;
  effectivePrice: number | null;
  currency: string;
  availability: 'IN_STOCK' | 'OUT_OF_STOCK' | 'PREORDER' | 'UNKNOWN';
  quantity: number | null;
  validFrom: string;
  validTo: string | null;
};

type InventoryItem = {
  storeId: string;
  storeName: string;
  address: string | null;
  availability: 'IN_STOCK' | 'OUT_OF_STOCK' | 'PREORDER' | 'UNKNOWN';
  quantity: number | null;
  lastCheckedAt: string;
};
```

Для графика:

- одна запись истории — это интервал состояния от `validFrom` до `validTo`;
- `validTo: null` означает, что состояние актуально сейчас;
- рисуй цену `effectivePrice`; интервалы доступности используй для подсказок или фона;
- не генерируй искусственные ежедневные точки: backend хранит только реальные изменения;
- если истории одна точка, это нормальный новый товар — покажи аккуратное состояние «история начнёт накапливаться после изменений», а не ошибку графика.

Если inventory вернул пустой список, показывай «По филиалам данные пока не переданы». Это не `OUT_OF_STOCK`.

### 6. Предложения из других городов с доставкой

```http
GET /api/variants/{variantId}/delivered-offers?cityId={destinationCityId}&page=0&size=20
```

```ts
type DeliveredOffer = {
  offerId: string;
  chainName: string;
  sourceCityName: string;
  itemPrice: number;
  deliveryPrice: number;
  totalPrice: number;
  minDeliveryDays: number | null;
  maxDeliveryDays: number | null;
  currency: string;
  productUrl: string | null;
};
```

Секция должна появляться только при непустом `items`. Сейчас для первого живого импорта 941 shipping quotes ещё нет, поэтому пустой ответ — штатный результат, не ошибка.

## Что реализовать

1. Создай единый типизированный API-клиент и типы контрактов выше. Если проект использует React Query, TanStack Query, RTK Query или другой подход, следуй уже принятому решению.
2. Убери из пользовательского сценария mock-цены и жёстко прошитые варианты; оставь mock только там, где он действительно нужен тестам и явно изолирован.
3. Добавь загрузку городов и выбор города.
4. Подключи каталог к `GET /api/products` с дебаунсом поиска, отменой устаревших запросов и пагинацией при `hasNext`.
5. Подключи карточку товара, динамические характеристики и выбор варианта.
6. Подключи предложения выбранного варианта, детали, условия, историю цены и наличие.
7. Обработай пустые, загрузочные и ошибочные состояния на каждом экране. Ошибку backend не скрывай: покажи пользователю понятный текст и возможность повторить запрос.
8. Добавь или обнови тесты для API-моделей, ключевых loading/empty/error состояний и сценария выбора варианта.
9. Проверь сборку, линтер и тесты командыми, уже принятыми в frontend-репозитории.

## Что не делать

- Не реализовывай интерфейс управления `/api/admin/*` в публичной витрине: в MVP там нет авторизации. Если в проекте уже существует защищённая внутренняя admin-зона, можно подключить её отдельным модулем, но не делай её видимой обычному покупателю.
- Не создавай во frontend отдельную бизнес-логику расчёта итоговой цены: используй `effectivePrice` и `totalPrice` backend.
- Не объявляй `OUT_OF_STOCK`, когда пришёл `UNKNOWN`, пустой inventory или нет shipping quotes.
- Не меняй backend, базу, миграции или API-маршруты ради удобства mock-данных.
- Не подставляй реальные UUID из локальной базы в код, тесты или маршруты.

## Критерии готовности

На локально запущенном backend пользователь может:

1. выбрать Екатеринбург;
2. найти `iPhone 16 Pro`;
3. открыть `apple-iphone-16-pro`;
4. выбрать вариант по памяти, цвету и SIM;
5. увидеть реальное предложение 941, цену, наличие и условия;
6. открыть историю цены выбранного предложения;
7. корректно увидеть пустое состояние доставки и филиалов, когда данных нет;
8. не увидеть никаких mock-цен вместо ответа backend.

В финальном отчёте перечисли изменённые файлы, выполненные команды проверки, реальные UI-сценарии и известные ограничения. Отдельно укажи, если backend не был запущен или ответ API не удалось проверить.
