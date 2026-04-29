package cr.libre.firmador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class SingleInstanceManager {
    private Path baseDir;
    private Path lockFile;
    private Path commandFile;

    private FileChannel channel;
    private FileLock lock;
    private Timer commandMonitor;
    final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public SingleInstanceManager() {
        SettingsManager.getInstance().getAndCreateSettings();


        String officePath = SettingsManager.getInstance().getPath().toString();
        Path parentDir = Paths.get(officePath).getParent();
        String directorio = parentDir != null ? parentDir.toString() : officePath;
        LOG.info("SingleInstanceManager: officePath: " + directorio);
        this.baseDir =  Paths.get(directorio);
        this.lockFile = baseDir.resolve(".firmador.lock");
        this.commandFile = baseDir.resolve(".firmador.command");
    }

    public boolean tryLockOrRecover(Consumer<String> onCommand, String command) {
        try {
            Files.createDirectories(baseDir);
            channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            lock = channel.tryLock();
            if (lock == null || !lock.isValid()) {
                Files.writeString(commandFile, command, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                return false;
            }
            startCommandWatcher(onCommand);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void startCommandWatcher(Consumer<String> onCommand) {
        commandMonitor = new Timer(true);
        commandMonitor.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (Files.exists(commandFile)) {
                        String command = Files.readString(commandFile).trim();
                        Files.delete(commandFile);
                        if (!command.isEmpty()) {
                            onCommand.accept(command);
                        }
                    }
                } catch (IOException ignored) {}
            }
        }, 0, 2000);
    }

    public void release() {
        try {
            if (commandMonitor != null) commandMonitor.cancel();
            if (lock != null) lock.release();
            if (channel != null) channel.close();
            Files.deleteIfExists(lockFile);
            Files.deleteIfExists(commandFile);
        } catch (IOException ignored) {}
    }
}
