package fr.kaelis.kaeliscore.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a KaelisCore player with all their data
 */
public class KaelisPlayer {

    private static final Gson GSON = new GsonBuilder().create();

    private final UUID uuid;
    private String username;
    
    // Economy
    private double balance;
    
    // Timestamps
    private long firstJoin;
    private long lastJoin;
    private long playTime;
    private transient long sessionStart;
    
    // Statistics
    private final Map<String, Double> statistics = new HashMap<>();
    
    // Custom data storage
    private final Map<String, Object> customData = new HashMap<>();

    public KaelisPlayer(UUID uuid) {
        this.uuid = uuid;
        this.balance = 0;
        this.firstJoin = 0;
        this.lastJoin = 0;
        this.playTime = 0;
    }

    // UUID
    public UUID getUuid() {
        return uuid;
    }

    // Username
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // Balance
    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = Math.max(0, balance);
    }

    public void addBalance(double amount) {
        this.balance = Math.max(0, this.balance + amount);
    }

    public void removeBalance(double amount) {
        this.balance = Math.max(0, this.balance - amount);
    }

    public boolean hasBalance(double amount) {
        return this.balance >= amount;
    }

    // Timestamps
    public long getFirstJoin() {
        return firstJoin;
    }

    public void setFirstJoin(long firstJoin) {
        this.firstJoin = firstJoin;
    }

    public long getLastJoin() {
        return lastJoin;
    }

    public void setLastJoin(long lastJoin) {
        this.lastJoin = lastJoin;
    }

    public long getPlayTime() {
        return playTime;
    }

    public void setPlayTime(long playTime) {
        this.playTime = playTime;
    }

    public void addPlayTime(long time) {
        this.playTime += time;
    }

    public long getSessionStart() {
        return sessionStart;
    }

    public void setSessionStart(long sessionStart) {
        this.sessionStart = sessionStart;
    }

    /**
     * Get play time formatted as string
     */
    public String getPlayTimeFormatted() {
        long seconds = playTime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dj %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else {
            return String.format("%dm", minutes);
        }
    }

    // Statistics
    public double getStat(String key) {
        return statistics.getOrDefault(key, 0.0);
    }

    public void setStat(String key, double value) {
        statistics.put(key, value);
    }

    public void addStat(String key, double value) {
        statistics.merge(key, value, Double::sum);
    }

    public Map<String, Double> getStatistics() {
        return statistics;
    }

    // Custom data
    public void setData(String key, Object value) {
        customData.put(key, value);
    }

    public Object getData(String key) {
        return customData.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(String key, Class<T> type) {
        Object value = customData.get(key);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    public <T> T getData(String key, T defaultValue) {
        Object value = customData.get(key);
        if (defaultValue != null && defaultValue.getClass().isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }

    public boolean hasData(String key) {
        return customData.containsKey(key);
    }

    public void removeData(String key) {
        customData.remove(key);
    }

    // Serialization
    public String toJson() {
        JsonObject json = new JsonObject();
        
        // Statistics
        JsonObject statsJson = new JsonObject();
        for (Map.Entry<String, Double> entry : statistics.entrySet()) {
            statsJson.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("statistics", statsJson);
        
        // Custom data (simple types only)
        JsonObject dataJson = new JsonObject();
        for (Map.Entry<String, Object> entry : customData.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String s) {
                dataJson.addProperty(entry.getKey(), s);
            } else if (value instanceof Number n) {
                dataJson.addProperty(entry.getKey(), n);
            } else if (value instanceof Boolean b) {
                dataJson.addProperty(entry.getKey(), b);
            }
        }
        json.add("customData", dataJson);
        
        return GSON.toJson(json);
    }

    public void loadFromJson(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) return;
        
        try {
            JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
            
            // Load statistics
            if (json.has("statistics")) {
                JsonObject statsJson = json.getAsJsonObject("statistics");
                for (String key : statsJson.keySet()) {
                    statistics.put(key, statsJson.get(key).getAsDouble());
                }
            }
            
            // Load custom data
            if (json.has("customData")) {
                JsonObject dataJson = json.getAsJsonObject("customData");
                for (String key : dataJson.keySet()) {
                    var element = dataJson.get(key);
                    if (element.isJsonPrimitive()) {
                        var primitive = element.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            customData.put(key, primitive.getAsString());
                        } else if (primitive.isNumber()) {
                            customData.put(key, primitive.getAsDouble());
                        } else if (primitive.isBoolean()) {
                            customData.put(key, primitive.getAsBoolean());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Invalid JSON, ignore
        }
    }
}
