package me.emiliovirtual.tombyard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class TombYardMain extends JavaPlugin implements Listener{
	PluginDescriptionFile pdfFile = this.getDescription();
	public final ConsoleCommandSender console = Bukkit.getConsoleSender();
	private final String PT = "[TombYard] ";

	private HashMap<Player, Location> playerDeathLocation = new HashMap<Player, Location>();
	private Map<String, String> translation = new HashMap<>();
	
	File configFile;
    File translationsFile;
    File tombyardsFile;
	FileConfiguration config;
    FileConfiguration trans;
    FileConfiguration tombyards;
	
	@Override
	public void onEnable(){
		getServer().getPluginManager().registerEvents(this, this);

        configFile = new File(getDataFolder(), "config.yml");   
        translationsFile = new File(getDataFolder(), "translations.yml");   
        tombyardsFile = new File(getDataFolder(), "tombyards.yml");     
 
        try {
            firstRun();
        } catch (Exception e) {
            e.printStackTrace();
        }

        config = new YamlConfiguration();
        trans = new YamlConfiguration();
        tombyards = new YamlConfiguration();
        loadYamls();
		
		loadTranslations();
	}
	
	@Override
	public void onDisable(){
		saveYamls();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("tombyard")) { 
			if (!(sender instanceof Player)) {
				sender.sendMessage(PT + "This command can only be run by a player.");
			} else {
				Player p = (Player) sender;
				
				if (args.length > 1){
					if(args[0].equalsIgnoreCase("add") && p.hasPermission("tombyard.*")){
						newTombYard(p, args[1]);
						p.sendMessage(PT + translation.get("TY_Added").replace("<name>", ChatColor.GOLD + args[1] + ChatColor.WHITE));
					} else if (args[0].equalsIgnoreCase("tp") && p.hasPermission("tombyard.tp")){
						if(isValidTombYard(args[1], p.getWorld().getName())){
							p.teleport(getTombYardLocation(args[1], p.getWorld().getName()));
							p.sendMessage(PT + translation.get("TY_TeleportatedTo").replace("<name>", ChatColor.GOLD + args[1] + ChatColor.WHITE));
						} else
							p.sendMessage(PT + translation.get("TY_NotFound").replace("<name>", ChatColor.GOLD + args[1] + ChatColor.WHITE));
					} else if (args[0].equalsIgnoreCase("delete") && p.hasPermission("tombyard.*")){
						if(isValidTombYard(args[1], p.getWorld().getName())){
							deleteTombYardByName(args[1], p.getWorld().getName());
							p.sendMessage(PT + translation.get("TY_Deleted").replace("<name>", ChatColor.GOLD + args[1] + ChatColor.WHITE));
						} else
							p.sendMessage(PT + translation.get("TY_NotFound").replace("<name>", ChatColor.GOLD + args[1] + ChatColor.WHITE));
					}
				} else if (args.length > 0){
					if (args[0].equalsIgnoreCase("tpnear") && p.hasPermission("tombyard.tp")){
						String NearestTombYard = searchNearestTombYard(p, p.getWorld().getName());
						if(NearestTombYard != ""){
							p.teleport(getTombYardLocation(NearestTombYard, p.getWorld().getName()));
							p.sendMessage(PT + translation.get("TY_TeleportatedTo").replace("<name>", ChatColor.GOLD + NearestTombYard + ChatColor.WHITE));
						} else {
							p.sendMessage(PT + translation.get("TY_NotFoundAny"));
						}
					} else if (args[0].equalsIgnoreCase("deletenear") && p.hasPermission("tombyard.*")){
						String NearestTombYard = searchNearestTombYard(p, p.getWorld().getName());
						if(NearestTombYard != ""){
							if(deleteTombYardByName(NearestTombYard, p.getWorld().getName()))
								p.sendMessage(PT + translation.get("TY_Deleted").replace("<name>", ChatColor.GOLD + NearestTombYard + ChatColor.WHITE));
							else
								p.sendMessage(PT + translation.get("TY_NotFoundAny"));
						} else {
							p.sendMessage(PT + translation.get("TY_NotFoundAny"));
						}
					} else if (args[0].equalsIgnoreCase("list") && p.hasPermission("tombyard.*")){
						String NearestTombYard = searchNearestTombYard(p, p.getWorld().getName());
						if(NearestTombYard != ""){
							showTombYardList(p);
						} else
							p.sendMessage(PT + translation.get("TY_NotFoundAny"));
					} else if (args[0].equalsIgnoreCase("reload") && p.hasPermission("tombyard.*")){
						pluginReload();
						p.sendMessage(PT + translation.get("TY_Reloaded"));
					}
				} else if (p.hasPermission("tombyard.*")){
					p.sendMessage(PT + ChatColor.GREEN + translation.get("TY_Help") + ":");
					p.sendMessage(ChatColor.GOLD + "/tombyard " + ChatColor.WHITE + translation.get("TY_CommMain"));
					p.sendMessage(ChatColor.GOLD + "/tombyard add <name> " + ChatColor.WHITE + translation.get("TY_CommAdd"));
					p.sendMessage(ChatColor.GOLD + "/tombyard delete <name> " + ChatColor.WHITE + translation.get("TY_CommDelete"));
					p.sendMessage(ChatColor.GOLD + "/tombyard tp <name> " + ChatColor.WHITE + translation.get("TY_CommTp"));
					p.sendMessage(ChatColor.GOLD + "/tombyard deletenear " + ChatColor.WHITE + translation.get("TY_CommDeleteNear"));
					p.sendMessage(ChatColor.GOLD + "/tombyard tpnear " + ChatColor.WHITE + translation.get("TY_CommTpNear"));
					p.sendMessage(ChatColor.GOLD + "/tombyard list " + ChatColor.WHITE + translation.get("TY_CommList"));
					p.sendMessage(ChatColor.GOLD + "/tombyard reload " + ChatColor.WHITE + translation.get("TY_CommReload"));
				}
			}
			return true;
		}
		return false;
	}
	
	@EventHandler(priority=EventPriority.NORMAL)
	public void onPlayerDeath(PlayerDeathEvent e){
	    Player p = e.getEntity().getPlayer();
	    playerDeathLocation.put(p, p.getLocation());
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent e) {
		Player p = e.getPlayer();
		
		if (p.hasPermission("tombyard.bypass")) return;
		
		String NearestTombYard = searchNearestTombYard(p, p.getWorld().getName());
	    e.setRespawnLocation(getTombYardLocation(NearestTombYard, p.getWorld().getName()));
	    if (config.getBoolean("show_message_on_respawn"))
	    	p.sendMessage(PT + translation.get("TY_RespawnedOn").replace("<name>", ChatColor.GOLD + NearestTombYard + ChatColor.WHITE));
	} 
	
	private void firstRun() throws Exception {
		if(!configFile.exists()){                        
			configFile.getParentFile().mkdirs();         
			copy(getResource("config.yml"), configFile); 
		}
		if(!translationsFile.exists()){
			translationsFile.getParentFile().mkdirs();
			copy(getResource("translations.yml"), translationsFile);
		}
		if(!tombyardsFile.exists()){
			tombyardsFile.getParentFile().mkdirs();
			copy(getResource("tombyards.yml"), tombyardsFile);
		}
	}
	 
	private void copy(InputStream in, File file) {
		try {
			OutputStream out = new FileOutputStream(file);
			byte[] buf = new byte[1024];
			int len;
			while((len=in.read(buf))>0){
				out.write(buf,0,len);
			}
			out.close();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loadYamls() {
		try {
			config.load(configFile);
			trans.load(translationsFile);
			tombyards.load(tombyardsFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	 
	public void saveYamls() {
		try {
			config.save(configFile);
			trans.save(translationsFile);
			tombyards.save(tombyardsFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void loadTranslations(){
		ConfigurationSection configSection = trans.getConfigurationSection("");
		
		for (String key : configSection.getKeys(false))
			translation.put(key, trans.getString(key));
	}
	
	private void pluginReload(){
		loadYamls();
		loadTranslations();
		getLogger().info(translation.get("TY_Reloaded"));
	}
	
	private void showTombYardList(Player p){	
		ConfigurationSection configSection = tombyards.getConfigurationSection("Tombyards." + p.getWorld().getName());
	    
		p.sendMessage(PT + translation.get("TY_List") + ":");
	    for(String key : configSection.getKeys(false))
	    	p.sendMessage("- " + ChatColor.GOLD + key);
	}
	
	private void newTombYard(Player p, String name){    
		Location loc = p.getLocation();
		String TombYardName;
		
		String world = p.getWorld().getName();

		TombYardName = "Tombyards." + world + "." + name;
		
		tombyards.set(TombYardName + ".X", Double.toString(loc.getX()));
		tombyards.set(TombYardName + ".Y", Double.toString(loc.getY()));
		tombyards.set(TombYardName + ".Z", Double.toString(loc.getZ()));

		try {
			tombyards.save(tombyardsFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Location getTombYardLocation(String name, String world){
		Location loc = getServer().getWorld(world).getBlockAt(0, 0, 0).getLocation();
		loc.setX(Double.parseDouble(tombyards.getString("Tombyards." + world + "." + name + ".X")));
		loc.setY(Double.parseDouble(tombyards.getString("Tombyards." + world + "." + name + ".Y")));
		loc.setZ(Double.parseDouble(tombyards.getString("Tombyards." + world + "." + name + ".Z"))); 
    	
    	return loc;
	}
	
	private String searchNearestTombYard(Player p, String world){
		String nearestTombYardName;
	    
	    try {
			ConfigurationSection configSection = tombyards
					.getConfigurationSection("Tombyards." + world);
			Location nearestLoc = getServer().getWorld(p.getWorld().getName())
					.getBlockAt(10000000, 100, 10000000).getLocation();
			Location currentLoc = getServer().getWorld(p.getWorld().getName())
					.getBlockAt(0, 0, 0).getLocation();
			nearestTombYardName = "";
			for (String key : configSection.getKeys(false)) {
				currentLoc.setX(Double.parseDouble(tombyards
						.getString("Tombyards." + world + "." + key + ".X")));
				currentLoc.setY(Double.parseDouble(tombyards
						.getString("Tombyards." + world + "." + key + ".Y")));
				currentLoc.setZ(Double.parseDouble(tombyards
						.getString("Tombyards." + world + "." + key + ".Z")));

				if (p.getLocation().distance(currentLoc) < p.getLocation()
						.distance(nearestLoc)) {
					nearestLoc = currentLoc.clone();
					nearestTombYardName = key;
				}
			}
		} catch (Exception e) {
			nearestTombYardName = "";
		}
		return nearestTombYardName;
	}
	
	private boolean isValidTombYard(String name, String world){
	    ConfigurationSection configSection = tombyards.getConfigurationSection("Tombyards." + world + "." + name);
	    
	    if(configSection.getKeys(false).size() > 0) return true;
	    else return false;
	}
	
	private boolean deleteTombYardByName(String name, String world){
	    ConfigurationSection configSection = tombyards.getConfigurationSection("Tombyards." + world + "." + name);
	    
	    if(configSection.getKeys(false).size() > 0){
	    	tombyards.set("Tombyards." + world + "." + name, null);
	    	try {
	    		tombyards.save(tombyardsFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
	    	return true;
	    } else {
	    	return false;
	    }
	}
}
