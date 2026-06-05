package com.nexuschat.integration;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Skips integration tests when PostgreSQL or Redis are not reachable (e.g. local dev without Docker).
 * CI provides both services via GitHub Actions service containers.
 */
public final class TestInfrastructure {

    private TestInfrastructure() {
    }

    public static boolean isAvailable() {
        String pgHost = System.getenv().getOrDefault("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/nexuschat");
        String redisHost = System.getenv().getOrDefault("SPRING_DATA_REDIS_HOST", "localhost");
        int redisPort = Integer.parseInt(System.getenv().getOrDefault("SPRING_DATA_REDIS_PORT", "6379"));

        int pgPort = 5432;
        String host = "localhost";
        if (pgHost.contains("://")) {
            String withoutScheme = pgHost.substring(pgHost.indexOf("://") + 3);
            String hostPort = withoutScheme.contains("/")
                    ? withoutScheme.substring(0, withoutScheme.indexOf('/'))
                    : withoutScheme;
            if (hostPort.contains(":")) {
                String[] parts = hostPort.split(":");
                host = parts[0];
                pgPort = Integer.parseInt(parts[1]);
            } else {
                host = hostPort;
            }
        }

        return isPortOpen(host, pgPort) && isPortOpen(redisHost, redisPort);
    }

    private static boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
