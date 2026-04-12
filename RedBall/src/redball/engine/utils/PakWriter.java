package redball.engine.utils;

import redball.engine.scene.AssetManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class PakWriter {
    private static final ArrayList<String> files = new ArrayList<>();
    private static Map<String, Long> index = new HashMap<>();

    private static void listFile(String path) {
        File root = new File(path);
        for (File file : root.listFiles()) {
            if (file.getName().equals("build")) continue;
            if (file.isDirectory()) {
                listFile(file.getPath());
            } else {
                files.add(file.getPath());
            }
        }
    }

    public static void writePak(String path) throws Exception {
        listFile(path);
        System.out.println(files);
        long offset = 0;
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(AssetManager.getINSTANCE().getBuildDirectory() + "assets.pak")))) {
            // Number of chunks
            out.writeInt(files.size());
            offset += 4;
            for (String filePath : files) {
                String name = filePath;
                byte[] data = Files.readAllBytes(Path.of(filePath));

                out.writeInt(name.length());
                out.writeBytes(name);
                out.writeInt(data.length);
                out.write(data);

                offset += 4;
                offset += name.length();
                index.put(name, offset);
                offset += 4;
                offset += data.length;
            }
        }
        System.out.println(index);
    }

    public static byte[] getAsset(String file) {
        Long offset = index.get(file);
        try (RandomAccessFile raf = new RandomAccessFile("/home/tejas/Projects/RedBall/samples/src/car/build/assets.pak", "r")) {
            raf.seek(offset);
            int datalen = raf.readInt();
            byte[] data = new byte[datalen];
            raf.readFully(data);
            return data;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void buildIndex() {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream("/home/tejas/Projects/RedBall/samples/src/car/build/assets.pak")))) {

            int chunkCount = in.readInt();
            long offset = 4;

            for (int i = 0; i < chunkCount; i++) {
                // Read name
                int nameLen = in.readInt();
                byte[] nameBytes = new byte[nameLen];
                in.readFully(nameBytes);
                String name = new String(nameBytes);

                // Read data
                offset += 4 + nameLen;
                index.put(name, offset);

                int dataLen = in.readInt();
                offset += 4 + dataLen;
                in.skipBytes(dataLen);

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Long> getIndex() {
        return index;
    }
}