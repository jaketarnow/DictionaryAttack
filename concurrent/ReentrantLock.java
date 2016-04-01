import java.util.HashMap;

/**
 * @author jaketarnow
 * A read/write lock that allows multiple readers, disallows multiple writers, and allows a writer to
 * acquire a read lock while holding the write lock.
 *
 */
public class ReentrantLock {
    //can have infinite number of readers, but only 1 active writer at a given time
    private HashMap<Thread, Integer> reads;
    private Thread activeWriter;//keep track of activeWriter at given time
    private Integer receiveWrite;//keep track of number of requests to write

    /**
     * Construct a new ReentrantLock.
     */
    public ReentrantLock() {
        this.activeWriter = null;
        this.reads = new HashMap<Thread, Integer>();
        this.receiveWrite = 0;
    }

    /**
     * Returns true if the invoking thread holds a read lock.
     * @return
     */
    public synchronized boolean hasRead() {
        Thread currentThread = Thread.currentThread();
        if (currentThread != null) {//if the current thread has something
            //then check to see if its in the map and if its greater than 0
            if(reads.containsKey(currentThread) && reads.get(currentThread) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the invoking thread holds a write lock.
     * @return
     */
    public synchronized boolean hasWrite() {
        Thread currentThread = Thread.currentThread();
        //if the current thread is the active writer
        if (currentThread == activeWriter) {
            return true;
        }
        return false;
    }

    /**
     * Non-blocking method that attempts to acquire the read lock.
     * Returns true if successful.
     * @return
     */
    public synchronized boolean tryLockRead() {
        Thread currentThread = Thread.currentThread();
        if (activeWriter == null || hasWrite()) {
            //if current thread is in map then increment the value
            if (reads.containsKey(currentThread)) {
                reads.put(currentThread, reads.get(currentThread) + 1);
                return true;
                //if not then add it with a value of 1 (new thread)
            } else if(!reads.containsKey(currentThread)){
                reads.put(currentThread, 1);
                return true;
            }
        }
        return false;
    }

    /**
     * Non-blocking method that attempts to acquire the write lock.
     * Returns true if successful.
     * @return
     */
    public synchronized boolean tryLockWrite() {
        //brute force to get write lock
        Thread currentThread = Thread.currentThread();
        //if there are any readers then no write lock can be granted
        if (reads.size() != 0) {
            return false;
        }
        //if current thread is already the writer, increment count
        if (currentThread == activeWriter) {
            receiveWrite += 1;
            return true;
        }
        //if activeWriter is null then set it as currentThread and increment
        if (activeWriter == null && !reads.containsKey(currentThread)) {
            activeWriter = currentThread;
            receiveWrite +=1;
            return true;
        }
        return false;
    }

    /**
     * Blocking method that will return only when the read lock has been
     * acquired.
     */
    public synchronized void lockRead() {
        //while you don't have the read lock, wait for it
        while (tryLockRead() == false) {
            //try having the currentThread wait
            try {
                this.wait();//blocking method that returns when lock is acquired
            } catch (IllegalMonitorStateException | InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    /**
     * Releases the read lock held by the calling thread. Other threads may continue
     * to hold a read lock.
     */
    public synchronized void unlockRead() {
        Thread currentThread = Thread.currentThread();
        //if you are the reader, then remove you
        if (reads.containsKey(currentThread)) {
            reads.put(currentThread, reads.get(currentThread) - 1);
        }
        if (reads.get(currentThread) <= 0) {
            reads.remove(currentThread);
        }
        //remember to notify all that the read lock is available
        this.notifyAll();
    }

    /**
     * Blocking method that will return only when the write lock has been
     * acquired.
     */
    public synchronized void lockWrite() {
        //while you are the single writer or there are no writers
        while (tryLockWrite() == false) {
            try {
                this.wait();
            } catch (IllegalMonitorStateException | InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    /**
     * Releases the write lock held by the calling thread. The calling thread may continue to hold
     * a read lock.
     */
    public synchronized void unlockWrite() {
        Thread currentThread = Thread.currentThread();
        //if thread holds write lock and no more requests, set writer to null
        if (currentThread == activeWriter) {
            --receiveWrite;
        }
        if (receiveWrite == 0) {
            activeWriter = null;
        }
        //remember to notify all that the write lock is available
        this.notifyAll();
    }
}