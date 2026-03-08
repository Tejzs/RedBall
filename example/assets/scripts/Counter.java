package redball.example.assets.scripts;

import redball.engine.entity.components.Component;

import java.io.Serial;

public class Counter extends Component {
    @Serial
    private static final long serialVersionUID = 1L;

    public float counter;

    @Override
    public void start() {

    }

    @Override
    public void update(float dt) {
        counter++;
        System.out.println(counter);
    }
}