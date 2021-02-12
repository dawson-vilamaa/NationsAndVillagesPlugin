package com.github.dawsonvilamaa.nationsandvillagesplugin.npcs;

import net.minecraft.server.v1_16_R3.ChatComponentText;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftVillager;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.util.Consumer;
import org.json.simple.JSONObject;

import java.util.UUID;

public class NationsVillager {
    private String name;
    private UUID uuid;
    private int nationID;
    private Jobs job;
    private Consumer<PlayerInteractEntityEvent> onClick;

    public enum Jobs {
      NONE,
        MERCHANT,
        MINER,
        FARMER,
        LUMBERJACK,
        GUARD
    };

    /**
     * Creates a NationsVillager data class and attaches it to an entity in the world
     * @param uuid
     */
    public NationsVillager(UUID uuid) {
        this.name = "Villager";
        this.uuid = uuid;
        this.nationID = -1;
        this.job = Jobs.NONE;
        this.onClick = null;
    }

    /**
     * @param jsonVillager
     */
    public NationsVillager(JSONObject jsonVillager) {
        this.uuid = UUID.fromString(jsonVillager.get("uuid").toString());
        this.name = jsonVillager.get("name").toString();
        this.nationID = Integer.parseInt(jsonVillager.get("nationID").toString());
        this.job = Jobs.valueOf(jsonVillager.get("job").toString());
        this.onClick = null;
    }

    /**
     * @return uuid
     */
    public UUID getUniqueID() {
        return this.uuid;
    }

    /**
     * @return name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name
     */
    public void setName(String name) {
        ((CraftVillager) Bukkit.getEntity(this.uuid)).getHandle().setCustomName(new ChatComponentText(name));
        this.name = name;
    }

    /**
     * @return nationID
     */
    public int getNationID() {
        return this.nationID;
    }

    /**
     * @param nationID
     */
    public void setNationID(int nationID) {
        this.nationID = nationID;
    }

    /**
     * @return job
     */
    public Jobs getJob() {
        return this.job;
    }

    /**
     * @param job
     */
    public void setJob(Jobs job) {
        this.job = job;
    }

    /**
     * @param e
     */
    public void onClick(PlayerInteractEntityEvent e) {
        if (this.onClick != null)
            this.onClick.accept(e);
    }

    /**
     * @param consumer
     */
    public void setOnClick(Consumer<PlayerInteractEntityEvent> consumer) {
        this.onClick = consumer;
    }

    /**
     * @return onClick
     */
    public Consumer<PlayerInteractEntityEvent> getOnClick() {
        return this.onClick;
    }

    /**
     * Send a message from this villager to a specified player
     * @param player
     * @param message
     */
    public void speakToPlayer(Player player, String message) {
        player.sendMessage("<" + name + "> " + message);
    }

    /**
     * Sends a message to all players within 30 blocks
     * @param message
     */
    public void shout(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (Bukkit.getEntity(this.uuid).getLocation().distance(player.getLocation()) <= 30) speakToPlayer(player, message);
        }
    }

    public JSONObject toJSON() {
        JSONObject jsonVillager = new JSONObject();
        jsonVillager.put("uuid", this.uuid.toString());
        jsonVillager.put("name", this.name);
        jsonVillager.put("nationID", String.valueOf(this.nationID));
        jsonVillager.put("job", this.job.toString());
        return jsonVillager;
    }
}