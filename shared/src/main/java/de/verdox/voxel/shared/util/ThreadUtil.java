package de.verdox.voxel.shared.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadUtil {
    public static ThreadFactory createFactoryForName(String name, boolean daemon) {
        return new ThreadFactory() {
            private final AtomicInteger idx = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, name + " - " + idx.getAndIncrement());
                t.setDaemon(daemon);
                return t;
            }
        };
    }
}
