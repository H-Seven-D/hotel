package com.homestay.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.dev.auto-start-frontend",
        havingValue = "true",
        matchIfMissing = true
)
public class DevFrontendLauncher {

    @Value("${server.port:8080}")
    private int serverPort;

    private volatile Process frontendProcess;
    private volatile boolean shutdownHookRegistered;

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void launchFrontend() {
        if (frontendProcess != null && frontendProcess.isAlive()) {
            return;
        }

        Path frontendDir = findFrontendDirectory();
        if (frontendDir == null) {
            log.warn("未找到 frontend 目录，跳过前端自动启动");
            return;
        }
        Path viteEntry = frontendDir.resolve("node_modules/vite/bin/vite.js");
        if (!Files.isRegularFile(viteEntry)) {
            log.warn("前端依赖尚未安装，请在 {} 执行 npm install", frontendDir);
            return;
        }

        try {
            int port = findAvailablePort();
            String nodeExecutable = findNodeExecutable();
            List<String> command = List.of(
                    nodeExecutable,
                    viteEntry.toString(),
                    "--host", "127.0.0.1",
                    "--port", String.valueOf(port),
                    "--strictPort",
                    "--open"
            );
            ProcessBuilder builder = new ProcessBuilder(command)
                    .directory(frontendDir.toFile())
                    .redirectErrorStream(true)
                    .inheritIO();
            builder.environment().put(
                    "VITE_PROXY_TARGET",
                    "http://127.0.0.1:" + serverPort
            );
            frontendProcess = builder.start();
            registerShutdownHook();
            log.info("前端开发服务器已启动：http://127.0.0.1:{}", port);
        } catch (Exception exception) {
            log.warn("自动启动前端失败：{}", exception.getMessage());
        }
    }

    private Path findFrontendDirectory() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                current.resolve("frontend"),
                current.resolve("../frontend").normalize()
        );
        return candidates.stream()
                .filter(Files::isDirectory)
                .findFirst()
                .orElse(null);
    }

    private int findAvailablePort() throws IOException {
        for (int port = 5173; port <= 5200; port++) {
            try (ServerSocket ignored = new ServerSocket(
                    port, 1, InetAddress.getByName("127.0.0.1"))) {
                return port;
            } catch (IOException ignored) {
                // Try the next local development port.
            }
        }
        throw new IOException("5173-5200 范围内没有可用前端端口");
    }

    private String findNodeExecutable() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return "node";
        }
        try {
            Process process = new ProcessBuilder("where.exe", "node")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(
                    process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            return output.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .findFirst()
                    .orElse("node");
        } catch (Exception ignored) {
            return "node";
        }
    }

    private void registerShutdownHook() {
        if (shutdownHookRegistered) {
            return;
        }
        shutdownHookRegistered = true;
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopFrontend));
    }

    @PreDestroy
    public synchronized void stopFrontend() {
        Process process = frontendProcess;
        if (process == null || !process.isAlive()) {
            return;
        }
        process.toHandle().descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        frontendProcess = null;
    }
}
