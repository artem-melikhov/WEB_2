import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class WorkerPool {
    private static volatile boolean running = true;

    private static class Worker implements Runnable {
        final int number;
        final int port;
        final Path directory;
        Process process;

        Worker(int number, int port, Path directory) {
            this.number = number;
            this.port = port;
            this.directory = directory;
        }

        @Override
        public void run() {
            Path log = directory.resolve("java-" + number + ".log");
            Path pidFile = directory.resolve("java-" + number + ".pid");

            while (running) {
                Process child = null;
                try {
                    String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
                    ProcessBuilder builder = new ProcessBuilder(java,
                            "-Dfile.encoding=UTF-8", "-DFCGI_PORT=" + port,
                            "-cp", System.getProperty("java.class.path"), "Main");
                    builder.redirectErrorStream(true);
                    builder.redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()));

                    child = builder.start();
                    synchronized (this) {
                        if (!running) {
                            return;
                        }
                        process = child;
                    }
                    Files.writeString(pidFile, Long.toString(child.pid()));
                    System.out.println("Java " + number + " запущена на порту " + port + ", PID " + child.pid());

                    int exitCode = child.waitFor();
                    if (running) {
                        System.out.println("Java " + number + " завершилась (код " + exitCode + "), перезапуск через секунду");
                    }
                } catch (IOException error) {
                    System.err.println("Не удалось запустить Java " + number + ": " + error.getMessage());
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    return;
                } finally {
                    if (child != null && child.isAlive()) {
                        stopProcess(child);
                    }
                    synchronized (this) {
                        process = null;
                    }
                    try {
                        Files.deleteIfExists(pidFile);
                    } catch (IOException error) {
                        System.err.println("Не удалось удалить " + pidFile + ": " + error.getMessage());
                    }
                }

                if (running) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

        void stop() {
            Process child;
            synchronized (this) {
                child = process;
            }
            if (child == null) {
                return;
            }
            stopProcess(child);
        }

        private void stopProcess(Process child) {
            child.destroy();
            try {
                if (!child.waitFor(3, TimeUnit.SECONDS)) {
                    child.destroyForcibly();
                    child.waitFor(3, TimeUnit.SECONDS);
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Использование: WorkerPool <первый FastCGI-порт> <папка журналов>");
        }

        int firstPort = Integer.parseInt(args[0]);
        if (firstPort < 1 || firstPort >= 65535) {
            throw new IllegalArgumentException("Нужны два соседних допустимых порта");
        }

        Path directory = Path.of(args[1]);
        Files.createDirectories(directory);
        try (ServerSocket firstSocket = new ServerSocket(firstPort);
             ServerSocket secondSocket = new ServerSocket(firstPort + 1)) {
            // Проверяем оба порта до запуска процессов.
        }
        Worker first = new Worker(1, firstPort, directory);
        Worker second = new Worker(2, firstPort + 1, directory);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
            first.stop();
            second.stop();
        }));

        Thread firstWatcher = new Thread(first);
        Thread secondWatcher = new Thread(second);
        firstWatcher.start();
        secondWatcher.start();
        firstWatcher.join();
        secondWatcher.join();
    }
}
