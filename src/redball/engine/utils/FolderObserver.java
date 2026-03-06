package redball.engine.utils;

import org.apache.commons.io.monitor.*;
import java.io.File;

public class FolderObserver {

    private static FileAlterationMonitor monitor;

    public static void start() throws Exception {
        File directory = new File("example/assets/scripts/");

        FileAlterationObserver observer = new FileAlterationObserver(directory);

        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileChange(File file) {
                System.out.println("File changed: " + file.getName());
                ScriptManager.scheduleReload(file); // enqueue only — no reload here
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