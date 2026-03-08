package redball.example.assets.scripts;
import redball.engine.input.KeyboardInput;
import redball.engine.scene.SceneManager;
import org.lwjgl.glfw.GLFW;
import redball.engine.entity.components.Component;

import java.io.Serial;

public class Menu extends Component {
    @Serial
    private static final long serialVersionUID = 1L;

    public int menu = 0;
    public int game = 2;

    @Override
    public void start() {

    }

    @Override
    public void update(float dt) {
        if (KeyboardInput.isKeyDown(GLFW.GLFW_KEY_Q)) {
            SceneManager.switchScenes(menu);
        }
        if (KeyboardInput.isKeyDown(GLFW.GLFW_KEY_P)) {
            SceneManager.switchScenes(game);
        }
    }
}