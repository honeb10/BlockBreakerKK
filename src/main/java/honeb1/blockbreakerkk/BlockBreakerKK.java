package honeb1.blockbreakerkk;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Directional;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BlockBreakerKK extends JavaPlugin implements Listener {
    Configuration data;
    ArrayList<Location> bbLocations = new ArrayList<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);
        //既存のBB読み込み
        data = this.getConfig();
        ArrayList<Location> locs  = new ArrayList<>();
        bbLocations = (ArrayList<Location>)data.getList("Locations");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        //保存
        data.set("Locations",bbLocations);
        saveConfig();
    }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
        if(!(sender instanceof Player)){
            sender.sendMessage("ゲーム内でのみ実行可能です。");
            return false;
        }
        Player p = (Player) sender;
        Block b = ((Player) sender).getTargetBlock(5);
        if(b == null || b.getType() != Material.DISPENSER){
            sender.sendMessage(ChatColor.RED + "ディスペンサーが見つかりません。");
            return  false;
        }else if(b.getType() != Material.DISPENSER){
            sender.sendMessage(ChatColor.RED + "ディスペンサーが見つかりません。");
            return  false;
        }
        if(bbLocations.contains(b.getLocation())){
            sender.sendMessage("既に登録済みです。");
            return false;
        }
        //ディスペンサー確定
        Dispenser dispenser = (Dispenser)b.getState();
        Inventory dInv = dispenser.getInventory();
        boolean hasOnlyPickaxe = false;
        for(int i=0;i<dInv.getSize();i++){
            ItemStack itemStack = dInv.getItem(i);
            if(itemStack == null){
                continue;
            }else if( !isPickaxe(itemStack) ) {//ツルハシ以外
                hasOnlyPickaxe = false;
                break;//使用不可
            }else if(hasOnlyPickaxe){//ツルハシ二個目
                hasOnlyPickaxe = false;
                break;
            }else{//最初のツルハシ
                hasOnlyPickaxe = true;
            }
        }
        if(!hasOnlyPickaxe){
            sender.sendMessage(ChatColor.RED + "ディスペンサーの中にはツルハシを一つだけ入れてください。");
            return false;
        }
        //登録
        bbLocations.add(b.getLocation());
        sender.sendMessage(ChatColor.AQUA + "ブロックブレイカーが作成されました。");
        return true;
    }

    @EventHandler
    public void onBlockDispense(BlockDispenseEvent event){
        //event.setCancelled(true);return;
        Block b = event.getBlock();
        if( b.getType() != Material.DISPENSER ){
            return;
        }
        Dispenser dispenser = (Dispenser) b.getState();
        if(/*true||*/ event.getBlock().getType() == Material.DISPENSER &&
           isPickaxe(event.getItem()) &&
           bbLocations.contains(event.getBlock().getLocation())){
            //方角判断
            BlockFace facing;
            String data = b.getBlockData().getAsString();
            //あんまりよくない実装かも…
            if(data.contains("facing=north")){
                facing = BlockFace.NORTH;
            }else if(data.contains("facing=south")){
                facing = BlockFace.SOUTH;
            }else if(data.contains("facing=east")){
                facing = BlockFace.EAST;
            }else if(data.contains("facing=west")){
                facing = BlockFace.WEST;
            }else if(data.contains("facing=up")){
                facing = BlockFace.UP;
            }else {//down
                facing = BlockFace.DOWN;
            }
            b.getRelative(facing).breakNaturally();
            event.setCancelled(true);
            return;
        }
        return;
    }
    //ピッケル判定
    public boolean isPickaxe(ItemStack item){
        Material material = item.getType();
        if(material == Material.WOODEN_PICKAXE ||
           material == Material.STONE_PICKAXE ||
           material == Material.IRON_PICKAXE ||
           material == Material.DIAMOND_PICKAXE ||
           material == Material.NETHERITE_PICKAXE ||
           material == Material.GOLDEN_PICKAXE) {
            return true;
        }else{
            return false;
        }
    }
}
