package redball.engine.renderer.texture;

public enum TextureMap {

    BACKGROUND("example/assets/background.png"),
    BALL("example/assets/ball.png"),
    TEST1("example/assets/ground.png"),
    TEST2("example/assets/red.jpeg");

    private String filepath;

    TextureMap(String filepath) {
        this.filepath = filepath;
    }

    public String getFilePath() {
        return filepath;
    }
}
