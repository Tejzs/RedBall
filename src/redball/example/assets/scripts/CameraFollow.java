package redball.example.assets.scripts;

import redball.engine.core.Engine;
import redball.engine.entity.GameObject;
import redball.engine.entity.components.Component;
import redball.engine.entity.components.Transform;

import java.io.IOException;
import java.io.Serial;

public class CameraFollow extends Component {
    @Serial
    private static final long serialVersionUID = 1L;

    public Transform camT;
    public Transform ballT;
    public Transform backGT;

    public GameObject ball;
    public GameObject background;

    @Override
    public void start() {
        camT = gameObject.getComponent(Transform.class);
        ballT = ball.getComponent(Transform.class);
        backGT = background.getComponent(Transform.class);
    }

    @Override
    public void update(float dt) {
        camT.setXPosition(ballT.getXPosition() - (Engine.getWindowManager().getWidth() / 2f));
        backGT.setXPosition(ballT.getXPosition());

        if (ballT.getYPosition() > 550) {
            camT.setYPosition(ballT.getYPosition() - (Engine.getWindowManager().getHeight() / 2f));
            backGT.setYPosition(ballT.getYPosition());
        }
    }
}