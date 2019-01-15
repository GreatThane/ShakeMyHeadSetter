package org.thane;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.Skull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ShakeMyHeadSetter extends JavaPlugin implements Listener {

    public static Plugin INSTANCE;
    static String VERSION;
    static Gson GSON = new GsonBuilder().disableHtmlEscaping().enableComplexMapKeySerialization().setPrettyPrinting().create();

    private static boolean PRETTY_PRINTING = false;

    public ShakeMyHeadSetter() {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        VERSION = packageName.substring(packageName.lastIndexOf('.') + 1);
        INSTANCE = this;
        SkullHelper.init();
    }

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            saveDefaultConfig();
        }
        PRETTY_PRINTING = getConfig().getBoolean("pretty-print-cache");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        SkullHelper.saveCache(PRETTY_PRINTING);
    }

    public static Gson getGson() {
        return GSON;
    }

    public static void setGson(Gson gson) {
        ShakeMyHeadSetter.GSON = gson;
    }

    public static String getVersion() {
        return VERSION;
    }

    public static void setVersion(String version) {
        ShakeMyHeadSetter.VERSION = version;
    }

    private static final Pattern URL_NAME_PATTERN = Pattern.compile("(?:[^/][\\d\\w.]+)$(?<=\\.\\w{3,4})", Pattern.MULTILINE);
    private static final Pattern COMMAND_PATTERN = Pattern.compile("\\\\\"text\\\\\":\\\\\"(.*?)\\\\\".*?Value:\"(.*?)\"", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "savecache":
                if (sender.hasPermission(new Permission("smh.save", PermissionDefault.OP))) {
                    if (args.length > 0) {
                        SkullHelper.saveCache(Boolean.parseBoolean(args[0]));
                    } else SkullHelper.saveCache(PRETTY_PRINTING);
                    return true;
                }
                break;
            case "registerfile":
                if (sender.hasPermission(new Permission("smh.register.file", PermissionDefault.OP))) {
                    if (args.length > 0) {
                        File file = new File(getDataFolder(), args[0]);
                        SkullHelper.cacheSkin(file.getName().substring(0, file.getName().lastIndexOf(".")), file, args.length > 1 ? Boolean.valueOf(args[1]) : false);
                    } else return false;
                    return true;
                }
                break;
            case "registerurl":
                if (sender.hasPermission(new Permission("smh.register.url", PermissionDefault.OP))) {
                    if (args.length > 0) {
                        String name;
                        Matcher matcher = URL_NAME_PATTERN.matcher(args[0]);
                        if (matcher.find()) {
                            name = matcher.group(0).substring(0, matcher.group(0).lastIndexOf("."));
                        } else {
                            name = String.valueOf(ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE / 1000));
                        }
                        try {
                            SkullHelper.cacheSkin(name, new URL(args[0]), args.length > 1 ? Boolean.valueOf(args[1]) : false);
                            sender.sendMessage(ChatColor.GREEN + "URL registered under name " + name);
                            return true;
                        } catch (MalformedURLException e) {
                            sender.sendMessage(ChatColor.RED + "URL " + args[0] + " is malformed.");
                            return false;
                        }
                    } else return false;
                }
                break;
            case "gethead":
                if (sender.hasPermission(new Permission("smh.supply.item", PermissionDefault.OP))) {
                    if (sender instanceof Player && args.length > 0) {
                        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                        SkullHelper.setTexture(head, args[0]);
                        ((Player) sender).getInventory().addItem(head);
                        return true;
                    } else return false;
                }
            case "getwand":
                if (sender.hasPermission(new Permission("smh.supply.set", PermissionDefault.OP))) {
                    if (sender instanceof Player && args.length > 0) {
                        ItemStack wand = new ItemStack(Material.STICK);
                        ItemMeta meta = wand.getItemMeta();
                        meta.setDisplayName(ChatColor.GREEN + "Head Transformation Wand");
                        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Head: " + String.join(" ", args).trim()));
                        wand.setItemMeta(meta);
                        ((Player) sender).getInventory().addItem(wand);
                        return true;
                    }
                }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "savecache":
                if (sender.hasPermission(new Permission("smh.save", PermissionDefault.OP))) {
                    if (args.length == 1) {
                        return Arrays.asList(Boolean.TRUE.toString(), Boolean.FALSE.toString());
                    } else return new ArrayList<>();
                }
                break;
            case "registerfile":
                if (sender.hasPermission(new Permission("smh.register.file", PermissionDefault.OP))) {
                    switch (args.length) {
                        case 0:
                            return Arrays.asList(getDataFolder().list());
                        case 1:
                            File file = new File(getDataFolder(), args[0]);
                            if (file.exists() && file.isDirectory()) {
                                return Arrays.asList(file.list((file1, name) -> {
                                    if (file1.isDirectory()) {
                                        return true;
                                    } else if (name.toLowerCase().endsWith(".png")) {
                                        return true;
                                    } else return false;
                                }));
                            } else return new ArrayList<>();
                        default:
                            return Arrays.asList(Boolean.TRUE.toString(), Boolean.FALSE.toString());
                    }
                }
                break;
            case "gethead":
                if (sender.hasPermission(new Permission("smh.supply.item", PermissionDefault.OP))) {
                    return SkullHelper.getSkinCache().entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList());
                }
                break;
            case "getwand":
                if (sender.hasPermission(new Permission("smh.supply.set", PermissionDefault.OP))) {
                    return SkullHelper.getSkinCache().entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList());
                }
                break;
        }
        return new ArrayList<>();
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getClickedBlock().getState() instanceof Skull) {
                if (event.getPlayer().hasPermission(new Permission("smh.register.file", PermissionDefault.OP))) {
                    if (event.hasItem() && event.getItem() != null && event.getItem().hasItemMeta() && event.getItem().getItemMeta().hasDisplayName() && event.getItem().getItemMeta().hasLore()) {
                        if (ChatColor.stripColor(event.getItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Head Transformation Wand")) {
                            String name = ChatColor.stripColor(event.getItem().getItemMeta().getLore().get(0)).replaceAll("Head: ", "").trim();
                            SkullHelper.setTexture((Skull) event.getClickedBlock().getState(), name);
                        }
                    }
                }
            } else if (event.getClickedBlock().getState() instanceof CommandBlock) {
                if (event.getPlayer().hasPermission(new Permission("smh.register.base64", PermissionDefault.OP)) && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.NETHER_STAR) {
                    Matcher matcher = COMMAND_PATTERN.matcher(((CommandBlock) event.getClickedBlock().getState()).getCommand());
                    if (matcher.find()) {
                        SkullHelper.cacheSkin(matcher.group(1), matcher.group(2), true);
                        event.getPlayer().sendMessage(ChatColor.GREEN + "this skull's texture has been added under name " + matcher.group(1));
                    }
                }
            }
        }
    }
}
