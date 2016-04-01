package concurrent;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WorkQueue to manage threads
 * @author jaketarnow
 *
 */
public class WorkQueue {

    private static final int defaultThreads = 16;//100 default threads
    private final PoolWorker[] threads;//array list of pool workers
    private final LinkedList<Runnable> queue;//linked list of runnable objects
    private volatile boolean shutdown;//volatile boolean shutdown so it's visible to all reads
    private final AtomicInteger count;//create counter for WorkQueue

    /**
     * Construct a WorkQueue with 10 default workers.
     * Construct a counter initialized at 0
     */
    public WorkQueue() {
        this(defaultThreads);
    }

    /**
     * Construct the WorkQueue that will add threads to the pool Worker
     * once added then it is initialized
     * @param nThreads
     */
    public WorkQueue(int nThreads) {
        this.count = new AtomicInteger();
        queue = new LinkedList<>();
        threads = new PoolWorker[nThreads];

        //for loop for adding threads to the PoolWorker
        for (int i = 0; i < nThreads; i++) {
            threads[i] = new PoolWorker();
            threads[i].start();
        }
    }

    /**
     * Create an increaseCounter method
     * Increment the count when runnable is initialized per thread
     */

    public synchronized void increaseCounter() {
        count.incrementAndGet();
    }

    /**
     * Create a decreaseCounter method
     * Decrement the count when runnable is finished
     */
    public synchronized void decreaseCounter() {
        count.decrementAndGet();
        if (count.get() == 0) {
            this.notifyAll();
        }
    }

    /**
     * Create a waitForComplete method
     * While the count is greater than 0, then wait
     */
    public synchronized void waitForComplete() {
        while (count.get() > 0) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Execute a new Runnable job.
     * If the queue is not shutdown, then add a runnable object and notify all
     * @param r
     */
    public void execute(Runnable r) {
        synchronized(queue) {
            if (!shutdown) {
                queue.addLast(r);
                //in case things are still on queue when shutdown
                queue.notifyAll();
            }
        }
    }

    /**
     * Implement a private PoolWorker class that extends the Thread class
     * Referenced http://www.ibm.com/developerworks/library/j-jtp0730/
     * Contains a runnable method
     */
    private class PoolWorker extends Thread {
        public void run() {
            boolean complete = false;
            while (!complete) {
                //implement runnable object r inside while loop
                Runnable r = null;
                //must use synchronized when checking if queue is empty
                synchronized(queue) {
                    if (shutdown && queue.isEmpty()) {
                        complete = true;//will then break out of while loop
                    }
                    //while queue is empty try wait()
                    while (!shutdown && queue.isEmpty()) {
                        //blocking : waiting for jobs to come in
                        try {
                            queue.wait();
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                    }
                    if (!queue.isEmpty()) {
                        r = queue.removeFirst();
                    }
                }
                //check if r is null, if so then it breaks out of loop
                if (r != null) {
                    //if its not null then it tries to run
                    try {
                        r.run();
                    } catch (RuntimeException re) {
                        //proceed instead of losing thread
                        re.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Stop accepting new jobs.
     * This method should not block until work is complete.
     */
    public void shutdown() {
        shutdown = true;//set shutdown to true to stop accepting jobs
        if (count.get() == 0) {
            synchronized(queue) {
                queue.notifyAll();//notify all threads of shutdown
            }
        }
    }

    /**
     * Block until all jobs in the queue are complete.
     */
    public void awaitTermination() {
        for (Thread thread : threads) {
            //for a thread contained in threads, try join
            try {
                thread.join();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }
}