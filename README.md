# RedBall Engine
> A 2D game engine being built from scratch in Java + OpenGL.<br>
> Actively in progress. Things will break. Things will change. That's the point.

![status](https://img.shields.io/badge/status-in%20progress-orange) ![java](https://img.shields.io/badge/language-Java-red) ![opengl](https://img.shields.io/badge/renderer-OpenGL%20%2F%20GLSL-blue)

---

![RedBall Engine](showcase/RedBall.png)

---

## ✅ What's Done

- [x] **OpenGL Renderer** — GLSL shaders, sprite rendering pipeline
- [x] **Batch Rendering** — All sprites drawn in a single draw call
- [x] **Entity-Component System (ECS)** — Entities composed of modular components
- [x] **Add Component Menu** — Searchable dropdown to attach components at runtime
- [x] **Custom Component Scripts** — Write your own components, they show up in the engine automatically
- [x] **Scene Save / Load** — Serialize and deserialize scenes to disk
- [x] **Prefab Support** — Create reusable prefabs for quick scene construction.

---

## 🚧 In Progress

- [ ] **Editor** — Runtime panel to view & edit entity properties, currently features project hierarchy and inspector
- [ ] **Packaging / Export** — Export projects as standalone JARs using .pak format for assets.

---

## 💭 Planned / Ideas

- [ ] **Tilemap Support** — Lay out levels with tiles
- [ ] Particle system
- [ ] Animation system (sprite sheets)
- [ ] Level editor improvements
- [ ] **Collision Callbacks** — `onCollisionEnter`, `onCollisionExit` events

---

## 🗂️ Project Structure

```
RedBall/
├── Redball/            # Engine source code (Java)
├── samples/            # Example game scenes demonstrating engine usage
│   └── src/
│       └── car/        # Example game project
│           ├── assets/ # Game assets (images, sounds, fonts)
│           ├── build/  # Compiled game builds
│           └── out/    # Compiled scripts/classes
├── shaders/            # GLSL vertex and fragment shaders
├── resources/          # Fonts, textures, and other assets
└── lib/                # External dependencies (e.g., LWJGL)
```

---

## 🧩 Writing a Custom Component

One of the core features, you can add your own logic as a component and it'll automatically appear in the **Add Component** menu inside the engine.

```java
public class MyComponent extends Component {
   @Serial
   private static final long serialVersionUID = 1L;

   public float speed = 5.0f;

    @Override
    public void start() {
        // runs once on scene start
    }

    @Override
    public void update(float dt) {
        // runs every frame
    }
}
```

---

## 📦 Pak System

During the build step, all game assets (sprites, scripts, scenes) are bundled into a single `.pak` file for distribution.

### File Layout

The `.pak` file is a flat binary format with a header followed by sequential asset chunks:

```
[4 bytes]  Number of chunks (int)
--- Repeated for each chunk ---
[2 bytes]  Asset name length
[N bytes]  Asset name 
[8 bytes]  Asset size in bytes 
[N bytes]  Raw asset data
```

### Index Building

At engine startup, `buildIndex()` scans the `.pak` file and populates a `HashMap<String, Long>` that maps each **asset path → byte offset** within the file. This means subsequent asset loads are O(1) seeks with no repeated scanning.

### Asset Retrieval

`getAsset(String path)` looks up the path in the index, seeks to the stored byte offset in the `.pak` file, reads the declared number of bytes, and returns the raw data. The caller is responsible for deserializing it (e.g. decoding a PNG, loading a scene, etc.).

### Example (Pseudocode)

```java
// Writing
PakWriter writer = new PakWriter("game.pak");
writer.add("assets/player.png", imageBytes);
writer.add("scenes/level1.scene", sceneBytes);
writer.write(); // serializes header + all chunks

// Reading
PakReader reader = new PakReader("game.pak");
reader.buildIndex();                          // scans offsets into HashMap
byte[] data = reader.getAsset("assets/player.png");
```
> **TODO:** Planned to add cache storage to reduce file reads from `getAssets` to increase performance
---

## 🛠️ Stack

| Layer | Tech |
|---|---|
| Language | Java |
| Rendering | OpenGL via LWJGL |
| Shaders | GLSL |
| GUI | [ImGui](https://github.com/SpaiR/imgui-java) |

---

## 🚀 Running It

1. Clone the repo
   ```bash
   git clone https://github.com/Abhineet1144/RedBall.git
   ```
2. Open in IntelliJ IDEA
3. Make sure `lib/` is on the classpath
4. Run the main class

---

> **Note:** This engine is a learning/passion project. It's not production-ready and the API will change as I figure things out. Contributions and feedback are welcome though!


*— [@Abhineet1144](https://github.com/Abhineet1144) & [@tejzs](https://github.com/Tejzs)*
