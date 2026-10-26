import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class WorkerPool {
    private static Process startWorker(int number, int port, Path directory) throws IOException {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        ProcessBuilder builder = new ProcessBuilder(java,
                "-Dfile.encoding=UTF-8", "-DFCGI_PORT=" + port,
                "-cp", System.getProperty("java.class.path"), "Main");
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(directory.resolve("java-" + number + ".log").toFile()));

        Process process = builder.start();
        try {
            Files.writeString(directory.resolve("java-" + number + ".pid"), Long.toString(process.pid()));
        } catch (IOException error) {
            stop(process);
            throw error;
        }
        System.out.println("Java " + number + " запущена на порту " + port + ", PID " + process.pid());
        return process;
    }

    private static void stop(Process process) {
        if (process == null || !process.isAlive()) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(3, TimeUnit.SECONDS);
            }
        } catch (InterruptedException error) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    private static void deletePidFiles(Path directory) {
        for (int number = 1; number <= 2; number++) {
            try {
                Files.deleteIfExists(directory.resolve("java-" + number + ".pid"));
            } catch (IOException error) {
                System.err.println("Не удалось удалить PID-файл Java " + number + ": " + error.getMessage());
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
            // Проверяем, что оба порта свободны.
        }

        Process first = null;
        Process second = null;
        try {
            first = startWorker(1, firstPort, directory);
            second = startWorker(2, firstPort + 1, directory);

            Process firstWorker = first;
            Process secondWorker = second;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                stop(firstWorker);
                stop(secondWorker);
                deletePidFiles(directory);
            }));

            CompletableFuture.anyOf(first.onExit(), second.onExit()).join();
            throw new IllegalStateException("Один из процессов Java завершился. Запустите WorkerPool снова.");
        } finally {
            stop(first);
            stop(second);
            deletePidFiles(directory);
        }
    }
}
