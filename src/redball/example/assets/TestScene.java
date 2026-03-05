package redball.example.assets;

import org.joml.Vector3f;

import redball.engine.core.*;
import redball.engine.entity.*;
import redball.engine.entity.components.*;
import redball.engine.renderer.*;
import redball.engine.renderer.texture.*;
import redball.engine.utils.*;
import redball.example.assets.scripts.CameraFollow;

public class TestScene extends AbstractScene {
    GameObject ball;
    GameObject groundL;
    GameObject groundC;
    GameObject background;
    GameObject groundR;
    GameObject camera = ECSWorld.createGameObject("Camera");

    @Override
    public void start() {
        PhysicsSystem.init();
        camera.addComponent(new Transform(new Vector3f(0.0f, 0.0f, 0.0f), 0.0f, new Vector3f(250.0f)));
        camera.addComponent(new CameraComponent(1920, 1080));
        camera.addComponent(new Tag("Camera"));

        background = ECSWorld.createGameObject("BackGround");
        background.addComponent(new Transform(new Vector3f(1920f / 2, 1080f / 2, -1f), 0f, new Vector3f(1920, 1080, 1)));
        background.addComponent(new SpriteRenderer(TextureManager.getTexture(TextureMap.BACKGROUND)));

        ball = ECSWorld.createGameObject("Ball");
        ball.addComponent(new Transform(new Vector3f(400.0f, 800.0f, -1.0f), 90.0f, new Vector3f(100.0f, 100.0f, 1.0f)));
        Rigidbody ballRb = ball.addComponent(new Rigidbody());
//        ball.addComponent(new SpriteRenderer(TextureManager.getTexture(TextureMap.BALL)));

        groundL = ECSWorld.createGameObject("GroundL");
        groundL.addComponent(new Transform(new Vector3f(150.0f, 250.0f, -1.0f), -15.0f, new Vector3f(1920.0f / 2, 20.0f, 1.0f)));
        Rigidbody groundLRb = this.groundL.addComponent(new Rigidbody());
        groundL.addComponent(new SpriteRenderer(TextureManager.getTexture(TextureMap.TEST1)));
        groundL.addComponent(new Tag("Ground"));

        groundC = ECSWorld.createGameObject("GroundC");
        groundC.addComponent(new Transform(new Vector3f(1000.0f, 150.0f, -1.0f), 0.0f, new Vector3f(1920.0f / 2, 20.0f, 1.0f)));
        Rigidbody groundCRb = this.groundC.addComponent(new Rigidbody());
        groundC.addComponent(new SpriteRenderer(TextureManager.getTexture(TextureMap.TEST1)));
        groundC.addComponent(new Tag("Ground"));

        groundR = ECSWorld.createGameObject("GroundR");
        groundR.addComponent(new Transform(new Vector3f(1900.0f, 250.0f, -1.0f), 15.0f, new Vector3f(1920.0f / 2, 20.0f, 1.0f)));
        Rigidbody groundRRb = this.groundR.addComponent(new Rigidbody());
        groundR.addComponent(new SpriteRenderer(TextureManager.getTexture(TextureMap.TEST1)));
        groundR.addComponent(new Tag("Ground"));

        ballRb.setMass(100);
        ballRb.setBounce(0.1);
        ballRb.setFriction(0.2);

        groundLRb.setBodyType(BodyType.STATIC);
        groundCRb.setBodyType(BodyType.STATIC);
        groundRRb.setBodyType(BodyType.STATIC);
        ballRb.setFixture(BodyFixture.CIRCLE);

        RenderManager.prepare(camera);
    }

    @Override
    public void update(float deltaTime) {
        ECSWorld.update(camera, deltaTime);
    }
}