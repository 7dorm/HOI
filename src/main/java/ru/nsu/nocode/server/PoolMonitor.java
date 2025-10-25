package ru.nsu.nocode.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class PoolMonitor {
    private static final Logger logger = LoggerFactory.getLogger(PoolMonitor.class);
    private final ExecutorService pool;
    private final AtomicInteger activeTasks;
    private final AtomicInteger completedTasks;
    private final int maxThreads;
    private volatile boolean monitoring = true;
    
    public PoolMonitor(ExecutorService pool, AtomicInteger activeTasks, AtomicInteger completedTasks, int maxThreads) {
        this.pool = pool;
        this.activeTasks = activeTasks;
        this.completedTasks = completedTasks;
        this.maxThreads = maxThreads;
        
        // Запускаем мониторинг в отдельном потоке
        Thread monitorThread = new Thread(this::monitorLoop);
        monitorThread.setDaemon(true);
        monitorThread.setName("PoolMonitor");
        monitorThread.start();
    }
    
    private void monitorLoop() {
        while (monitoring) {
            try {
                Thread.sleep(10000); // Каждые 10 секунд
                
                int active = activeTasks.get();
                int completed = completedTasks.get();
                int queueSize = ((java.util.concurrent.ThreadPoolExecutor) pool).getQueue().size();
                
                logger.info("Pool status - Active: {}/{}, Completed: {}, Queue: {}", 
                    active, maxThreads, completed, queueSize);
                
                // Предупреждение при высокой загрузке
                if (active >= maxThreads * 0.8) {
                    logger.warn("High pool utilization: {}%", (active * 100) / maxThreads);
                }
                
                if (queueSize > maxThreads) {
                    logger.warn("Large queue size: {} tasks waiting", queueSize);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    public void stop() {
        monitoring = false;
    }
}
