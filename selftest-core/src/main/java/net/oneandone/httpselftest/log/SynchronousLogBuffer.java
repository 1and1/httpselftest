package net.oneandone.httpselftest.log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

public class SynchronousLogBuffer {

    private static final int MAX_SIZE = 200;

    private Deque<SelftestEvent> deque;

    private boolean linesWereDropped;

    public SynchronousLogBuffer() {
        deque = new ArrayDeque<>(MAX_SIZE);
        linesWereDropped = false;
    }

    public synchronized void add(SelftestEvent e) {
        if (deque.size() >= MAX_SIZE) {
            deque.removeFirst();
            linesWereDropped = true;
        }
        deque.add(e);
    }

    public synchronized LogSnapshot snapshot() {
        LogSnapshot result = new LogSnapshot(new ArrayList<>(deque), linesWereDropped);
        deque.clear();
        linesWereDropped = false;
        return result;
    }

}
