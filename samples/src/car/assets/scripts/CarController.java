package car.assets.scripts;

import redball.engine.entity.components.Component;
import redball.engine.entity.components.Transform;
import redball.engine.input.KeyboardInput;
import org.lwjgl.glfw.GLFW;

import java.io.Serial;

public class CarController extends Component {
    @Serial
    private static final long serialVersionUID = 1L;

    public Transform carTransform;
    public float move;
    public float speed = 800f;

    @Override
    public void start() {
        carTransform = gameObject.getComponent(Transform.class);
    }

    @Override
    public void update(float dt) {
        if (carTransform.getXPosition() > 400) {
            if (KeyboardInput.isKeyDown(GLFW.GLFW_KEY_LEFT)) {
                carTransform.setXPosition(carTransform.getXPosition() - speed * dt);
            }
        }
        if (carTransform.getXPosition() < 1535) {
            if (KeyboardInput.isKeyDown(GLFW.GLFW_KEY_RIGHT)) {
                carTransform.setXPosition(carTransform.getXPosition() + speed * dt);
            }
        }
        if (KeyboardInput.isKeyDown(GLFW.GLFW_KEY_A)) {
            carTransform.setXPosition(carTransform.getXPosition() + speed * dt);
        }
    }
}