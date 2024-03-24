package me.stephenminer.redvblue;

import me.stephenminer.redvblue.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ArenaGui {
    private final RedBlue plugin;
    private final Inventory inv;
    public ArenaGui(RedBlue plugin){
        this.plugin = plugin;
        inv = Bukkit.createInventory(null, 54, ChatColor.AQUA + "Arena Selector");
        init();
    }

    private void init(){
        ItemStack filler = filler();
        for (int i = 45; i < 54; i++){
            inv.setItem(i, filler);
        }
    }




    private void update(){
        new BukkitRunnable(){
            @Override
            public void run(){
                if (inv.getViewers().isEmpty()) {
                    this.cancel();
                    return;
                }else{
                    populate();
                }
            }
        }.runTaskTimer(plugin,0,100);
    }

    private void populate(){
        inv.clear();
        init();
        Set<String> section = plugin.arenas.getConfig().getConfigurationSection("arenas").getKeys(false);
        for (String id : section){
            inv.addItem(arenaIcon(id));
        }
    }




    public void handleInteract(InventoryClickEvent event){
        if(inv.equals(event.getView().getTopInventory()) && event.getView().getTitle().equalsIgnoreCase(ChatColor.AQUA + "Arena Selector")){
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return;
            String arenaId = item.getItemMeta().getLore().stream().filter(l->l.contains("arena:")).findAny().orElse(null);
            if (arenaId == null) return;
            arenaId = ChatColor.stripColor(arenaId.replace("arena:",""));
            Bukkit.dispatchCommand(event.getWhoClicked(),"joinrvb " + arenaId);
        }
    }



    private ItemStack arenaIcon(String arenaId){
        Material mat = Material.OAK_SIGN;
        Arena arena = fromId(arenaId);
        String players =  arena == null ?ChatColor.YELLOW +  "0" : (ChatColor.YELLOW + "" + arena.getPlayers().size());
        players+=" players";
        String startText = arena == null || !arena.isStarted() ? ChatColor.GREEN + "Waiting to start" : ChatColor.YELLOW + "Game Started";

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + arenaId);
        List<String> lore = new ArrayList<>();
        lore.add(players);
        lore.add(startText);
        lore.add(ChatColor.BLACK + "arena:" + arenaId);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    private ItemStack filler(){
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    public void display(Player player){
        player.openInventory(inv);
        update();
    }

    private Arena fromId(String id){
        return Arena.arenas.stream().filter(arena->arena.getId().equalsIgnoreCase(id)).findAny().orElse(null);
    }

    public Inventory getInv(){ return inv; }
}
