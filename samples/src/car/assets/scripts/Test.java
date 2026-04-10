package car.assets.scripts;

import org.joml.Vector2f;
import redball.engine.entity.ECSWorld;
import redball.engine.entity.GameObject;
import redball.engine.entity.components.Component;

import java.io.Serial;
import java.util.Random;

public class Test extends Component {
    @Serial
    private static final long serialVersionUID = 1L;

    public GameObject carPrefab;
    public float spawnInterval = 2.0f;
    public float minInterval = 0.5f;
    public float difficultyRate = 0.01f;

    private static final float[] LANES = { 0, 50, 100, 150 };

    private float timer = 0f;
    private Random random = new Random();
    private boolean[] laneCooldown = new boolean[LANES.length];
    private float[] laneCooldownTimer = new float[LANES.length];
    private static final float LANE_COOLDOWN = 1.5f;

    @Override
    public void start() {
        System.out.println("start call");
    }

    @Override
    public void update(float dt) {
        timer += dt;

        for (int i = 0; i < LANES.length; i++) {
            if (laneCooldown[i]) {
                laneCooldownTimer[i] -= dt;
                if (laneCooldownTimer[i] <= 0) laneCooldown[i] = false;
            }
        }

        if (timer >= spawnInterval) {
            timer = 0f;
            spawnCar();
            spawnInterval = Math.max(minInterval, spawnInterval - difficultyRate);
        }
    }

    private void spawnCar() {
        if (carPrefab == null) return;

        java.util.List<Integer> available = new java.util.ArrayList<>();
        for (int i = 0; i < LANES.length; i++) {
            if (!laneCooldown[i]) available.add(i);
        }

        if (available.isEmpty()) return;

        int laneIndex = available.get(random.nextInt(available.size()));
        float x = LANES[laneIndex];

        ECSWorld.instantiate(carPrefab, new Vector2f(x, 0f));

        laneCooldown[laneIndex] = true;
        laneCooldownTimer[laneIndex] = LANE_COOLDOWN;
    }
}