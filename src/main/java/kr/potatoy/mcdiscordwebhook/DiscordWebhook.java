package kr.potatoy.mcdiscordwebhook;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static kr.potatoy.mcdiscordwebhook.McDiscordWebhook.LOGGER;

public final class DiscordWebhook {
    private static final Path CONFIG = Path.of("config", "mcdiscordwebhook.properties");
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private static final URI URL = readUrl();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private static final ExecutorService SENDER = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setNameFormat("mcdiscordwebhook-sender").setDaemon(true).build());

    static {
        if (URL == null) SENDER.shutdown();
    }

    private DiscordWebhook() {
    }

    public static void send(String content) {
        enqueue(newMessage(content));
    }

    public static void send(String content, String username, String avatarUrl) {
        String name = username.toLowerCase(Locale.ROOT);
        if (name.contains("discord") || name.contains("clyde")) return;

        JsonObject message = newMessage(content);
        message.addProperty("username", username);
        message.addProperty("avatar_url", avatarUrl);
        enqueue(message);
    }

    public static void close() {
        SENDER.shutdown();
        try {
            if (!SENDER.awaitTermination(5, TimeUnit.SECONDS)) LOGGER.warn("Unsent webhook messages dropped");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void enqueue(JsonObject message) {
        if (SENDER.isShutdown()) return;
        String json = message.toString();
        SENDER.execute(() -> {
            try {
                post(json);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOGGER.warn("Webhook send failed: {}", e.toString());
            }
        });
    }

    private static void post(String json) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URL)
            .timeout(TIMEOUT)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        IOException failure = null;
        Duration wait = Duration.ZERO;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            Thread.sleep(wait.toMillis());

            HttpResponse<Void> response;
            try {
                response = HTTP.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (IOException e) {
                failure = e;
                wait = Duration.ofSeconds(attempt);
                continue;
            }

            int status = response.statusCode();
            if (status < 300) {
                if (response.headers().firstValue("X-RateLimit-Remaining").orElse("").equals("0")) {
                    Thread.sleep(seconds(response, "X-RateLimit-Reset-After").toMillis());
                }
                return;
            }
            if (status == 401 || status == 404) {
                SENDER.shutdownNow();
                throw new IOException("Invalid webhook (HTTP " + status + ")");
            }
            failure = new IOException("HTTP " + status);
            if (status != 429 && status < 500) throw failure;
            wait = status == 429 ? seconds(response, "Retry-After") : Duration.ofSeconds(attempt);
        }
        throw failure;
    }

    private static Duration seconds(HttpResponse<?> response, String header) {
        return Duration.ofMillis((long) (Double.parseDouble(response.headers().firstValue(header).orElse("1")) * 1000));
    }

    private static JsonObject newMessage(String content) {
        JsonObject message = new JsonObject();
        message.addProperty("content", content);

        JsonObject allowedMentions = new JsonObject();
        allowedMentions.add("parse", new JsonArray());
        message.add("allowed_mentions", allowedMentions);
        return message;
    }

    private static URI readUrl() {
        try {
            if (Files.notExists(CONFIG)) {
                Files.createDirectories(CONFIG.getParent());
                Files.writeString(CONFIG, """
                    # Discord webhook URL
                    webhook-url=
                    """);
            }

            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(CONFIG)) {
                properties.load(reader);
            }

            String value = properties.getProperty("webhook-url", "").trim();
            if (!value.isEmpty()) return URI.create(value);
            LOGGER.warn("Set webhook-url in {}", CONFIG);
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.error("Could not read {}: {}", CONFIG, e.toString());
        }
        return null;
    }
}
