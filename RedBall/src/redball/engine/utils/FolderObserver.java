package redball.engine.utils;

import org.apache.commons.io.monitor.*;
import redball.engine.editor.EditorLayer;
import redball.engine.scene.AssetManager;

import java.io.File;

public class FolderObserver {

    private static FileAlterationMonitor monitor;

    public static void start() throws Exception {
        File directory = new File(AssetManager.getINSTANCE().getScriptDirectory());
        System.out.println(directory.getPath());

        FileAlterationObserver observer = new FileAlterationObserver(directory);

        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileChange(File file) {
                System.out.println("File changed: " + file.getName());
                ScriptManager.scheduleReload(file);
            }

            @Override
            public void onFileCreate(File file) {
                try {
                    ScriptManager.compileAll(AssetManager.getINSTANCE().getScriptDirectory());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onFileDelete(File file) {
                try {
                    ScriptManager.compileAll(AssetManager.getINSTANCE().getScriptDirectory());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        long pollingInterval = 1000;
        monitor = new FileAlterationMonitor(pollingInterval, observer);
        monitor.start();
        System.out.println("Watching: " + directory.getAbsolutePath());
    }

    public static void stop() throws Exception {
        if (monitor != null) {
            monitor.stop();
        }
    }
}