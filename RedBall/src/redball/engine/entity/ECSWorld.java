package redball.engine.entity;

import org.apache.commons.lang3.SerializationUtils;
import org.joml.Vector2f;
import redball.engine.core.Engine;
import redball.engine.entity.components.*;
import redball.engine.renderer.RenderManager;
import redball.engine.renderer.texture.TextureManager;
import redball.engine.utils.PakWriter;

import java.util.ArrayList;
import java.util.List;

public class ECSWorld {
    // List of all gameobjects
    private static List<GameObject> gameObjects = new ArrayList<>();
    private static final List<GameObject> pendingAdd = new ArrayList<>();

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
    public static boolean removeGameObject(GameObject gameObject) {
        for (GameObject g : gameObjects) {
            if (g.equals(gameObject)) {
                gameObjects.remove(g);
                assert true : "SUCCESS: REMOVED GAMEOBJECT";
                return true;
            }
        }
        assert false : "FAILED: TO REMOVE GAMEOBJECT";
        return false;
    }

    public static void clearGameObjects() {
        gameObjects = new ArrayList<>();
    }

    /**
     * @description removes gameobject by given name
     * @param name of gameobject
     * @return true if found else false
     */
    public static boolean removeGameObject(String name) {
        GameObject go = findGameObjectByName(name);
        if (go == null) {
            assert false : "FAILED: TO REMOVE GAMEOBJECT, IS NULL";
            return false;
        }
        return removeGameObject(go);
    }

    /**
     * @description removes gameobject by given tag
     * @param tag of gameobject
     * @return true if found else false
     */
    public static boolean removeGameObjectByTag(String tag) {
        GameObject go = findGameObjectByTag(tag);
        if (go == null) {
            assert false : "FAILED: TO REMOVE GAMEOBJECT, IS NULL";
            return false;
        }
        return removeGameObject(go);
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
        GameObject go = SerializationUtils.deserialize(SerializationUtils.serialize(prefab));
        go.getComponent(Transform.class).setXPosition(position.x);
        go.getComponent(Transform.class).setYPosition(position.y);
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
}