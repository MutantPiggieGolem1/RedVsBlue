package me.stephenminer.redvblue.commands;

import me.stephenminer.redvblue.RedBlue;
import me.stephenminer.redvblue.chests.TableGui;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class LootChest implements CommandExecutor, TabCompleter {
    private final RedBlue plugin;
    public static HashMap<UUID, TableGui> editing = new HashMap<>();
    public LootChest (RedBlue plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (sender instanceof Player player){
            int size = args.length;
            if (size < 2){
                player.sendMessage(ChatColor.RED + "Not enough arguments!");
                return false;
            }
            String sub = args[0];
            String id = args[1];
            if (size == 2){
                if (sub.equalsIgnoreCase("give")){
                    if (chestExists(id)){
                        player.getInventory().addItem(chestItem(id));
                        player.sendMessage(ChatColor.GREEN + "If you had inventory space, the chest was added to your inventory!");
                        return true;
                    }
                }
            }
            if (size == 3) {
                if (sub.equalsIgnoreCase("create")) {
                    String sMat = args[2];
                    if (!chestExists(id)) {
                        Material mat = Material.matchMaterial(sMat);
                        if (mat != null) {
                            createLootChest(id, mat);
                            player.sendMessage(ChatColor.GREEN + "Created new lootchest with id " + id + " and type " + sMat);
                            return true;
                        } else player.sendMessage(ChatColor.RED + "Input material " + sMat + " doesn't exist!");
                    } else
                        player.sendMessage(ChatColor.RED + "This id already exists! Try editing the one that exists!");
                }
                if (sub.equalsIgnoreCase("edit")) {
                    if (chestExists(id)) {
                        int tableNum = Integer.parseInt(args[2]);
                        TableGui gui = new TableGui(plugin, id);
                        Inventory inv = gui.loadTable(tableNum);
                        player.openInventory(inv);
                        editing.put(player.getUniqueId(), gui);
                        player.sendMessage(ChatColor.GREEN + "Opening loot table #" + tableNum + " for chest " + id);
                        return true;
                    } else player.sendMessage(ChatColor.RED + "The inputted id isn't in usage!");
                }
                if (sub.equalsIgnoreCase("setActive")){
                    if (chestExists(id)){
                        String mode = args[2];
                        boolean postWall = mode.equalsIgnoreCase("post-wall");
                        setActiveMode(id, postWall);
                        if (postWall) player.sendMessage(ChatColor.GREEN + "This chest-type will fill itself after the wall falls");
                        else player.sendMessage(ChatColor.GREEN + "This chest-type will fill itself when the game starts");
                        return true;
                    }else player.sendMessage(ChatColor.RED + "The inputted loot-id isn't in usage!");
                }
            }
        }else sender.sendMessage(ChatColor.RED + "Ultimately, only players can use this command!");
        return false;
    }

    private boolean chestExists(String id){
        return plugin.tables.getConfig().contains("tables." + id);
    }
    private void createLootChest(String id, Material type){
        String path = "tables." + id;
        plugin.tables.getConfig().set(path + ".type", type.name());
        plugin.tables.saveConfig();
    }
    private void setActiveMode(String id, boolean postWall){
        plugin.tables.getConfig().set("tables." + id + ".post-wall",postWall);
        plugin.tables.saveConfig();
    }


    private ItemStack chestItem(String id){
        String path = "tables." + id;
        String sMat = plugin.tables.getConfig().getString(path + ".type");
        try {
            Material mat = Material.matchMaterial(sMat);
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Game Chest [" + id + "]");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Place in an arena to add it there!");
            lore.add(ChatColor.BLACK + "lc:" + id);
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }catch (Exception e){
            plugin.getLogger().warning("Something went wrong grabbing material entry for chest " + id);
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        int size = args.length;
        if (size == 1) return subCmds(args[0]);
        if (size == 2) {
            if (args[0].equalsIgnoreCase("create")) return namePlaceholder();
            else return ids(args[1]);
        }
        if (size == 3){
            if (args[0].equalsIgnoreCase("create")) return materials(args[2]);
            if (args[0].equalsIgnoreCase("edit")) return namePlaceholder();
        }
        return null;
    }

    private List<String> ids(String match){
        Set<String> ids = plugin.tables.getConfig().getConfigurationSection("tables").getKeys(false);
        return plugin.filter(ids, match);
    }

    private List<String> subCmds(String match){
        List<String> subs = new ArrayList<>();
        subs.add("create");
        subs.add("edit");
        subs.add("give");
        subs.add("setActive");
        return plugin.filter(subs, match);
    }
    private List<String> activeModes(String match){
        List<String> options = new ArrayList<>();
        options.add("pre-wall");
        options.add("post-wall");
        return plugin.filter(options,  match);
    }

    private List<String> namePlaceholder(){
        List<String> name = new ArrayList<>();
        name.add("[your-id-here");
        return name;
    }
    private List<String> tableNum(){
        List<String> num = new ArrayList<>();
        num.add("[loot-table-number-here]");
        return num;
    }
    private List<String> materials(String match){
        List<String> sMats = new ArrayList<>();
        for (Material mat : Material.values()){
            sMats.add(mat.name());
        }
        return plugin.filter(sMats, match);
    }



}
