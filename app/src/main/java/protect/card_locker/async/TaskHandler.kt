package protect.card_locker.async

import android.os.Handler
import android.os.Looper
import java.util.LinkedList
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * AsyncTask has been deprecated so this provides very rudimentary compatibility without
 * needing to redo too many Parts.
 *
 *
 * However this is a much, much more cooperative Behaviour than before so
 * the callers need to ensure we do NOT rely on forced cancellation and feed less into the
 * ThreadPools so we don't OOM/Overload the Users device
 *
 *
 * This assumes single-threaded callers.
 */
class TaskHandler {
    enum class TYPE {
        BARCODE,
        IMPORT,
        EXPORT
    }

    var executors: HashMap<TYPE, ThreadPoolExecutor> = generateExecutors()

    private val taskList = HashMap<TYPE, LinkedList<Future<*>>>()

    private val uiHandler = Handler(Looper.getMainLooper())

    private fun generateExecutors(): HashMap<TYPE, ThreadPoolExecutor> {
        val initExecutors = HashMap<TYPE, ThreadPoolExecutor>()
        for (type in TYPE.entries) {
            replaceExecutor(initExecutors, type, false, false)
        }
        return initExecutors
    }

    /**
     * Replaces (or initializes) an Executor with a clean (new) one
     *
     * @param executors Map Reference
     * @param type      Which Queue
     * @param flushOld  attempt shutdown
     * @param waitOnOld wait for Termination
     */
    private fun replaceExecutor(
        executors: HashMap<TYPE, ThreadPoolExecutor>,
        type: TYPE,
        flushOld: Boolean,
        waitOnOld: Boolean
    ) {
        val oldExecutor = executors[type]
        if (oldExecutor != null) {
            if (flushOld) {
                oldExecutor.shutdownNow()
            }
            if (waitOnOld) {
                try {
                    oldExecutor.awaitTermination(5, TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
        executors[type] =
            Executors.newCachedThreadPool() as ThreadPoolExecutor
    }

    /**
     * Queue a Pseudo-AsyncTask for execution
     *
     * @param type     Queue
     * @param callable PseudoAsyncTask
     */
    fun executeTask(type: TYPE, callable: CompatCallable<*>) {
        val runner = Runnable {
            try {
                // Run on the UI Thread
                uiHandler.post { callable.onPreExecute() }

                // Background
                val result = callable.call()

                // Post results on UI Thread so we can show them
                uiHandler.post {
                    callable.onPostExecute(result)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        var list = taskList[type]

        if (list == null) {
            list = LinkedList()
        }

        val executor = executors[type]

        if (executor != null) {
            val task = executor.submit(runner)
            // Test Queue Cancellation:
            // task.cancel(true);
            list.push(task)
            taskList[type] = list
        }
    }

    /**
     * This will attempt to cancel a currently running list of Tasks
     * Useful to ignore scheduled tasks - but not able to hard-stop tasks that are running
     *
     * @param type                    Which Queue to target
     * @param forceCancel             attempt to close the Queue and force-replace it after
     * @param waitForFinish           wait and return after the old executor finished. Times out after 5s
     * @param waitForCurrentlyRunning wait before cancelling tasks. Useful for tests.
     */
    fun flushTaskList(
        type: TYPE,
        forceCancel: Boolean,
        waitForFinish: Boolean,
        waitForCurrentlyRunning: Boolean
    ) {
        // Only used for Testing
        if (waitForCurrentlyRunning) {
            val oldExecutor = executors[type]

            if (oldExecutor != null) {
                try {
                    oldExecutor.awaitTermination(5, TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }

        // Attempt to cancel known Tasks and clean the List
        var tasks = taskList[type]
        if (tasks != null) {
            for (task in tasks) {
                if (!task.isDone || !task.isCancelled) {
                    // Interrupt any Task we can
                    task.cancel(true)
                }
            }
        }
        tasks = LinkedList()
        taskList[type] = tasks

        if (forceCancel || waitForFinish) {
            val oldExecutor = executors[type]

            if (oldExecutor != null) {
                if (forceCancel) {
                    if (waitForFinish) {
                        try {
                            oldExecutor.awaitTermination(5, TimeUnit.SECONDS)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                    oldExecutor.shutdownNow()
                    replaceExecutor(executors, type, true, false)
                } else {
                    try {
                        oldExecutor.awaitTermination(5, TimeUnit.SECONDS)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}