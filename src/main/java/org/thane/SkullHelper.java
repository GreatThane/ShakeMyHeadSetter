package org.thane;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import javax.imageio.ImageIO;
import java.io.*;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.UUID;

public class SkullHelper {

    private static final URL UPLOAD_URL;

    private static final File SKIN_CACHE_FILE = new File(ShakeMyHeadSetter.INSTANCE.getDataFolder().getAbsolutePath() + File.separatorChar + "skin_cache.json");

    private static final String BOUNDARY = "*****";
    private static final String HYPHENS = "--";
    private static final String CRLF = "\r\n";

    private static final JsonObject SKIN_CACHE;

    private static final Field ITEM_PROFILE_FIELD;
    private static final Field BLOCK_PROFILE_FIELD;

    static {
        JsonObject SKIN_CACHE1 = null;
        Field ITEM_PROFILE_FIELD1 = null;
        Field BLOCK_PROFILE_FIELD1 = null;
        URL UPLOAD_URL1 = null;
        try {
            UPLOAD_URL1 = new URL("https://api.mineskin.org/generate/upload");
            if (SKIN_CACHE_FILE.exists()) {
                SKIN_CACHE1 = new JsonParser().parse(new String(Files.readAllBytes(SKIN_CACHE_FILE.toPath()))).getAsJsonObject();
            } else SKIN_CACHE1 = new JsonObject();
            ITEM_PROFILE_FIELD1 = Bukkit.getItemFactory().getItemMeta(Material.PLAYER_HEAD).getClass().getDeclaredField("profile");
            ITEM_PROFILE_FIELD1.setAccessible(true);
            BLOCK_PROFILE_FIELD1 = Class.forName("org.bukkit.craftbukkit." + ShakeMyHeadSetter.VERSION + ".block.CraftSkull").getDeclaredField("profile");
            BLOCK_PROFILE_FIELD1.setAccessible(true);
        } catch (IOException | NoSuchFieldException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        UPLOAD_URL = UPLOAD_URL1;
        ITEM_PROFILE_FIELD = ITEM_PROFILE_FIELD1;
        BLOCK_PROFILE_FIELD = BLOCK_PROFILE_FIELD1;
        SKIN_CACHE = SKIN_CACHE1;
    }

    @SuppressWarnings("Duplicates")
    public static void cacheSkin(String cachedName, File file, boolean override) {
        if (override || !SKIN_CACHE.has(cachedName)) {
            Runnable runnable = () -> {
                try {
                    byte[] pngBytes = Files.readAllBytes(file.toPath());

                    HttpURLConnection connection = (HttpURLConnection) UPLOAD_URL.openConnection();

                    connection.setRequestMethod("POST");

                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);
                    connection.connect();

                    connection.getOutputStream().write((HYPHENS + BOUNDARY + CRLF).getBytes());
                    connection.getOutputStream().write(("Content-Disposition: form-data; name=\"name\"" + CRLF + CRLF).getBytes());
                    connection.getOutputStream().write((cachedName + CRLF).getBytes());

                    connection.getOutputStream().write((HYPHENS + BOUNDARY + CRLF).getBytes());
                    connection.getOutputStream().write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + '\"' + CRLF).getBytes());
                    connection.getOutputStream().write(("Content-Type: image/png" + CRLF + CRLF).getBytes());
                    connection.getOutputStream().write(pngBytes);
                    connection.getOutputStream().write(CRLF.getBytes());

                    connection.getOutputStream().write((HYPHENS + BOUNDARY + HYPHENS + CRLF).getBytes());

                    connection.getOutputStream().flush();
                    connection.disconnect();

                    connection.setDoInput(true);

                    InputStream responseStream = new BufferedInputStream(connection.getInputStream());
                    BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));
                    String line;
                    StringBuilder builder = new StringBuilder();
                    while ((line = responseStreamReader.readLine()) != null) {
                        builder.append(line).append("\n");
                    }
                    responseStream.close();
                    connection.disconnect();

                    synchronized (SKIN_CACHE) {
                        SKIN_CACHE.add(cachedName, new JsonParser().parse(builder.toString()).getAsJsonObject());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            if (Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTaskAsynchronously(ShakeMyHeadSetter.INSTANCE, runnable);
            } else runnable.run();
        }
    }

    @SuppressWarnings("Duplicates")
    public static void cacheSkin(String cachedName, String base64, boolean override) {
        if (override || !SKIN_CACHE.has(cachedName)) {
            JsonObject object = new JsonParser().parse(Base64Coder.decodeString(base64)).getAsJsonObject();
            JsonObject finalObject = new JsonObject();
            finalObject.add("name", new JsonPrimitive(cachedName));
            JsonObject data = new JsonObject();
            data.add("uuid", new JsonPrimitive(UUID.randomUUID().toString()));
            JsonObject texture = new JsonObject();
            texture.add("url", object.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url"));
            data.add("textures", texture);
            data.add("value", new JsonPrimitive(base64));
            finalObject.add("data", data);

            SKIN_CACHE.add(cachedName, finalObject);
        }
    }

    @SuppressWarnings("Duplicates")
    public static void cacheSkin(String cachedName, UUID playerUUID, boolean override) {
        if (override || !SKIN_CACHE.has(cachedName)) {
            Runnable runnable = () -> {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL("https://api.mineskin.org/generate/user/" + playerUUID.toString()).openConnection();

                    connection.setDoInput(true);

                    InputStream responseStream = new BufferedInputStream(connection.getInputStream());
                    BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));
                    String line;
                    StringBuilder builder = new StringBuilder();
                    while ((line = responseStreamReader.readLine()) != null) {
                        builder.append(line).append("\n");
                    }
                    responseStream.close();
                    connection.disconnect();
                    synchronized (SKIN_CACHE) {
                        SKIN_CACHE.add(cachedName, new JsonParser().parse(builder.toString()).getAsJsonObject());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };
            if (Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTaskAsynchronously(ShakeMyHeadSetter.INSTANCE, runnable);
            } else runnable.run();
        }
    }

    @SuppressWarnings("Duplicates")
    public static void cacheSkin(String cachedName, URL url, boolean override) {
        if (override || !SKIN_CACHE.has(cachedName)) {
            if (url.getHost().contains("minecraft.net") || url.getHost().contains("mojang.com")) {
                JsonObject object = new JsonObject();
                object.add("name", new JsonPrimitive(cachedName));
                JsonObject data = new JsonObject();
                data.add("uuid", new JsonPrimitive(UUID.randomUUID().toString()));
                JsonObject texture = new JsonObject();
                texture.add("url", new JsonPrimitive(url.toExternalForm()));
                data.add("textures", texture);
                object.add("data", data);

                SKIN_CACHE.add(cachedName, object);
            } else {
                Runnable runnable = () -> {
                    try {
                        HttpURLConnection connection = (HttpURLConnection) new URL("https://api.mineskin.org/generate/url?url=" + url.toExternalForm()).openConnection();
                        connection.setRequestMethod("POST");
                        connection.setDoOutput(true);
                        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);
                        connection.connect();
                        connection.getOutputStream().write(CRLF.getBytes());
                        connection.getOutputStream().write((HYPHENS + BOUNDARY + HYPHENS + CRLF).getBytes());

                        connection.getOutputStream().flush();
                        connection.disconnect();

                        connection.setDoInput(true);

                        InputStream responseStream = new BufferedInputStream(connection.getInputStream());
                        BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));
                        String line;
                        StringBuilder builder = new StringBuilder();
                        while ((line = responseStreamReader.readLine()) != null) {
                            builder.append(line).append("\n");
                        }
                        responseStream.close();
                        connection.disconnect();

                        synchronized (SKIN_CACHE) {
                            SKIN_CACHE.add(cachedName, new JsonParser().parse(builder.toString()).getAsJsonObject());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                };
                if (Bukkit.isPrimaryThread()) {
                    Bukkit.getScheduler().runTaskAsynchronously(ShakeMyHeadSetter.INSTANCE, runnable);
                } else runnable.run();
            }
        }
    }

    public static void cacheSkin(String cachedName, JsonObject object, boolean override) {
        if (override || !SKIN_CACHE.has(cachedName))
            SKIN_CACHE.add(cachedName, object);
    }

    @SuppressWarnings("Duplicates")
    public static void setTexture(ItemStack head, String cachedName) {
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        JsonObject element = (JsonObject) SKIN_CACHE.get(cachedName);

        String name = element.get("name").getAsString();
        if (name != null && name.isEmpty()) name = null;

        GameProfile profile = new GameProfile(UUID.fromString(element.getAsJsonObject("data").get("uuid").getAsString()), name);
        profile.getProperties().put("textures", new Property("textures", Base64Coder.encodeString(String.format("{textures:{SKIN:{url:\"%s\"}}}",
                SKIN_CACHE.get(cachedName).getAsJsonObject().getAsJsonObject("data").getAsJsonObject("textures").get("url").getAsString()))));
        try {
            ITEM_PROFILE_FIELD.set(meta, profile);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        head.setItemMeta(meta);
    }

    @SuppressWarnings("Duplicates")
    public static void setTexture(Skull skull, String cachedName) {
        JsonObject element = (JsonObject) SKIN_CACHE.get(cachedName);
        String name = element.get("name").getAsString();
        if (name != null && name.isEmpty()) name = null;
        GameProfile profile = new GameProfile(UUID.fromString(element.getAsJsonObject("data").get("uuid").getAsString()), name);
        profile.getProperties().put("textures", new Property("textures", Base64Coder.encodeString(String.format("{textures:{SKIN:{url:\"%s\"}}}",
                element.getAsJsonObject("data").getAsJsonObject("textures").get("url").getAsString()))));
        try {
            BLOCK_PROFILE_FIELD.set(skull, profile);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        skull.update(true, false);
    }

    public static void saveCache() {
        saveCache(false);
    }

    public static void saveCache(boolean prettyPrinting) {
        if (!SKIN_CACHE_FILE.exists()) {
            try {
                SKIN_CACHE_FILE.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (PrintStream out = new PrintStream(new FileOutputStream(SKIN_CACHE_FILE))) {
            if (prettyPrinting) {
                out.print(ShakeMyHeadSetter.getGson().toJson(SKIN_CACHE));
            } else out.print(SKIN_CACHE.toString());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static JsonObject getSkinCache() {
        return SKIN_CACHE;
    }

    static void init() {
    }
}
