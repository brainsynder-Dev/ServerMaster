package org.bsdevelopment.servermaster.instance.server.gamerule;

import java.util.List;

public class GameRuleCatalog {

    public enum Type {BOOLEAN, INTEGER}

    public record Alias(String name, boolean invertBoolean) {
    }

    public record GameRule(String name, Type type, String defaultValue, String since, String description,
                           Integer minimum, Integer maximum, List<Alias> aliases) {
        public boolean isBoolean() {
            return type == Type.BOOLEAN;
        }
    }

    private static final List<GameRule> RULES = List.of(
            bool("announceAdvancements", "true", "1.12", "Whether advancements are announced in chat.", "show_advancement_messages"),
            bool("blockExplosionDropDecay", "true", "1.21", "Whether blocks destroyed by block-caused explosions (e.g. beds, respawn anchors) lose some of their drops."),
            bool("commandBlockOutput", "true", "1.7.2", "Whether command blocks notify admins when they run a command."),
            integer("commandModificationBlockLimit", "32768", "1.19.4", "Maximum number of blocks a single command (fill, clone, etc.) may change.", "max_block_modifications", 1, null),
            boolInverted("disableElytraMovementCheck", "false", "1.9", "Whether the server skips its speed check for players gliding with elytra.", "elytra_movement_check"),
            boolInverted("disablePlayerMovementCheck", "false", "1.20.5", "Whether the server skips its player movement-speed check entirely.", "player_movement_check"),
            boolInverted("disableRaids", "false", "1.14.3", "Whether pillager raids are disabled.", "raids"),
            bool("doDaylightCycle", "true", "1.6.1", "Whether time advances and the daylight/moon cycle progresses.", "advance_time"),
            bool("doEntityDrops", "true", "1.8.1", "Whether non-mob entities (minecarts, item frames, boats) drop items.", "entity_drops"),
            bool("doFireTick", "true", "1.8.1", "Whether fire spreads and naturally extinguishes."),
            bool("doImmediateRespawn", "false", "1.15", "Whether players respawn immediately without the death screen.", "immediate_respawn"),
            bool("doInsomnia", "true", "1.15", "Whether phantoms spawn for sleepless players.", "spawn_phantoms"),
            bool("doLimitedCrafting", "false", "1.14.3", "Whether players can only craft recipes they have unlocked.", "limited_crafting"),
            bool("doMobLoot", "true", "1.8.1", "Whether mobs drop items and experience when killed.", "mob_drops"),
            bool("doMobSpawning", "true", "1.8.1", "Whether mobs spawn naturally. Does not affect spawners.", "spawn_mobs"),
            bool("doPatrolSpawning", "true", "1.15.2", "Whether pillager patrols spawn.", "spawn_patrols"),
            bool("doTileDrops", "true", "1.8.1", "Whether broken blocks drop as items.", "block_drops"),
            bool("doTraderSpawning", "true", "1.15.2", "Whether wandering traders spawn naturally.", "spawn_wandering_traders"),
            bool("doVinesSpread", "true", "1.19", "Whether vines spread randomly to nearby blocks.", "spread_vines"),
            bool("doWardenSpawning", "true", "1.19", "Whether wardens can spawn.", "spawn_wardens"),
            bool("doWeatherCycle", "true", "1.11", "Whether weather changes naturally.", "advance_weather"),
            bool("drowningDamage", "true", "1.15", "Whether players take drowning damage."),
            bool("enderPearlsVanishOnDeath", "true", "1.21", "Whether thrown ender pearls vanish when the owner dies."),
            bool("fallDamage", "true", "1.15", "Whether players take fall damage."),
            bool("fireDamage", "true", "1.15", "Whether players take fire and lava damage."),
            bool("forgiveDeadPlayers", "true", "1.16", "Whether angered neutral mobs calm down once their target player dies."),
            bool("freezeDamage", "true", "1.17", "Whether players take freezing damage from powder snow."),
            bool("globalSoundEvents", "true", "1.20", "Whether world-wide events (ender dragon death, Wither spawn) are heard everywhere."),
            bool("keepInventory", "false", "1.4.2", "Whether players keep their inventory and experience after death."),
            bool("lavaSourceConversion", "false", "1.19", "Whether flowing lava between two sources forms a new lava source."),
            bool("logAdminCommands", "true", "1.7.2", "Whether admin commands are written to the server log."),
            integer("maxCommandChainLength", "65536", "1.12", "Maximum number of chained commands a function or command block may run.", "max_command_sequence_length", 0, null),
            integer("maxCommandForkCount", "65536", "1.20.3", "Maximum number of contexts a command like execute may fork into.", "max_command_forks", 1, null),
            integer("maxEntityCramming", "24", "1.11", "Number of entities that may occupy one block before cramming damage occurs. 0 disables it.", 0, null),
            bool("mobExplosionDropDecay", "true", "1.21", "Whether blocks destroyed by mob explosions (e.g. creepers) lose some of their drops."),
            bool("mobGriefing", "true", "1.8.1", "Whether mobs can alter blocks (creeper craters, enderman, villagers) and pick up items."),
            bool("naturalRegeneration", "true", "1.8.1", "Whether players regenerate health when their hunger is high enough.", "natural_health_regeneration"),
            integer("playersNetherPortalCreativeDelay", "0", "1.19", "Ticks a creative-mode player must stand in a nether portal before teleporting.", 0, null),
            integer("playersNetherPortalDefaultDelay", "80", "1.19", "Ticks a survival/adventure player must stand in a nether portal before teleporting.", 0, null),
            integer("playersSleepingPercentage", "100", "1.17", "Percentage of online players that must sleep to skip the night.", 0, null),
            bool("projectilesCanBreakBlocks", "true", "1.21", "Whether impact projectiles may destroy blocks that are breakable by them."),
            integer("randomTickSpeed", "3", "1.8.1", "Random block ticks per chunk section per game tick (crop growth, leaf decay, fire).", 0, null),
            bool("reducedDebugInfo", "false", "1.8.1", "Whether the F3 debug screen shows reduced information."),
            bool("sendCommandFeedback", "true", "1.8.1", "Whether command output is shown in chat."),
            bool("showDeathMessages", "true", "1.8.1", "Whether death messages appear in chat."),
            integer("snowAccumulationHeight", "1", "1.19", "Maximum number of snow layers that accumulate naturally during snowfall.", "max_snow_accumulation_height", 0, 8),
            integer("spawnChunkRadius", "2", "1.20.5", "Radius in chunks around the world spawn that stays permanently loaded."),
            integer("spawnRadius", "10", "1.8.1", "Size of the square area around spawn where players may first appear or respawn.", "respawn_radius", 0, null),
            bool("spectatorsGenerateChunks", "true", "1.15", "Whether players in spectator mode can generate new chunks."),
            bool("tntExplosionDropDecay", "false", "1.21", "Whether blocks destroyed by TNT explosions lose some of their drops."),
            bool("universalAnger", "false", "1.16", "Whether angered neutral mobs attack any nearby player, not only the one who angered them."),
            bool("waterSourceConversion", "true", "1.19", "Whether flowing water between two sources forms a new water source.")
    );

    public static List<GameRule> all() {
        return RULES;
    }

    public static GameRule find(String name) {
        if (name == null) return null;
        for (GameRule rule : RULES) {
            if (rule.name().equalsIgnoreCase(name)) return rule;
        }
        return null;
    }

    private static GameRule bool(String name, String defaultValue, String since, String description) {
        return new GameRule(name, Type.BOOLEAN, defaultValue, since, description, null, null, List.of());
    }

    private static GameRule bool(String name, String defaultValue, String since, String description, String renamedTo) {
        return new GameRule(name, Type.BOOLEAN, defaultValue, since, description, null, null, List.of(new Alias(renamedTo, false)));
    }

    private static GameRule boolInverted(String name, String defaultValue, String since, String description, String renamedTo) {
        return new GameRule(name, Type.BOOLEAN, defaultValue, since, description, null, null, List.of(new Alias(renamedTo, true)));
    }

    private static GameRule integer(String name, String defaultValue, String since, String description) {
        return new GameRule(name, Type.INTEGER, defaultValue, since, description, null, null, List.of());
    }

    private static GameRule integer(String name, String defaultValue, String since, String description, Integer minimum, Integer maximum) {
        return new GameRule(name, Type.INTEGER, defaultValue, since, description, minimum, maximum, List.of());
    }

    private static GameRule integer(String name, String defaultValue, String since, String description, String renamedTo, Integer minimum, Integer maximum) {
        return new GameRule(name, Type.INTEGER, defaultValue, since, description, minimum, maximum, List.of(new Alias(renamedTo, false)));
    }
}
