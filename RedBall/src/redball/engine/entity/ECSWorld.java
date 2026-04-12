package redball.engine.entity;

import org.apache.commons.lang3.SerializationUtils;
import org.joml.Vector2f;
import redball.engine.core.Engine;
import redball.engine.core.PhysicsSystem;
import redball.engine.entity.components.*;
import redball.engine.renderer.RenderManager;
import redball.engine.renderer.texture.TextureManager;
import redball.engine.save.SaveObject;
import redball.engine.utils.PakWriter;

import java.util.ArrayList;
import java.util.List;

public class ECSWorld {
    // List of all gameobjects
    private static List<GameObject> gameObjects = new ArrayList<>();
    private static final List<GameObject> pendingAdd = new ArrayList<>();
    private static final List<GameObject> pendingRemove = new ArrayList<>();

    public ECSWorld() {}

    /**
     * @description Creates new gameobject of given name.
     * @param name of the object.
     * @return the created gameobject
     */
    public static GameObject createGameObject(String name) {
        GameObject go = new GameObject(name);
        gameObjects.add(go);
        return go;
    }

    /**
     * @description finds gameobject by given name
     * @param name of the object
     * @return gameobject if found else null
     */
    public static GameObject findGameObjectByName(String name) {
        for (GameObject g : gameObjects) {
            if (g.getName().equals(name)) return g;
        }
        return null;
    }

    /**
     * @description finds gameobject by given tag
     * @param tag of the object
     * @return gameobject if found else null
     */
    public static GameObject findGameObjectByTag(String tag) {
        for (GameObject g : gameObjects) {
            Tag gTag = g.getComponent(Tag.class);
            if (gTag != null) {
                if (gTag.getTag().equals(tag)) return g;
            }
        }
        return null;
    }

    /**
     * @description removes given gameobject
     * @param gameObject we want to remove
     * @return true if found else false
     */
    public static void removeGameObject(GameObject gameObject) {
        pendingRemove.add(gameObject);
    }

    public static void clearGameObjects() {
        gameObjects = new ArrayList<>();
    }

    public static void removeAll() {
        gameObjects = new ArrayList<>();
    }

    /**
     * @description updates all gameobjects
     * @param dt delta time
     */
    public static void update(GameObject camera, float dt) {
        camera.update(dt);
        for (GameObject g : gameObjects) {
            g.update(dt);
        }
        if (!pendingAdd.isEmpty()) {
            gameObjects.addAll(pendingAdd);
            pendingAdd.clear();
            RenderManager.rebuild();
        }
        if (!pendingRemove.isEmpty()) {
            for (GameObject gameObject : pendingRemove) {
                PhysicsSystem.getWorld().removeBody(gameObject.getComponent(Rigidbody.class).getBody());
            }
            gameObjects.removeAll(pendingRemove);
            pendingRemove.clear();
            RenderManager.rebuild();
        }
    }

    public static void start() {
        for (GameObject g : gameObjects) {
            g.start();
        }
    }

    public static List<GameObject> getGameObjects() {
        return gameObjects;
    }

    public static void setGameObjects(List<GameObject> gameObjects) {
        ECSWorld.gameObjects = gameObjects;
    }

    public static void instantiate(GameObject prefab) {
        GameObject go = SerializationUtils.deserialize(SerializationUtils.serialize(prefab));
        SpriteRenderer sr = go.getComponent(SpriteRenderer.class);
        Rigidbody rb = go.getComponent(Rigidbody.class);
        if (rb != null) {
            rb.createBody();
        }

        if (sr != null && sr.getFilePath() != null) {
            if (Engine.isBuild) {
                sr.setTexture(TextureManager.getTexture(sr.getFilePath(), PakWriter.getAsset(sr.getFilePath())));
            } else {
                sr.setTexture(TextureManager.getTexture(sr.getFilePath()));
            }
        }
        go.start();
        pendingAdd.add(go);
    }

    public static void instantiate(GameObject prefab, Vector2f position) {
        GameObject instance = SaveObject.parseFrom(new SaveObject(new ArrayList<>(List.of(prefab))).toByteArray()).getGameObjects().getFirst();

        SpriteRenderer sr = instance.getComponent(SpriteRenderer.class);
        Rigidbody rb = instance.getComponent(Rigidbody.class);
        if (rb != null) {
            rb.createBody();
        }

        instance.getComponent(Transform.class).setXPosition(position.x);
        instance.getComponent(Transform.class).setYPosition(position.y);

        if (sr != null && sr.getFilePath() != null) {
            if (Engine.isBuild) {
                sr.setTexture(TextureManager.getTexture(sr.getFilePath(), PakWriter.getAsset(sr.getFilePath())));
            } else {
                sr.setTexture(TextureManager.getTexture(sr.getFilePath()));
            }
        }
        instance.start();
        pendingAdd.add(instance);
    }
}