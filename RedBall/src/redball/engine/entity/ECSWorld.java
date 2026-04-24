package redball.engine.entity;

import redball.engine.core.PhysicsSystem;
import redball.engine.editor.EditorLayer;
import redball.engine.entity.components.*;
import redball.engine.renderer.RenderManager;
import redball.engine.renderer.texture.TextureManager;
import redball.engine.save.SaveObject;

import java.util.*;

public class ECSWorld {
    // List of all gameobjects
    private static List<GameObject> gameObjects = new ArrayList<>();
    private static final List<GameObject> pendingAdd = new ArrayList<>();
    private static final List<GameObject> pendingRemove = new ArrayList<>();
    private static Map<String, GameObject> pool = new HashMap<>();


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
        Rigidbody rb = gameObject.getComponent(Rigidbody.class);
        if (rb != null) {
            PhysicsSystem.getWorld().removeBody(rb.getBody());
            rb.setBody(null);
        }
        pool.put(gameObject.getName(), gameObject);
        pendingRemove.add(gameObject);
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
            for (GameObject gameObject : pendingAdd) {
                int count = ECSWorld.countDuplicates(gameObject.getName());
                if (count > 0) {
                    int suffix = count;
                    while (count > 0) {
                        suffix++;
                        count = ECSWorld.countDuplicates(gameObject.getName() + " (" + suffix + ")");
                    }
                    gameObject.setName(gameObject.getName() + " (" + suffix + ")");
                }
                gameObject.start();
            }
            gameObjects.addAll(pendingAdd);
            pendingAdd.clear();
            RenderManager.rebuild();
        }
        if (!pendingRemove.isEmpty()) {
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

    public static void addPrefab(GameObject prefab) {
        GameObject instance = SaveObject.parseFrom(new SaveObject(new ArrayList<>(List.of(prefab))).toByteArray()).getGameObjects().getFirst();
        int count = countDuplicates(prefab.getName());
        if (count > 0) {
            int suffix = count;
            while (count > 0) {
                suffix++;
                count = countDuplicates(instance.getName() + " (" + suffix + ")");
            }
            instance.setName(instance.getName() + " (" + suffix + ")");
        }
        Rigidbody rb = instance.getComponent(Rigidbody.class);
        if (rb != null) {
            rb.createBody();
        }

        TextureManager.loadTextureForSprite(instance.getComponent(SpriteRenderer.class));
        gameObjects.add(instance);
        RenderManager.rebuild();
    }

    public static int countDuplicates(String name) {
        int count = 0;
        for (GameObject go : ECSWorld.getGameObjects()) {
            int index = go.getName().lastIndexOf("(");
            String stripped = (index == -1 ? go.getName() : go.getName().substring(0, index - 1));
            if (go.getName().equals(name) || stripped.equals(name)) {
                count++;
            }
        }
        return count;
    }

    public static List<GameObject> getPendingAdd() {
        return pendingAdd;
    }

    public static Map<String, GameObject> getPool() {
        return pool;
    }

    public static GameObject getCamera() {
        return Objects.requireNonNull(findGameObjectByTag("Camera"));
    }
}