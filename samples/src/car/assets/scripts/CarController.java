package car.assets.scripts;

import redball.engine.entity.ECSWorld;
import redball.engine.entity.components.Component;
import redball.engine.entity.components.Rigidbody;
import redball.engine.entity.components.Transform;
import redball.engine.input.KeyboardInput;
import org.lwjgl.glfw.GLFW;
import redball.engine.scene.SceneManager;

import java.io.IOException;
import java.io.Serial;

public class CarController extends Component {
    @Serial
    private static final long serialVersionUID = 1L;

    public Transform transform;
    public Rigidbody rigidbody;

    public float clamp = 850;
    public float speed = 800f;

    @Override
    public void start() {
        transform = gameObject.getComponent(Transform.class);
        rigidbody = gameObject.getComponent(Rigidbody.class);
    }

    @Override
    public void update(float dt) {
        if (transform.getXPosition() > -clamp) {
            if (KeyboardInput.isKeyDown(GLFW.GLFW_KEY_LEFT)) {
                transform.setXPosition(transform.getXPosition() - speed * dt);
            }
        }
        if (transform.getXPosition() < clamp) {
            if (KeyboardInput.isKeyDown(GLFW.GLFW_KEY_RIGHT)) {
                transform.setXPosition(transform.getXPosition() + speed * dt);
            }
        }s
        if (KeyboardInput.isKeyDown(GLFW.GLFW_KEY_A)) {
            transform.setXPosition(transform.getXPosition() + speed * dt);
        }

        if (rigidbody.isCollided()) {
            ECSWorld.removeGameObject(gameObject);
            try {
                SceneManager.switchScenes(1);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}