package redball.engine.core;

import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

public class PhysicsSystem {
    private static World<Body> world;
    public static final float PPM = 32.0f;

    public static void init() {
        world = new World<>();
        world.setGravity(new Vector2(0, -25f));
    }

    public static void update(float deltaTime) {
        PhysicsSystem.getWorld().update(deltaTime);
    }

    public static World<Body> getWorld() {
        return world;
    }

    public static void clear() {
        world.removeAllBodiesAndJoints();
    }
}