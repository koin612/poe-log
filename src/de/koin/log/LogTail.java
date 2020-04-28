package de.koin.log;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.SubmissionPublisher;

public class LogTail implements Runnable, Closeable {

    private File file;
    private RandomAccessFile raFile;
    private long lastKnownPosition = 0;

    private SubmissionPublisher<String> publisher;

    public LogTail(File file, SubmissionPublisher<String> publisher) throws FileNotFoundException {
        if (!file.exists())
            throw new FileNotFoundException();

        this.file = file;
        this.publisher = publisher;

        try {
            raFile = new RandomAccessFile(file, "r");
            raFile.seek(file.length());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();

            file.getParentFile().toPath().register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY);

            WatchKey watchKey;
            while ((watchKey = watchService.take()) != null) {
                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    if (!event.context().toString().equals(file.getName()))
                        continue;

                    if (file.length() > lastKnownPosition) {
                        String line;
                        while ((line = raFile.readLine()) != null) {
                            for (String current : line.split(System.lineSeparator())) {
                                if (current.isBlank())
                                    continue;
                                publisher.submit(current);
                            }
                        }
                        lastKnownPosition = raFile.getFilePointer();
                    }
                }
                watchKey.reset();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        if (raFile != null)
            raFile.close();
    }
}
