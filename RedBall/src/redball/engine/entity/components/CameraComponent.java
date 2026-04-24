package redball.engine.entity.components;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import redball.engine.core.Engine;
import redball.engine.renderer.Camera;

import java.io.Serial;

// CameraComponent.java
public class CameraComponent extends Component {
    @Serial
    private static final long serialVersionUID = 1L;

    public Camera camera;
    public boolean isMain;
    private float[] cameraColor = new float[]{0.196f, 0.254f, 0.431f};;

    public CameraComponent() {
        this.camera = new Camera(new Vector2f(0, 0));
        this.isMain = true;
    }

    @Override
    public void update(float dt) {
        if (Engine.isPlaying()) {
            camera.setPosition(new Vector2f(gameObject.getComponent(Transform.class).position.x, gameObject.getComponent(Transform.class).position.y));
        }
    }

    public Matrix4f getViewMatrix() {
        return camera.getViewMat();
    }

    public Matrix4f getProjectionMatrix() {
        return camera.getProjectionMat();
    }

    public boolean isMain() {
        return isMain;
    }

    public Camera getCamera() {
        return camera;
    }

    public float[] getCameraColor() {
        return cameraColor;
    }
}