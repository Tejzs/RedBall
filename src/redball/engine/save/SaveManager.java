package redball.engine.save;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.joml.Vector3f;
import redball.engine.core.Engine;
import redball.engine.entity.components.*;
import redball.engine.renderer.texture.Texture;
import redball.engine.scene.AssetManager;
import redball.engine.core.PhysicsSystem;
import redball.engine.entity.ECSWorld;
import redball.engine.entity.GameObject;
import redball.engine.renderer.RenderManager;
import redball.engine.renderer.texture.TextureManager;
import redball.engine.scene.SceneManager;
import redball.engine.utils.PakWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SaveManager {
    public static void save() {
        try (BufferedOutputStream sceneOut = new BufferedOutputStream(new FileOutputStream((AssetManager.getINSTANCE().currentWorkingScene)))) {
            IOUtils.write(new SaveObject().toByteArray(), sceneOut);
        } catch (IOException e) {
            System.out.println("ERROR:" + e);
        }
    }

    public static void loadScene(String scene) {
        try (BufferedInputStream sceneIn = new BufferedInputStream(new FileInputStream(scene))) {
            SaveObject saveObject = SaveObject.parseFrom(IOUtils.toByteArray(sceneIn));

            if (PhysicsSystem.getWorld() != null) {
                PhysicsSystem.clear();
            }
            ECSWorld.removeAll();
            RenderManager.clear();
            TextureManager.clear();

            PhysicsSystem.init();
            ECSWorld.setGameObjects(saveObject.getGameObjects());

            // reload all textures from file paths
            for (GameObject go : ECSWorld.getGameObjects()) {
                SpriteRenderer sr = go.getComponent(SpriteRenderer.class);
                Rigidbody rb = go.getComponent(Rigidbody.class);
                if (rb != null) {
                    rb.createBody();
                }

                if (sr != null && sr.getFilePath() != null) {
                    if (Engine.isBuild) {
                        sr.setTexture(TextureManager.getTexture(PakWriter.getManifestFile().get(sr.getFilePath())));
                    } else {
                        sr.setTexture(TextureManager.getTexture(sr.getFilePath()));
                    }
                }
            }

            RenderManager.prepare(ECSWorld.findGameObjectByTag("Camera"));
            ECSWorld.start();
            AssetManager.getINSTANCE().currentWorkingScene = scene;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void newScene(String sceneName) {
        // create default camera
        GameObject camera = new GameObject("Camera");
        camera.addComponent(new Transform(new Vector3f(0, 0, 0), 0f, new Vector3f(1, 1, 1)));
        camera.addComponent(new CameraComponent(Engine.getWindowManager().getWidth(), Engine.getWindowManager().getHeight()));
        camera.addComponent(new Tag("Camera"));
        ArrayList<GameObject> gameObjects = new ArrayList<>();
        gameObjects.add(camera);
        try (BufferedOutputStream sceneOut = new BufferedOutputStream(new FileOutputStream((AssetManager.getINSTANCE().getScenesDirectory() + sceneName)))) {
            IOUtils.write(new SaveObject(gameObjects).toByteArray(), sceneOut);
        } catch (IOException e) {
            System.out.println("ERROR:" + e);
        }
    }
}
