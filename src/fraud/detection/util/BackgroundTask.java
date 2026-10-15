package fraud.detection.util;

import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

/**
 * Runs slow work (database calls, model training) on a BACKGROUND THREAD so
 * windows never freeze.
 *   doInBackground() -> worker thread           (database / ML)
 *   done()           -> Swing UI thread (EDT)   (update labels, tables, charts)
 * <T> is the result type, so one helper works for every kind of result.
 */
public final class BackgroundTask {

    private BackgroundTask() {
    }

    public static <T> void run(Component parent, Callable<T> task, Consumer<T> onSuccess) {
        run(parent, task, onSuccess, null);
    }

    /** onFinally (may be null) always runs afterwards on the UI thread, e.g. to re-enable a button. */
    public static <T> void run(Component parent, Callable<T> task, Consumer<T> onSuccess, Runnable onFinally) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    MessageUtil.error(parent, e.getCause());
                } finally {
                    if (onFinally != null) {
                        onFinally.run();
                    }
                }
            }
        }.execute();
    }
}
