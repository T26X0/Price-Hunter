import { Container, getContainer } from "@cloudflare/containers";
import { env as workerEnv } from "cloudflare:workers";

interface Env {
  PRICE_HUNTER_CONTAINER: DurableObjectNamespace<PriceHunterContainer>;
  DB_URL: string;
  DB_USERNAME: string;
  DB_PASSWORD: string;
  ADMIN_API_TOKEN: string;
  CORS_ALLOWED_ORIGINS: string;
}

const runtimeEnv = workerEnv as unknown as Env;

/** Запускает единственный production-экземпляр Spring Boot и передаёт ему секреты только через окружение. */
export class PriceHunterContainer extends Container {
  defaultPort = 8080;
  requiredPorts = [8080];
  sleepAfter = "10m";
  enableInternet = true;
  pingEndpoint = "localhost/healthz";
  envVars = {
    DB_URL: runtimeEnv.DB_URL,
    DB_USERNAME: runtimeEnv.DB_USERNAME,
    DB_PASSWORD: runtimeEnv.DB_PASSWORD,
    CORS_ALLOWED_ORIGINS: runtimeEnv.CORS_ALLOWED_ORIGINS,
    PARSER_SCHEDULING_ENABLED: "false"
  };
}

/** Сравнивает bearer-токен без публикации секрета в Java-контейнере или репозитории. */
function isAdminAuthorized(request: Request, env: Env): boolean {
  return request.headers.get("authorization") === `Bearer ${env.ADMIN_API_TOKEN}`;
}

/** Возвращает стандартный JSON-ответ для закрытого административного API. */
function unauthorized(): Response {
  return Response.json(
    { status: 401, error: "Unauthorized", message: "Admin bearer token is required" },
    { status: 401, headers: { "www-authenticate": "Bearer" } }
  );
}

/** Возвращает стабильную ссылку на единственный экземпляр backend-контейнера. */
function backend(env: Env) {
  return getContainer(env.PRICE_HUNTER_CONTAINER, "production");
}

export default {
  /** Закрывает /api/admin на границе Cloudflare и передаёт остальные запросы Spring Boot. */
  async fetch(request: Request, env: Env): Promise<Response> {
    const path = new URL(request.url).pathname;
    if (path.startsWith("/api/admin") && !isAdminAuthorized(request, env)) {
      return unauthorized();
    }
    return backend(env).fetch(request);
  },

  /** Раз в сутки будит контейнер и запускает один ограниченный проход по просроченным источникам. */
  async scheduled(_controller: ScheduledController, env: Env): Promise<void> {
    const container = backend(env);
    await container.startAndWaitForPorts({
      ports: [8080],
      cancellationOptions: { portReadyTimeoutMS: 120_000 }
    });

    const response = await container.fetch(new Request(
      "http://price-hunter.internal/api/admin/parser-scheduler/run",
      {
        method: "POST",
        headers: { authorization: `Bearer ${env.ADMIN_API_TOKEN}` }
      }
    ));
    if (!response.ok) {
      throw new Error(`Scheduled parser sweep failed with HTTP ${response.status}`);
    }
  }
} satisfies ExportedHandler<Env>;
