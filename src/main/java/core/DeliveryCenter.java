package core;

import java.util.ArrayList;
import java.util.List;

import thread.RiderWorker;
import view.Logger;

public class DeliveryCenter {
    private final int riderCount;
    private final QueueManager queueManager;
    private final List<RiderWorker> riders;
    private final Logger logger;

    public DeliveryCenter(int riderCount, QueueManager queueManager, Logger logger) {
        this.riderCount = riderCount;
        this.queueManager = queueManager;
        this.riders = new ArrayList<>();
        this.logger = logger;
    }

    /**
     * 설정된 인원만큼 배달원 스레드를 생성하고 가동합니다.
     */
    public void startOperations() {
        for (int i = 1; i <= riderCount; i++) {
            // 배달원은 배달 전용 큐 하나만 주시하면 됩니다.
            RiderWorker rider = new RiderWorker(i, queueManager.getDeliveryQueue(), logger);
            Thread thread = new Thread(rider, "Rider-" + i);
            thread.start();
            riders.add(rider);
        }
    }

    /**
     * 대시보드 표시를 위해 모든 배달원의 현재 상태 리스트를 반환합니다.
     */
    public List<RiderWorker> getRiderStatus() {
        return riders;
    }
}