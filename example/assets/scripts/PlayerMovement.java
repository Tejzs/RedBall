package redball.example.assets.scripts;

import org.dyn4j.geometry.Vector2;
import org.lwjgl.glfw.GLFW;
import redball.engine.entity.components.Component;
import redball.engine.entity.components.Rigidbody;
import redball.engine.entity.components.Tag;
import redball.engine.input.KeyboardInput;
import redball.engine.scene.SceneManager;

import java.io.Serial;

public class PlayerMovement extends Component {
    @Serial
    private static final long serialVersionUID = 1L;

    public Rigidbody ballBody;
    private boolean wasSpaceDown = false;
    public float maxSpeed = 27f;
    public float jumpForce = 200;
    public float jumpFossrce = 200;

    @Override
    public void start() {
        ballBody = gameObject.getComponent(Rigidbody.class);
    }

    @Override
    public void update(float dt) {
        Vector2 ballVelocity = ballBody.getBody().getLinearVelocity();
        boolean spaceDown = KeyboardInput.isKeyDown(GLFW.GLFW_KEY_SPACE) || KeyboardInput.isKeyDown(GLFW.GLFW_KEY_UP);
        Rigidbody hit = ballBody.isColliding();
        if (hit != null) {
            if (spaceDown && !wasSpaceDown && hit.gameObject.getComponent(Tag.class).getTag().equals("Ground")) {
                ballBody.getBody().applyImpulse(new Vector2(0, jumpForce));
            }
            wasSpaceDown = spaceDown;
        }

        if (KeyboardInput.isKeyDown(GLFW.GLFW_KEY_LEFT) ) {
            if (ballVelocity.x > -maxSpeed) {
                ballBody.getBody().applyForce(new Vector2(-1000000 * dt, 0));
            }
        }

        if (KeyboardInput.isKeyDown(GLFW.GLFW_KEY_RIGHT)) {
            if (ballVelocity.x < maxSpeed) {
                ballBody.getBody().applyForce(new Vector2(1000000 * dt, 0));
            }
        }

        // Press P to switch to demo scene
        if (KeyboardInput.isKeyDown(GLFW.GLFW_KEY_P)) {
            SceneManager.switchScenes(0);
        }
    }
}