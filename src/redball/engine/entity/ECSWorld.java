package redball.engine.entity;

import redball.engine.entity.components.Tag;
import redball.engine.renderer.BatchRenderer;
import redball.engine.renderer.RenderManager;

import java.util.ArrayList;
import java.util.List;

public class ECSWorld {
    // List of all gameobjects
    private static List<GameObject> gameObjects = new ArrayList<>();

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
        gameObjects.clear();
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
        RenderManager.render(camera);
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
}