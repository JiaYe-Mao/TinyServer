package org.mao.tinyserver.io;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class ThreadPool {

    private static ThreadPoolExecutor threadPoolExecutor;

    static {
        threadPoolExecutor = new ThreadPoolExecutor(Server.serverConfig.getCorePoolSize(),
                Server.serverConfig.getMaxThread(),
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new ThreadPoolExecutor.DiscardPolicy());
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                threadPoolExecutor.shutdown();
            }
        });
    }

    public static void execute(Runnable task) {
        threadPoolExecutor.execute(task);
    }
}
