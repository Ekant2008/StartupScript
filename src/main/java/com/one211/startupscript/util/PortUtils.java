package com.one211.startupscript.util;

import java.io.IOException;
import java.net.ServerSocket;

public class PortUtils {
    private static final int MIN_PORT = 8081;
    private static final int MAX_PORT = 65535;
    public static int getUnusedPort() {
        int port;
        do {
            port = MIN_PORT + (int) (Math.random() * (MAX_PORT - MIN_PORT));
        } while (isPortInUse(port));
        return port;
    }
    private static boolean isPortInUse(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return false; // port is free
        } catch (IOException e) {
            return true; // port already in use
        }
    }
}

