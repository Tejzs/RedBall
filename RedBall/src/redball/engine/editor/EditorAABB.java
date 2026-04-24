package redball.engine.editor;

public class EditorAABB {
    private float x;
    private float y;
    private float width;
    private float height;

    public EditorAABB(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains(float x, float y, float rotation) {
        float cx = this.x + width / 2.0f;
        float cy = this.y + height / 2.0f;

        float dx = x - cx;
        float dy = y - cy;

        float rad = (float) Math.toRadians(-rotation);

        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float localX = dx * cos - dy * sin + cx;
        float localY = dx * sin + dy * cos + cy;

        return localX >= this.x && localX <= this.x + width && localY >= this.y && localY <= this.y + height;
    }
}
