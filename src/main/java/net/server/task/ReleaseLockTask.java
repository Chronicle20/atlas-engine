package net.server.task;

import net.server.audit.LockCollector;

/**
 * Thread responsible for expiring locks signalized for dispose.
 */
public class ReleaseLockTask implements Runnable {
    @Override
    public void run() {
        LockCollector.getInstance().runLockCollector();
    }
}
