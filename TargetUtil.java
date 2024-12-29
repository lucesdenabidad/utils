package datta.dev.plugin.utils;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Utilidad para manejar selectores de entidades al estilo Minecraft
 * Soporta los siguientes selectores:
 * - @a: Todos los jugadores
 * - @p: Jugador más cercano
 * - @r: Jugador aleatorio
 * - @s: Ejecutor del comando (self)
 * - @e: Todas las entidades (actualmente solo jugadores)
 */
public class TargetUtil {

    /**
     * Obtiene una lista de jugadores basada en los selectores proporcionados
     * @param targets Lista de selectores a procesar
     * @param executor Jugador que ejecuta el comando (para @s y referencias de distancia)
     * @return Lista de jugadores que coinciden con los selectores
     */
    public static List<Player> getFromTargets(List<String> targets, Player executor) {
        List<Player> players = new ArrayList<>();

        for (String target : targets) {
            if (target.equals("@s")) {
                if (executor != null) {
                    players.add(executor);
                }
                continue;
            }

            if (target.equals("@r")) {
                List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (!onlinePlayers.isEmpty()) {
                    players.add(onlinePlayers.get(new Random().nextInt(onlinePlayers.size())));
                }
                continue;
            }

            if (target.startsWith("@a[") || target.startsWith("@p[") || target.startsWith("@r[") || target.startsWith("@e[")) {
                boolean isNearest = target.startsWith("@p[");
                boolean isRandom = target.startsWith("@r[");

                String criteria = target.substring(3, target.length() - 1);
                String[] conditions = criteria.split(",");

                List<Player> matchingPlayers = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (matchesConditions(player, conditions, executor)) {
                        matchingPlayers.add(player);
                    }
                }

                if (isNearest && executor != null && !matchingPlayers.isEmpty()) {
                    Player nearest = null;
                    double nearestDistance = Double.MAX_VALUE;

                    for (Player player : matchingPlayers) {
                        if (player == executor) continue;
                        double distance = player.getLocation().distanceSquared(executor.getLocation());
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearest = player;
                        }
                    }

                    if (nearest != null) {
                        players.add(nearest);
                    }
                } else if (isRandom && !matchingPlayers.isEmpty()) {
                    players.add(matchingPlayers.get(new Random().nextInt(matchingPlayers.size())));
                } else {
                    players.addAll(matchingPlayers);
                }
            } else if (target.equals("@a") || target.equals("@e")) {
                players.addAll(Bukkit.getOnlinePlayers());
            } else {
                Player player = Bukkit.getPlayer(target);
                if (player != null && player.isOnline()) {
                    players.add(player);
                }
            }
        }

        return players;
    }

    /**
     * Verifica si un jugador cumple con todas las condiciones especificadas
     */
    public static boolean matchesConditions(Player player, String[] conditions, Player executor) {
        Location referenceLocation = executor != null ? executor.getLocation() : player.getWorld().getSpawnLocation();

        for (String condition : conditions) {
            String[] parts = condition.split("=", 2);
            if (parts.length != 2) continue;

            String key = parts[0];
            String value = parts[1];

            switch (key) {
                case "name":
                    if (!matchesName(player, value)) return false;
                    break;
                case "gamemode":
                    if (!matchesGamemode(player, value)) return false;
                    break;
                case "distance":
                    if (!isWithinDistance(player, value, referenceLocation)) return false;
                    break;
                case "team":
                    if (!matchesTeam(player, value)) return false;
                    break;
                case "tag":
                    if (!matchesTag(player, value)) return false;
                    break;
                case "scores":
                    if (!matchesScores(player, value)) return false;
                    break;
                case "level":
                    if (!matchesLevel(player, value)) return false;
                    break;
                case "x":
                case "y":
                case "z":
                    if (!matchesCoordinate(player, key, value)) return false;
                    break;
            }
        }
        return true;
    }

    /**
     * Verifica si el nombre del jugador coincide con el criterio
     */
    public static boolean matchesName(Player player, String value) {
        if (value.startsWith("!")) {
            return !player.getName().equalsIgnoreCase(value.substring(1));
        }
        return player.getName().equalsIgnoreCase(value);
    }

    /**
     * Verifica si el modo de juego del jugador coincide
     */
    public static boolean matchesGamemode(Player player, String value) {
        try {
            GameMode gameMode = GameMode.valueOf(value.toUpperCase());
            return player.getGameMode() == gameMode;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Verifica si el jugador está dentro del rango de distancia especificado
     */
    public static boolean isWithinDistance(Player player, String range, Location reference) {
        double distance = player.getLocation().distance(reference);

        if (range.startsWith("..")) {
            double max = Double.parseDouble(range.substring(2));
            return distance <= max;
        } else if (range.contains("..")) {
            String[] parts = range.split("\\.\\.");
            double min = Double.parseDouble(parts[0]);
            double max = Double.parseDouble(parts[1]);
            return distance >= min && distance <= max;
        } else {
            double exact = Double.parseDouble(range);
            return Math.abs(distance - exact) < 0.1;
        }
    }

    /**
     * Verifica si el jugador pertenece al equipo especificado
     */
    public static boolean matchesTeam(Player player, String value) {
        if (value.startsWith("!")) {
            String teamName = value.substring(1);
            return player.getScoreboard().getEntryTeam(player.getName()) == null ||
                    !Objects.requireNonNull(player.getScoreboard().getEntryTeam(player.getName())).getName().equals(teamName);
        }
        return player.getScoreboard().getEntryTeam(player.getName()) != null &&
                Objects.requireNonNull(player.getScoreboard().getEntryTeam(player.getName())).getName().equals(value);
    }

    /**
     * Verifica si el jugador tiene el tag especificado
     */
    public static boolean matchesTag(Player player, String value) {
        if (value.startsWith("!")) {
            return !player.getScoreboardTags().contains(value.substring(1));
        }
        return player.getScoreboardTags().contains(value);
    }

    /**
     * Verifica si el jugador cumple con los criterios de puntaje
     */
    public static boolean matchesScores(Player player, String value) {
        // Removemos los {} del valor
        value = value.substring(1, value.length() - 1);
        String[] scores = value.split(",");

        for (String score : scores) {
            String[] scoreParts = score.split("=");
            if (scoreParts.length != 2) continue;

            String objective = scoreParts[0];
            String range = scoreParts[1];

            int playerScore = Objects.requireNonNull(Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard()
                            .getObjective(objective))
                    .getScore(player.getName())
                    .getScore();

            if (!isInRange(playerScore, range)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verifica si el nivel del jugador cumple con el criterio
     */
    public static boolean matchesLevel(Player player, String value) {
        return isInRange(player.getLevel(), value);
    }

    /**
     * Verifica si una coordenada específica coincide
     */
    public static boolean matchesCoordinate(Player player, String coordinate, String value) {
        int playerCoord;
        switch (coordinate) {
            case "x":
                playerCoord = player.getLocation().getBlockX();
                break;
            case "y":
                playerCoord = player.getLocation().getBlockY();
                break;
            case "z":
                playerCoord = player.getLocation().getBlockZ();
                break;
            default:
                return false;
        }
        return isInRange(playerCoord, value);
    }

    /**
     * Utilidad para verificar si un número está dentro de un rango especificado
     */
    public static boolean isInRange(int value, String range) {
        if (range.startsWith("..")) {
            int max = Integer.parseInt(range.substring(2));
            return value <= max;
        } else if (range.contains("..")) {
            String[] parts = range.split("\\.\\.");
            int min = Integer.parseInt(parts[0]);
            int max = Integer.parseInt(parts[1]);
            return value >= min && value <= max;
        } else {
            int exact = Integer.parseInt(range);
            return value == exact;
        }
    }
}
