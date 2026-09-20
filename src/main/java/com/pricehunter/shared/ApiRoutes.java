package com.pricehunter.shared;

/**
 * Единственное место с публичными HTTP-маршрутами Price Hunter.
 * Контроллеры используют эти константы, чтобы API не расходился между кодом, Swagger и frontend.
 */
public final class ApiRoutes {

    /** Общий префикс публичного REST API. */
    public static final String ROOT = "/api";

    private ApiRoutes() {
    }

    /** Маршруты справочника городов. */
    public static final class Cities {
        public static final String ROOT = ApiRoutes.ROOT + "/cities";

        private Cities() {
        }
    }

    /** Маршруты канонических моделей товаров. */
    public static final class Products {
        public static final String ROOT = ApiRoutes.ROOT + "/products";
        public static final String BY_IDENTIFIER = "/{catalogKey}";
        public static final String VARIANTS = BY_IDENTIFIER + "/variants";

        private Products() {
        }
    }

    /** Маршруты текущих предложений магазинов. */
    public static final class Offers {
        public static final String ROOT = ApiRoutes.ROOT + "/offers";
        public static final String BY_ID = "/{offerId}";
        public static final String HISTORY = BY_ID + "/history";
        public static final String INVENTORY = BY_ID + "/inventory";

        private Offers() {
        }
    }

    /** Маршруты сравнения одной конфигурации товара. */
    public static final class Variants {
        public static final String ROOT = ApiRoutes.ROOT + "/variants";
        public static final String OFFERS = "/{variantId}/offers";
        public static final String DELIVERED_OFFERS = "/{variantId}/delivered-offers";

        private Variants() {
        }
    }

    /** Маршруты минимального контура настройки и ручного запуска парсеров. */
    public static final class Admin {
        public static final String ROOT = ApiRoutes.ROOT + "/admin";
        public static final String CITIES = "/cities";
        public static final String CHAINS = "/chains";
        public static final String MARKETS = "/markets";
        public static final String PARSER_SOURCES = "/parser-sources";
        public static final String PARSER_JOBS = "/parser-jobs";
        public static final String PARSER_SCHEDULER_RUN = "/parser-scheduler/run";
        public static final String REVIEW_CANDIDATES = "/review-candidates";

        private Admin() {
        }
    }
}
