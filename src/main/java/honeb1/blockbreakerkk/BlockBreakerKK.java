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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
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
import java.util.*;

public final class BlockBreakerKK extends JavaPlugin implements Listener {
    Configuration data;
    ArrayList<Location> bbLocations = new ArrayList<>();
    Map<Location, BlockFace> facingMap = new HashMap<>();

    final Material[] PickAxes = //type  1
            {Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
             Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE};
    final Material[] Axes = //type 2
            {Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
             Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE};
    final Material[] Shovels = //type 3
            {Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                    Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL};
    final Material[] Swords = //type 4
            {Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                    Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD};
    final Material[] Hoes = //type 5
            {Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
                    Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE};
    final Material[] Shears = {Material.SHEARS};//type 6



    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);
        //既存のBB読み込み
        data = this.getConfig();
        ArrayList<Location> locs  = new ArrayList<>();
        bbLocations = (ArrayList<Location>)data.getList("Locations");
        //無効なBB削除
        int deletedBBs = 0;
        ArrayList<Integer> indexesToRemove = new ArrayList<Integer>();
        for(int i=0;i<bbLocations.size();i++){
            Location loc = bbLocations.get(i);
            if(loc.getBlock().getType() != Material.DISPENSER){
                indexesToRemove.add(i);
                deletedBBs++;
                continue;
            }
            updateBBFace(loc);
        }
        for(int index : indexesToRemove){
            bbLocations.remove(index);
        }
        getServer().getConsoleSender().sendMessage("削除されたBB:"+deletedBBs+"個");
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
            bbLocations.remove(b.getLocation());
            sender.sendMessage("ブロックブレイカーが削除されました。");
            return true;
        }
        //ディスペンサー確定
        Dispenser dispenser = (Dispenser)b.getState();
        Inventory dInv = dispenser.getInventory();
        boolean hasOnlyOneTool = false;
        for(int i=0;i<dInv.getSize();i++){
            ItemStack itemStack = dInv.getItem(i);
            if(itemStack == null){
                continue;
            }else if( getToolType(itemStack.getType()) == 0 ) {//ツール以外
                hasOnlyOneTool = false;
                break;//使用不可
            }else if(hasOnlyOneTool){//ツール二個目
                hasOnlyOneTool = false;
                break;
            }else{//最初のツール
                hasOnlyOneTool = true;
            }
        }
        if(!hasOnlyOneTool){
            sender.sendMessage(ChatColor.RED + "ディスペンサーの中にはツールを一つだけ入れてください。");
            return false;
        }
        //登録
        bbLocations.add(b.getLocation());
        updateBBFace(b.getLocation());
        sender.sendMessage(ChatColor.AQUA + "ブロックブレイカーが作成されました。");
        return true;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event){
        Block b = event.getBlock();
        if(b.getType() != Material.DISPENSER){
            return;
        }else if(!bbLocations.contains(b.getLocation())){
            return;
        }
        //登録済みBB
        //登録解除
        bbLocations.remove(b.getLocation());
        event.getPlayer().sendMessage(ChatColor.DARK_GREEN + "ブロックブレイカーが削除されました。");
        return;
    }

    @EventHandler
    public void onBlockDispense(BlockDispenseEvent event){
        //event.setCancelled(true);return;
        Block b = event.getBlock();
        if( b.getType() != Material.DISPENSER ){
            return;
        }
        if(bbLocations.contains(event.getBlock().getLocation())){
            //方角判断
            BlockFace facing = facingMap.get(b.getLocation());
            Material targetMaterial = b.getRelative(facing).getType();
            event.setCancelled(true);
            if(targetMaterial == Material.BEDROCK ||
               targetMaterial == Material.END_PORTAL_FRAME ||
               targetMaterial == Material.COMMAND_BLOCK ||
               targetMaterial == Material.END_PORTAL ||
               targetMaterial == Material.WATER ||
               targetMaterial == Material.LAVA ||
               targetMaterial == Material.BARRIER){//破壊不可
                return;
            }
            ItemStack tool = event.getItem();
            Block targetBlock = b.getRelative(facing);
            if(targetBlock.getType() == Material.SNOW &&
               getToolType(tool.getType()) == 3/*シャベル*/){
                ItemStack itemToDrop;
                if(tool.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0){
                    itemToDrop = new ItemStack(Material.SNOW);
                }else{
                    itemToDrop = new ItemStack(Material.SNOWBALL);
                }
                targetBlock.breakNaturally();
                targetBlock.getWorld().dropItem(targetBlock.getLocation(),itemToDrop);
            }
            else {
                targetBlock.breakNaturally(tool);
            }
            return;
        }
        return;
    }
    //ピッケル判定
    public int getToolType(Material material){
        for(Material p : Shovels){
            if (material == p){
                return 3;//喋る
            }
        }
        for(Material p : PickAxes){
            if (material == p){
                return 1;//ピッケル
            }
        }
        for(Material p : Axes){
            if (material == p){
                return 2;//Oh No
            }
        }
        for(Material p : Swords){
            if (material == p){
                return 4;//剣
            }
        }
        for(Material p : Hoes){
            if (material == p){
                return 5;//ほえぇ...
            }
        }
        for(Material p : Shears){
            if (material == p){
                return 6;//✌
            }
        }
        return 0;//ツール以外
    }

    public void updateBBFace(Location loc){
        BlockFace facing;
        String data = loc.getBlock().getBlockData().getAsString();
        //あんまりよくない実装だけどgetFacingできないから仕方ない（怒）
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
        facingMap.put(loc,facing);
    }
}
