package com.github.dawsonvilamaa.nationsandvillagesplugin;

import com.github.dawsonvilamaa.nationsandvillagesplugin.classes.Nation;
import com.github.dawsonvilamaa.nationsandvillagesplugin.classes.NationsChunk;
import com.github.dawsonvilamaa.nationsandvillagesplugin.classes.NationsPlayer;
import com.github.dawsonvilamaa.nationsandvillagesplugin.listeners.InventoryListener;
import com.github.dawsonvilamaa.nationsandvillagesplugin.npcs.Lumberjack;
import com.github.dawsonvilamaa.nationsandvillagesplugin.npcs.Merchant;
import com.github.dawsonvilamaa.nationsandvillagesplugin.npcs.NationsVillager;
import com.github.dawsonvilamaa.nationsandvillagesplugin.listeners.NationsVillagerListener;
import com.github.dawsonvilamaa.nationsandvillagesplugin.listeners.PlayerListener;
import com.github.dawsonvilamaa.nationsandvillagesplugin.listeners.WorldListener;
import net.minecraft.server.v1_16_R3.EntityVillager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftVillager;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

public class Main extends JavaPlugin {
    private PlayerListener playerListener = new PlayerListener(this);
    private NationsVillagerListener nationsVillagerListener = new NationsVillagerListener(this);
    private WorldListener worldListener = new WorldListener(this);
    private InventoryListener inventoryListener = new InventoryListener(this);

    public static Main plugin;
    public static NationsManager nationsManager;
    public PluginManager pm = getServer().getPluginManager();

    @Override
    public void onEnable() {
        plugin = this;
        nationsManager = new NationsManager();

        //register commands
        NAVCommandExecutor commandExecutor = new NAVCommandExecutor(this);
        getCommand("autoclaim").setExecutor(commandExecutor);
        getCommand("autounclaim").setExecutor(commandExecutor);
        getCommand("balance").setExecutor(commandExecutor);
        getCommand("claim").setExecutor(commandExecutor);
        getCommand("demote").setExecutor(commandExecutor);
        getCommand("exile").setExecutor(commandExecutor);
        getCommand("invite").setExecutor(commandExecutor);
        getCommand("nation").setExecutor(commandExecutor);
        getCommand("promote").setExecutor(commandExecutor);
        getCommand("sell").setExecutor(commandExecutor);
        getCommand("unclaim").setExecutor(commandExecutor);

        //register events
        pm.registerEvents(playerListener, this);
        pm.registerEvents(nationsVillagerListener, this);
        pm.registerEvents(worldListener, this);
        pm.registerEvents(inventoryListener, this);

        String savePath = "plugins\\NationsAndVillages\\";
        //create data folder if it doesn't exist
        File dir = new File(savePath);
        if (!dir.exists()) dir.mkdir();

        //create data files if they don't exist
        try {
            File playersFile = new File(savePath + "players.json");
            if (playersFile.createNewFile()) {
                FileWriter writer = new FileWriter(playersFile);
                writer.write("[]");
                writer.close();
            }

            File nationsFile = new File(savePath + "nations.json");
            if (nationsFile.createNewFile()) {
                FileWriter writer = new FileWriter(nationsFile);
                writer.write("[]");
                writer.close();
            }

            File villagersFile = new File(savePath + "villagers.json");
            if (villagersFile.createNewFile()) {
                FileWriter writer = new FileWriter(villagersFile);
                writer.write("[]");
                writer.close();
            }

            File chunksFile = new File(savePath + "chunks.json");
            if (chunksFile.createNewFile()) {
                FileWriter writer = new FileWriter(chunksFile);
                writer.write("[]");
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //load data
        JSONParser parser = new JSONParser();
        try {
            //load players
            JSONArray jsonPlayers = (JSONArray) parser.parse(new FileReader("plugins\\NationsAndVillages\\players.json"));
            Iterator<JSONObject> iterator = jsonPlayers.iterator();
            while (iterator.hasNext()) {
                NationsPlayer player = new NationsPlayer(iterator.next());
                nationsManager.getPlayers().put(player.getUniqueID(), player);
            }

            //load nations
            JSONArray jsonNations = (JSONArray) parser.parse(new FileReader("plugins\\NationsAndVillages\\nations.json"));
            iterator = jsonNations.iterator();
            while (iterator.hasNext())
                nationsManager.addNation(new Nation(iterator.next()));

            //load chunks
            JSONArray jsonChunks = (JSONArray) parser.parse(new FileReader("plugins\\NationsAndVillages\\chunks.json"));
            iterator = jsonChunks.iterator();
            while (iterator.hasNext())
                nationsManager.getChunks().add(new NationsChunk(iterator.next()));

            //load villagers
            JSONArray jsonVillagers = (JSONArray) parser.parse(new FileReader("plugins\\NationsAndVillages\\villagers.json"));
            iterator = jsonVillagers.iterator();
            while (iterator.hasNext()) {
                JSONObject jsonVillager = iterator.next();
                NationsVillager nationsVillager;
                switch (NationsVillager.Job.valueOf(jsonVillager.get("job").toString())) {
                    case MERCHANT:
                        nationsVillager = new Merchant(jsonVillager);
                    break;

                    case LUMBERJACK:
                        nationsVillager = new Lumberjack(jsonVillager);
                    break;

                    default:
                        nationsVillager = new NationsVillager(jsonVillager);
                }
                nationsManager.addVillager(nationsVillager);
            }

            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    for (Entity entity : chunk.getEntities()) {
                        if (entity instanceof CraftVillager) {
                            EntityVillager villager = ((CraftVillager) entity).getHandle();
                            //add new villager if one with that UUID doesn't already exist
                            if (Main.nationsManager.getVillagerByUUID(villager.getUniqueID()) == null) {
                                Main.nationsManager.addVillager(new NationsVillager(villager.getUniqueID()));
                            }
                            NationsVillager nationsVillager = Main.nationsManager.getVillagerByUUID(villager.getUniqueID());
                            NationsChunk nationsChunk = Main.nationsManager.getChunkByCoords(chunk.getX(), chunk.getZ());
                            //add villager to nation if it is in a claimed chunk
                            if (nationsChunk != null) {
                                if (nationsVillager.getNationID() == -1) {
                                    nationsVillager.setNationID(nationsChunk.getNationID());
                                    Main.nationsManager.getNationByID(nationsVillager.getNationID()).incrementPopulation();
                                }
                            }
                            //remove villager from its nation if it is not in a claimed chunk
                            else {
                                if (nationsVillager.getNationID() != -1) {
                                    Main.nationsManager.getNationByID(nationsVillager.getNationID()).decrementPopulation();
                                    nationsVillager.setNationID(-1);
                                }
                            }
                        }
                    }
                }
            }

        } catch(IOException e) {
            getLogger().info("IOException: " + e.getMessage());
        } catch(ParseException ex) {
            getLogger().info("ParseException: " + ex.getMessage());
        }
    }

    @Override
    public void onDisable() {
        //save player data
        JSONArray jsonPlayers = new JSONArray();
        for (NationsPlayer player : nationsManager.getPlayers().values())
            jsonPlayers.add(player.toJSON());

        FileWriter playerFile = null;
        try {
            playerFile = new FileWriter("plugins\\NationsAndVillages\\players.json");
            playerFile.write(jsonPlayers.toJSONString());
        } catch(IOException e) {
            getLogger().info("IOException: " + e.getMessage());
        } finally {
            try {
                playerFile.flush();
                playerFile.close();
            } catch (IOException ex) {
                getLogger().info("IOException: " + ex.getMessage());
            }
        }

        //save nations data
        JSONArray jsonNations = new JSONArray();
        for (Nation nation : nationsManager.getNations().values())
            jsonNations.add(nation.toJSON());

        FileWriter nationsFile = null;
        try {
            nationsFile = new FileWriter("plugins\\NationsAndVillages\\nations.json");
            nationsFile.write(jsonNations.toJSONString());
        } catch (IOException e) {
            getLogger().info("IOException: " + e.getMessage());
        } finally {
            try {
                nationsFile.flush();
                nationsFile.close();
            } catch(IOException e) {
                getLogger().info("IOException: " + e.getMessage());
            }
        }

        //save villager data
        JSONArray jsonVillagers = new JSONArray();
        for (NationsVillager villager : nationsManager.getVillagers().values())
            jsonVillagers.add(villager.toJSON());

        FileWriter villagersFile = null;
        try {
            villagersFile = new FileWriter("plugins\\NationsAndVillages\\villagers.json");
            villagersFile.write(jsonVillagers.toJSONString());
        } catch (IOException e) {
            getLogger().info("IOException: " + e.getMessage());
        } finally {
            try {
                villagersFile.flush();
                villagersFile.close();
            } catch (IOException e) {
                getLogger().info("IOException: " + e.getMessage());
            }
        }

        //save chunk data
        JSONArray jsonChunks = new JSONArray();
        for (NationsChunk chunk : nationsManager.getChunks())
            jsonChunks.add(chunk.toJSON());

        FileWriter chunksFile = null;
        try {
            chunksFile = new FileWriter("plugins\\NationsAndVillages\\chunks.json");
            chunksFile.write(jsonChunks.toJSONString());
        } catch (IOException e) {
            getLogger().info("IOException: " + e.getMessage());
        } finally {
            try {
                chunksFile.flush();
                chunksFile.close();
            } catch(IOException e) {
                getLogger().info("IOException: " + e.getMessage());
            }
        }
    }
}
