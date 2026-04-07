package redball.engine.entity;

import redball.engine.entity.components.Component;
import redball.engine.entity.components.Rigidbody;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
}