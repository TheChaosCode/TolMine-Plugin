package me.TheChaosCode.main;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class cancelblocks
  extends JavaPlugin
  implements Listener
{
  private HashMap<Block, Material> tasks = new HashMap<Block, Material>();
  WorldGuardPlugin worldguard;
  
  public void onEnable()
  {
    getServer().getPluginManager().registerEvents(this, this);
    if (!new File(getDataFolder(), "config.yml").exists()) {
      saveResource("config.yml", true);
    }
    this.worldguard = getWorldGuard();
    if (this.worldguard == null) {
      getServer().getPluginManager().disablePlugin(this);
    }
    System.out.println("TolEiland Enabled!");
  }
  
  public void onDisable()
  {
    for (Block b : this.tasks.keySet()) {
      b.setType((Material)this.tasks.get(b));
    }
    this.tasks.clear();
    Bukkit.getScheduler().cancelAllTasks();
  }
  
  private WorldGuardPlugin getWorldGuard()
  {
    Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
    if ((plugin == null) || (!(plugin instanceof WorldGuardPlugin))) {
      return null;
    }
    return (WorldGuardPlugin)plugin;
  }
  
  public boolean isInMine(Location loc)
  {
    RegionManager regionManager = this.worldguard.getRegionManager(loc.getWorld());
    ApplicableRegionSet set = regionManager.getApplicableRegions(loc);
    for (ProtectedRegion region : set)
    {
      String id = region.getId();
      if ((getConfig().get("regions") != null) &&
        (new ArrayList<Object>(getConfig().getStringList("regions")).contains(id))) {
        return true;
      }
    }
    return false;
  }
  
@SuppressWarnings("deprecation")
@EventHandler
  public void onBlockBreak(BlockBreakEvent e)
  {
    if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
      return;
    }
    if (!isInMine(e.getBlock().getLocation())) {
      return;
    }
    Player p = e.getPlayer();
    final Block b = e.getBlock();
    final Material mat = b.getType();
    if (!mat.toString().contains("ORE")) {
      return;
    }
    if (mat == Material.GLOWING_REDSTONE_ORE) {
      p.getInventory().addItem(new ItemStack[] { new ItemStack(Material.REDSTONE_ORE) });
    } else {
      p.getInventory().addItem(new ItemStack[] { new ItemStack(mat) });
    }
    p.giveExp(e.getExpToDrop());
    
    e.setCancelled(true);
    b.setType(Material.BEDROCK);
    
    e.getPlayer().sendBlockChange(b.getLocation(), Material.DIAMOND_BLOCK.getId(), (byte)0);
    
    long respawn = 1200L;
    String type = mat.toString();
    if (mat == Material.GLOWING_REDSTONE_ORE) {
      type = "REDSTONE_ORE";
    }
    if (getConfig().get("respawn." + type.toLowerCase()) != null) {
      respawn = getConfig().getLong("respawn." + type.toLowerCase()) * 20L;
    }
    Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable()
    {
      public void run()
      {
        b.setType(mat);
        cancelblocks.this.tasks.remove(b);
      }
    }, respawn);
    
    this.tasks.put(b, mat);
  }
  
  @EventHandler
  public void onBlockPlace(BlockPlaceEvent e)
  {
    if ((isInMine(e.getBlock().getLocation())) && (e.getPlayer().isOp())) {
      e.setCancelled(true);
      e.getPlayer().sendMessage(ChatColor.RED + " You dont have the right permission");
    }
  }
  
  @EventHandler
  public void onCommand(PlayerCommandPreprocessEvent e)
  {
    if (((e.getMessage().toLowerCase().startsWith("/mine"))))
    {
      e.setCancelled(true);
      String[] args = e.getMessage().split(" ");
      if ((args.length == 3) && (args[1].equalsIgnoreCase("define")))
      {
        String id = args[2];
        
        RegionManager regionManager = this.worldguard.getRegionManager(e.getPlayer().getWorld());
        if (!new ArrayList<String>(Arrays.asList(regionManager.getRegions().keySet().toArray(new String[regionManager.getRegions().size()]))).contains(id))
        {
          e.getPlayer().sendMessage(ChatColor.RED + " §f[§6IRLMC§f] --> Sorry this region doesn niet excist!");
          return;
        }
        ArrayList<String> list = new ArrayList<String>();
        if (getConfig().get("regions") != null) {
          list = new ArrayList<String>(getConfig().getStringList("regions"));
        }
        if (!list.contains(id)) {
          list.add(id);
        }
        getConfig().set("regions", list);
        saveConfig();
        
        e.getPlayer().sendMessage(ChatColor.GREEN + "Defined '" + id + "' as a region!");
      }
      else
      {
        e.getPlayer().sendMessage(ChatColor.RED + "§f[§6IRLMC§f] --> Usage: §d/mine define <region>");
      }
    }
  }
}
