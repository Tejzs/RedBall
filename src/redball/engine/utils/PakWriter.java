package redball.engine.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import redball.engine.scene.AssetManager;

import java.io.*;
import java.util.HashMap;
import java.util.Map;


public class PakWriter {
    private static int counter = 0;
    private static final JsonArray manifest = new JsonArray();
    private static final Map<String, String> manifestFile = new HashMap<>();

    public static void writePak(String path) throws Exception {
        if (AssetManager.getINSTANCE().getWorkingDirectory().equals(path)) {
            if (!ScriptManager.compileAll(AssetManager.getINSTANCE().getScriptDirectory())) {
                System.err.println("Build failed fix errors");
                return;
            }
        }
        Gson gson = new Gson();
        File root = new File(path);
        for (File file : root.listFiles()) {
            if (file.getName().equals("build")) continue;
            if (file.isDirectory()) {
                writePak(file.getPath());
            } else {
                JsonObject meta = new JsonObject();
                byte[] fileBytes = new byte[(int) file.length()];
                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.read(fileBytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try (FileOutputStream fos = new FileOutputStream(AssetManager.getINSTANCE().getBuildDirectory() + "pak" + counter + ".pak")) {
                    fos.write(fileBytes);
                    meta.addProperty(file.getPath(), "pak" + counter + ".pak");
                    manifest.add(meta);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                counter++;
            }
        }
        if (AssetManager.getINSTANCE().getWorkingDirectory().equals(path)) {
            System.out.println("Successfully Built!!");
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(AssetManager.getINSTANCE().getBuildDirectory() + "manifest.meta"))) {
            String json = gson.toJson(manifest);
            bw.write(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadPak() {
        Gson gson = new Gson();
        JsonArray jsonArray;
        try (BufferedReader br = new BufferedReader(new FileReader("manifest.meta"))) {
            jsonArray = gson.fromJson(br, JsonArray.class);
            System.out.println(jsonArray);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (JsonElement element : jsonArray) {
            JsonObject obj = element.getAsJsonObject();

            for (String key : obj.keySet()) {
                String value = obj.get(key).getAsString();
                manifestFile.put(key, value);
            }
        }
        PakWriter.getManifestFile().forEach((k, v) -> System.out.println("KEY: " + k + " → " + v));

    }

    public static Map<String, String> getManifestFile() {
        return manifestFile;
    }

    public static void resetCounter() {
        PakWriter.counter = 0;
    }
}