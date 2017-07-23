package nickel;

import nickel.util.PrintUtil;

/**
 * Created by Murray on 23/07/2017
 */
public abstract class AbstractTask implements Runnable {

    protected final PrintUtil printUtil;
    private boolean isCancelled = false;

    public AbstractTask(PrintUtil printUtil) {
        this.printUtil = printUtil;
    }

    @Override
    public final void run() {
        try {
            runTask();
        } catch (Exception e) {
            printUtil.printAndLog(e);
        }
    }

    protected abstract void runTask() throws Exception;

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }
}
