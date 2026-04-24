package redball.engine.entity;

import org.apache.commons.lang3.SerializationUtils;
import org.joml.Vector2f;
import redball.engine.editor.EditorAABB;
import redball.engine.entity.components.Component;
import redball.engine.entity.components.Rigidbody;
import redball.engine.entity.components.SpriteRenderer;
import redball.engine.entity.components.Transform;
import redball.engine.renderer.texture.TextureManager;
import redball.engine.utils.ScriptManager;

import java.io.*;
import java.util.ArrayList;


public class GameObject implements Serializable {
    // Name of the gameobject
    private String name;
    private static final long serialVersionUID = 1L;
    // List of all components
    private ArrayList<Component> components;

    /**
     * @description Creates new gameobject of given name.
     * @param name of the object.
     */
    public GameObject(String name) {
        this.name = name;
        components = new ArrayList<>();
    }

    /**
     * @description gets a component from a gameobject.
     * @param componentClass type of component.
     * @return the component.
     */
    public <T extends Component> T getComponent(Class<T> componentClass) {
        for (Component c : components) {
            try {
                if (componentClass.isInstance(c)) {
                    return componentClass.cast(c);
                }
            } catch (ClassCastException e) {
                e.printStackTrace();
                assert false : "Error: Casting Component";
            }
        }
        return null;
    }

    /**
     * @description removes a component from a gameobject
     * @param componentClass type of component.
     * @return true if success else false
     */
    public <T extends Component> boolean removeComponent(Class<T> componentClass) {
        for (Component c : components) {
            if (componentClass.isInstance(c)) {
                components.remove(c);
                return true;
            }
        }
        return false;
    }


    /**
     * @description adds a component to a gameobject
     * @param c type of component
     */
    public <T extends Component> T addComponent(Component c) {
        if (c == null) {
            return null;
        }
        if (contains(c)) return null;
        this.components.add(c);
        c.gameObject = this;
        return (T) getComponent(c.getClass());
    }

    /**
     * @description updates the gameobject (called every frame)
     * @param dt delta time
     */
    public void update(float dt) {
        for (Component c : components) {
            try {
                c.update(dt);
            } catch (Exception e) {
                System.err.println("ERROR: " + e);
            }
        }
    }

    /**
     * @description initializes the gameobjects (called once, in first frame)
     */
    public void start() {
        for (Component c : components) {
            try {
                c.start();
            } catch (Exception e) {
                System.err.println("ERROR: " + e);
            }
        }
    }

    /**
     * @return the name of a gameobject
     */
    public String getName() {
        return name;
    }

    public ArrayList<Component> getComponents() {
        return components;
    }

    public boolean contains(Component c) {
        for (Component component : components) {
            if (c.getClass().isInstance(component)) {
                return true;
            }
        }
        return false;
    }

    public void removeComponentByName(String className) {
        components.removeIf(c -> c.getClass().getName().equals(className));
    }

    public void setName(String name) {
        this.name = name;
    }

    public static void instantiate(GameObject prefab) {
        ECSWorld.getPendingAdd().add(instantiateInternal(prefab));
    }

    public static void instantiate(GameObject prefab, Vector2f position) {
        GameObject instance = instantiateInternal(prefab);
        instance.getComponent(Transform.class).setXPosition(position.x);
        instance.getComponent(Transform.class).setYPosition(position.y);
        ECSWorld.getPendingAdd().add(instance);
    }

    private static GameObject instantiateInternal(GameObject prefab) {
        if (ECSWorld.getPool().containsKey(prefab.getName())) {
            GameObject instance = ECSWorld.getPool().remove(prefab.getName());
            instance.getComponent(Rigidbody.class).createBody();
            return instance;
        }

        GameObject instance = prefab.deepCopy();
        Rigidbody rb = instance.getComponent(Rigidbody.class);
        if (rb != null) {
            rb.createBody();
        }
        TextureManager.loadTextureForSprite(instance.getComponent(SpriteRenderer.class));
        return instance;
    }

    private GameObject deepCopy() {
        byte[] bytes = SerializationUtils.serialize(this);

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes)) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                ClassLoader loader = ScriptManager.getScriptClassLoader(desc.getName());
                try {
                    return Class.forName(desc.getName(), false, loader);
                } catch (ClassNotFoundException e) {
                    return super.resolveClass(desc);
                }
            }
        }) {
            return (GameObject) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize scene: " + e.getMessage(), e);
        }
    }

    public void delete() {
        ECSWorld.removeGameObject(this);
    }

    public EditorAABB getBounds() {
        Transform transform = this.getComponent(Transform.class);
        if (transform != null) {
            if (transform.scale.x < 1 || transform.scale.y < 1) return null;
            return new EditorAABB(transform.position.x - (transform.scale.x / 2), transform.position.y - (transform.scale.y / 2), transform.scale.x, transform.scale.y);
        }
        return null;
    }
}