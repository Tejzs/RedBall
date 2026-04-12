package car.assets.scripts;

import org.lwjgl.glfw.GLFW;
import redball.engine.core.Engine;
import redball.engine.entity.components.Component;
import redball.engine.input.KeyboardInput;
import redball.engine.scene.SceneManager;

import java.io.IOException;
import java.io.Serial;

public class End extends Component {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void update(float dt) {
        if (KeyboardInput.isKeyDown(GLFW.GLFW_KEY_SPACE)) {
            try {
                SceneManager.switchScenes(2);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (KeyboardInput.isKeyDown(GLFW.GLFW_KEY_BACKSPACE)) {
            try {
                SceneManager.switchScenes(0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
