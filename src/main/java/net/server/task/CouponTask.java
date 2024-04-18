package net.server.task;

import net.server.Server;
import tools.FilePrinter;

import java.sql.SQLException;

/**
 * Thread responsible for maintaining coupons EXP & DROP effects active
 */
public class CouponTask implements Runnable {
    @Override
    public void run() {
        try {
            Server.getInstance().updateActiveCoupons();
            Server.getInstance().commitActiveCoupons();
        } catch (SQLException sqle) {
            FilePrinter.printError(FilePrinter.EXCEPTION_CAUGHT, "Unexpected SQL error: " + sqle.getMessage());
        }
    }
}
