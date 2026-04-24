package car.assets.scripts;

import redball.engine.entity.components.Component;
import redball.engine.entity.components.Transform;

import java.io.Serial;

public class EnemyCars extends Component {
    @Serial
    private static final long serialVersionUID = 1L;

    public Transform transform;

    @Override
    public void start() {
        transform = gameObject.getComponent(Transform.class);
    }

    @Override
    public void update(float dt) {
        if (transform.getYPosition() < -800) {
            gameObject.delete();
        }
    }
}
