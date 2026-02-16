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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private static final long RESPONSE_TIMEOUT_MS = 650;
    private static final long BETWEEN_RULES_MS = 120;

    private final ServerOutputListener delegate;
    private final LoadedRules data;
    private final ScheduledExecutorService scheduler;
    private Iterator<Map.Entry<String, String>> iterator;
    private volatile boolean started;
    private volatile boolean finished;
    private String currentCanonical;
    private String currentValue;
    private List<String> currentNames;
    private int currentNameIndex;
    private ScheduledFuture<?> timeoutFuture;

    public static ServerOutputListener wrap(ServerOutputListener delegate, Path jsonFile) {
        Objects.requireNonNull(delegate, "delegate");
        Objects.requireNonNull(jsonFile, "jsonFile");

        var loaded = loadRuleFile(jsonFile);
        if (loaded.rules.isEmpty()) return delegate;

        return new GameRuleFileApplier(delegate, loaded);
    }

    private record LoadedRules(LinkedHashMap<String, String> rules, Map<String, List<String>> aliasesByRuleName) {
    }

    private static LoadedRules loadRuleFile(Path file) {
        if (!Files.exists(file)) {
            LogViewer.system("Gamerule file missing: " + file);
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
        if (!line.contains(" INFO]: Incorrect argument for command") && !UNKNOWN_GAMERULE_4.matcher(line).matches())
            delegate.onLine(server, stream, line);

        if (!started && SERVER_READY.matcher(line).matches()) {
            started = true;
            iterator = data.rules.entrySet().iterator();
            LogViewer.system("Server ready — applying gamerules from file...");
            scheduler.execute(this::applyNextRule);
            return;
        }

        if (!started || finished) return;
        if (currentCanonical == null) return;

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
        currentValue = entry.getValue();

        currentNames = buildNameList(currentCanonical);
        currentNameIndex = 0;

        sendAttempt();
    }

    private List<String> buildNameList(String canonical) {
        List<String> aliases = data.aliasesByRuleName.get(canonical);
        if (aliases == null || aliases.isEmpty()) return List.of(canonical);

        var list = new ArrayList<String>(1 + aliases.size());
        list.add(canonical);
        list.addAll(aliases);
        return List.copyOf(list);
    }

    private void sendAttempt() {
        cancelTimeout();

        if (currentNameIndex >= currentNames.size()) {
            LogViewer.system("Failed to apply gamerule '" + currentCanonical + "' (no alias worked).");
            scheduleNext();
            return;
        }

        String name = currentNames.get(currentNameIndex);
        ServerHandlerAPI.sendServerCommand("gamerule " + name + " " + currentValue);

        timeoutFuture = scheduler.schedule(() -> assumeSuccess(name), RESPONSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private void tryNextName() {
        cancelTimeout();
        currentNameIndex++;
        sendAttempt();
    }

    private void skipInvalidValue() {
        cancelTimeout();
        LogViewer.system("Invalid value for gamerule '" + currentCanonical + "': " + currentValue);
        scheduleNext();
    }

    private void assumeSuccess(String usedName) {
        LogViewer.system("Set gamerule '" + usedName + "' = " + currentValue);
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
