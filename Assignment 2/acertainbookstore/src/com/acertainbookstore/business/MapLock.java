package com.acertainbookstore.business;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by steffenkarlsson on 27/11/14.
 */
public class MapLock<K, V> extends HashMap<K, V> {

    private Lock lock = new ReentrantLock();

    public void dirtyWriteLock() {
        lock.lock();
    }

    public void dirtyWriteUnlock() {
        lock.unlock();
    }
}
