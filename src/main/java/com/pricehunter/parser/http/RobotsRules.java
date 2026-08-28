package com.pricehunter.parser.http;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Минимальная модель правил robots.txt, необходимых безопасному клиенту парсера. */
final class RobotsRules {

    private final List<Rule> rules;
    private final Duration crawlDelay;

    /** Сохраняет неизменяемый набор правил и минимально допустимую задержку. */
    private RobotsRules(List<Rule> rules, Duration crawlDelay) {
        this.rules = List.copyOf(rules);
        this.crawlDelay = crawlDelay;
    }

    /** @return разрешающие всё правила с безопасной задержкой в одну секунду */
    static RobotsRules allowAll() {
        return new RobotsRules(List.of(), Duration.ofSeconds(1));
    }

    /** Разбирает секции User-agent, Allow, Disallow и Crawl-delay для Price Hunter или всех роботов. */
    static RobotsRules parse(String content) {
        List<Rule> rules = new ArrayList<>();
        Duration delay = Duration.ofSeconds(1);
        boolean applies = false;

        for (String rawLine : content.split("\\R")) {
            String line = rawLine.replaceFirst("#.*$", "").trim();
            if (line.isEmpty() || !line.contains(":")) {
                continue;
            }
            String[] parts = line.split(":", 2);
            String field = parts[0].trim().toLowerCase(Locale.ROOT);
            String value = parts[1].trim();
            if (field.equals("user-agent")) {
                applies = value.equals("*") || value.equalsIgnoreCase("PriceHunter");
            } else if (applies && (field.equals("allow") || field.equals("disallow")) && !value.isEmpty()) {
                rules.add(new Rule(value, field.equals("allow")));
            } else if (applies && field.equals("crawl-delay")) {
                try {
                    delay = Duration.ofMillis(Math.max(1000L, Math.round(Double.parseDouble(value) * 1000)));
                } catch (NumberFormatException ignored) {
                    delay = Duration.ofSeconds(1);
                }
            }
        }
        return new RobotsRules(rules, delay);
    }

    /** Определяет доступ по самому длинному совпавшему правилу; Allow выигрывает при равной длине. */
    boolean allows(URI uri) {
        String target = uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        Rule winner = null;
        for (Rule rule : rules) {
            if (rule.matches(target) && (winner == null || rule.path().length() > winner.path().length()
                    || rule.path().length() == winner.path().length() && rule.allow())) {
                winner = rule;
            }
        }
        return winner == null || winner.allow();
    }

    /** @return требуемый промежуток между запросами к одному домену */
    Duration crawlDelay() {
        return crawlDelay;
    }

    /** Одно разрешающее или запрещающее правило пути. */
    private record Rule(String path, boolean allow) {
        /** Проверяет путь и query-параметры адреса с поддержкой шаблона {@code *}. */
        boolean matches(String target) {
            String regex = path
                    .replace(".", "\\.")
                    .replace("?", "\\?")
                    .replace("*", ".*")
                    .replace("$", "$");
            return target.matches("^" + regex + ".*");
        }
    }
}
