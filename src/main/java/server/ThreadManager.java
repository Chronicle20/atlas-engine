package server;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadManager {
    private static final ThreadManager instance = new ThreadManager();
    private ThreadPoolExecutor tpe;

    private ThreadManager() {
    }

    public static ThreadManager getInstance() {
        return instance;
    }

    public void newTask(Runnable r) {
        tpe.execute(r);
    }

    public void start() {
        RejectedExecutionHandler reh = new RejectedExecutionHandlerImpl();
        ThreadFactory tf = Executors.defaultThreadFactory();

        tpe = new ThreadPoolExecutor(20, 1000, 77, TimeUnit.SECONDS, new ArrayBlockingQueue<>(50), tf, reh);
    }

    public void stop() {
        tpe.shutdown();
        try {
            tpe.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException ie) {
        }
    }

    private static class RejectedExecutionHandlerImpl implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            Thread t = new Thread(r);
            t.start();
        }

    }

}
