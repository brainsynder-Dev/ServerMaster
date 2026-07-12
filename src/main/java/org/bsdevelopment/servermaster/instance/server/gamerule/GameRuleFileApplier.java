package org.bsdevelopment.servermaster.instance.server.gamerule;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.bsdevelopment.servermaster.LogViewer;
import org.bsdevelopment.servermaster.instance.server.Server;
import org.bsdevelopment.servermaster.instance.server.ServerHandlerAPI;
import org.bsdevelopment.servermaster.instance.server.thread.ServerOutputListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * File format:
 * <pre>
 * {
 *   "rules": [
 *     { "name": "keepInventory", "value": "true" },
 *     { "name": "doDaylightCycle", "value": "false", "aliases": ["oldName1"] }
 *   ]
 * }
 * </pre>
 */
public final class GameRuleFileApplier implements ServerOutputListener {
    private static final Pattern SERVER_READY = Pattern.compile(".*\\bDone \\(.*\\)!.*");
    private static final Pattern UNKNOWN_GAMERULE_1 = Pattern.compile(".*Unknown game rule.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNKNOWN_GAMERULE_2 = Pattern.compile(".*No game rule called.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNKNOWN_GAMERULE_3 = Pattern.compile(".*Incorrect argument for command.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNKNOWN_GAMERULE_4 = Pattern.compile(".*gamerule (.*) (.*)<--\\[HERE\\].*", Pattern.CASE_INSENSITIVE);
    private static final Pattern INVALID_VALUE = Pattern.compile(
            ".*(Invalid|Expected).*(true|false|boolean|integer|int).*",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern GAMERULE_SET = Pattern.compile(".*is now set to.*", Pattern.CASE_INSENSITIVE);
    private static final long RESPONSE_TIMEOUT_MS = 650;
    private static final long BETWEEN_RULES_MS = 120;

    private final ServerOutputListener delegate;
    private final LoadedRules data;
    private final ScheduledExecutorService scheduler;
    private Iterator<Map.Entry<String, String>> iterator;
    private volatile boolean started;
    private volatile boolean finished;
    private String currentCanonical;
    private List<Attempt> currentAttempts;
    private int currentAttemptIndex;
    private String currentAttemptName;
    private String currentAttemptValue;
    private String currentSentCommand;
    private String lastErrorDetail;
    private ScheduledFuture<?> timeoutFuture;

    public static ServerOutputListener wrap(ServerOutputListener outputListener, Path jsonFile) {
        Objects.requireNonNull(outputListener, "Missing server output listener");
        Objects.requireNonNull(jsonFile, "Missing gamerule JSON file");

        var loaded = loadRuleFile(jsonFile);
        if (loaded.rules.isEmpty()) return outputListener;

        return new GameRuleFileApplier(outputListener, loaded);
    }

    private record LoadedRules(LinkedHashMap<String, String> rules, Map<String, List<String>> aliasesByRuleName) {
    }

    private record Attempt(String name, String value) {
    }

    private static LoadedRules loadRuleFile(Path file) {
        if (!Files.exists(file)) {
            LogViewer.system("Gamerule file missing: " + file.toAbsolutePath());
            return new LoadedRules(new LinkedHashMap<>(), Map.of());
        }

        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject root = Json.parse(raw).asObject();

            JsonArray rulesArray = root.get("rules") != null && root.get("rules").isArray()
                    ? root.get("rules").asArray()
                    : new JsonArray();

            var rules = new LinkedHashMap<String, String>();
            var aliases = new LinkedHashMap<String, List<String>>();

            for (JsonValue v : rulesArray) {
                if (v == null || !v.isObject()) continue;

                JsonObject o = v.asObject();
                String name = stringOrNull(o.get("name"));
                String value = stringOrNull(o.get("value"));

                if (name == null || name.isBlank()) continue;
                if (value == null || value.isBlank()) continue;

                List<String> aliasList = readStringList(o.get("aliases"));
                rules.put(name, value);
                aliases.put(name, aliasList);
            }

            return new LoadedRules(rules, aliases);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read gamerule file: " + file, e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to parse gamerule file: " + file, e);
        }
    }

    private static String stringOrNull(JsonValue v) {
        if (v == null || !v.isString()) return null;
        return v.asString();
    }

    private static List<String> readStringList(JsonValue v) {
        if (v == null || !v.isArray()) return List.of();

        var arr = v.asArray();
        var list = new ArrayList<String>(arr.size());
        for (JsonValue item : arr) {
            if (item == null || !item.isString()) continue;
            String s = item.asString();
            if (s == null || s.isBlank()) continue;
            list.add(s);
        }
        return List.copyOf(list);
    }

    private GameRuleFileApplier(ServerOutputListener delegate, LoadedRules data) {
        this.delegate = delegate;
        this.data = data;

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ServerMaster-GameRule");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void onLine(Server server, Stream stream, String line) {
        boolean parseError = line.contains(" INFO]: Incorrect argument for command") || UNKNOWN_GAMERULE_4.matcher(line).matches();
        if (!parseError) delegate.onLine(server, stream, line);

        if (started && !finished && currentCanonical != null && UNKNOWN_GAMERULE_4.matcher(line).matches()) {
            lastErrorDetail = line.trim();
        }

        if (!started && SERVER_READY.matcher(line).matches()) {
            started = true;
            iterator = data.rules.entrySet().iterator();
            LogViewer.system("Server ready — applying gamerules from file...");
            scheduler.execute(this::applyNextRule);
            return;
        }

        if (!started || finished) return;
        if (currentCanonical == null) return;

        if (currentAttemptName != null && GAMERULE_SET.matcher(line).matches()
                && line.toLowerCase().contains(currentAttemptName.toLowerCase())) {
            scheduler.execute(this::confirmSuccess);
            return;
        }

        if (UNKNOWN_GAMERULE_1.matcher(line).matches()
                || UNKNOWN_GAMERULE_2.matcher(line).matches()
                || UNKNOWN_GAMERULE_3.matcher(line).matches()) {
            scheduler.execute(this::tryNextName);
            return;
        }

        if (INVALID_VALUE.matcher(line).matches()) {
            scheduler.execute(this::skipInvalidValue);
        }
    }

    private void applyNextRule() {
        if (finished) return;

        if (iterator == null || !iterator.hasNext()) {
            finished = true;
            LogViewer.system("Gamerules applied.");
            scheduler.shutdown();
            return;
        }

        var entry = iterator.next();
        currentCanonical = entry.getKey();

        currentAttempts = buildAttempts(currentCanonical, entry.getValue());
        currentAttemptIndex = 0;
        lastErrorDetail = null;

        sendAttempt();
    }

    private List<Attempt> buildAttempts(String canonical, String value) {
        var attempts = new ArrayList<Attempt>();
        var seenNames = new ArrayList<String>();

        addAttempt(attempts, seenNames, canonical, value);

        GameRuleCatalog.GameRule rule = GameRuleCatalog.find(canonical);
        if (rule != null) {
            for (GameRuleCatalog.Alias alias : rule.aliases()) {
                String aliasValue = alias.invertBoolean() ? invertBoolean(value) : value;
                addAttempt(attempts, seenNames, alias.name(), aliasValue);
            }
        }

        List<String> userAliases = data.aliasesByRuleName.get(canonical);
        if (userAliases != null) {
            for (String userAlias : userAliases) addAttempt(attempts, seenNames, userAlias, value);
        }

        return List.copyOf(attempts);
    }

    private static void addAttempt(List<Attempt> attempts, List<String> seenNames, String name, String value) {
        if (name == null || name.isBlank()) return;
        for (String seen : seenNames) {
            if (seen.equalsIgnoreCase(name)) return;
        }
        seenNames.add(name);
        attempts.add(new Attempt(name, value));
    }

    private static String invertBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) return "false";
        if ("false".equalsIgnoreCase(value)) return "true";
        return value;
    }

    private void sendAttempt() {
        cancelTimeout();

        if (currentAttemptIndex >= currentAttempts.size()) {
            currentAttemptName = null;
            String reason = lastErrorDetail != null ? "server rejected it: " + lastErrorDetail
                    : "no known name for this version worked";
            LogViewer.system("Could not apply gamerule '" + currentCanonical + "' — " + reason
                    + "  [sent: \"" + visible(currentSentCommand) + "\", length="
                    + (currentSentCommand == null ? 0 : currentSentCommand.length()) + "]");
            scheduleNext();
            return;
        }

        Attempt attempt = currentAttempts.get(currentAttemptIndex);
        currentAttemptName = attempt.name();
        currentAttemptValue = attempt.value();

        String command = "gamerule " + sanitizeToken(attempt.name()) + " " + sanitizeToken(attempt.value());
        currentSentCommand = command;
        ServerHandlerAPI.sendServerCommand(command);

        timeoutFuture = scheduler.schedule(() -> assumeSuccess(attempt.name(), attempt.value()), RESPONSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private static String sanitizeToken(String token) {
        if (token == null) return "";

        var builder = new StringBuilder(token.length());
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (Character.isISOControl(c)) continue;
            if (Character.getType(c) == Character.FORMAT) continue;
            builder.append(c);
        }
        return builder.toString().strip();
    }

    private static String visible(String value) {
        if (value == null) return "";

        var builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r') builder.append("\\r");
            else if (c == '\n') builder.append("\\n");
            else if (c == '\t') builder.append("\\t");
            else if (c < 0x20 || c == 0x7F || Character.getType(c) == Character.FORMAT) builder.append(String.format("\\u%04X", (int) c));
            else builder.append(c);
        }
        return builder.toString();
    }

    private void confirmSuccess() {
        if (currentAttemptName != null) assumeSuccess(currentAttemptName, currentAttemptValue);
    }

    private void tryNextName() {
        cancelTimeout();
        currentAttemptName = null;
        currentAttemptIndex++;
        sendAttempt();
    }

    private void skipInvalidValue() {
        cancelTimeout();
        LogViewer.system("Invalid value for gamerule '" + currentCanonical + "': " + currentAttemptValue);
        currentAttemptName = null;
        scheduleNext();
    }

    private void assumeSuccess(String usedName, String usedValue) {
        if (currentAttemptName == null) return;
        currentAttemptName = null;
        LogViewer.system("Set gamerule '" + usedName + "' = " + usedValue);
        scheduleNext();
    }

    private void scheduleNext() {
        cancelTimeout();
        scheduler.schedule(this::applyNextRule, BETWEEN_RULES_MS, TimeUnit.MILLISECONDS);
    }

    private void cancelTimeout() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
            timeoutFuture = null;
        }
    }
}
