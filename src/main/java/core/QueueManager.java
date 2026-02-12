package core;

import java.util.EnumMap;
import java.util.Map;

import model.MenuItem;
import model.OrderQueue;

public class QueueManager {
    // 메뉴별 큐를 저장하는 맵 (EnumMap은 메모리 효율이 높음)
    private final Map<MenuItem, OrderQueue> menuQueues;
    // 모든 조리가 완료된 주문이 들어가는 큐
    private final OrderQueue deliveryQueue;

    public QueueManager(int menuQueueSize, int deliveryQueueSize) {
        this.menuQueues = new EnumMap<>(MenuItem.class);
        this.deliveryQueue = new OrderQueue("deliveryQueue", deliveryQueueSize);

        // MenuItem Enum에 정의된 모든 메뉴에 대해 각각의 큐를 생성
        for (MenuItem item : MenuItem.values()) {
            menuQueues.put(item, new OrderQueue(item.getName().toLowerCase() + "Queue", menuQueueSize));
        }
    }

    /**
     * 특정 메뉴에 해당하는 큐를 반환
     */
    public OrderQueue getMenuQueue(MenuItem item) {
        return menuQueues.get(item);
    }

    /**
     * 모든 메뉴 큐의 맵을 반환 (CookerWorker의 RR 스케줄링에서 사용)
     */
    public Map<MenuItem, OrderQueue> getAllMenuQueues() {
        return menuQueues;
    }

    /**
     * 배달 대기열을 반환
     */
    public OrderQueue getDeliveryQueue() {
        return deliveryQueue;
    }

    /**
     * 특정 메뉴 큐의 현재 대기 수량을 반환 (대시보드용)
     */
    public int getQueueSize(MenuItem item) {
        return menuQueues.get(item).size();
    }
}