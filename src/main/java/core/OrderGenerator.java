package core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import model.MenuItem;
import model.Order;
import view.Logger;

public class OrderGenerator implements Runnable {
    private static final int MAX_RECENT = 5;
    private final QueueManager queueManager;
    private final AtomicInteger orderIdCounter = new AtomicInteger(100); // 100번부터 시작
    private final Random random = new Random();
    private final LinkedList<String> recentOrders = new LinkedList<>();
    private boolean running = true;

    public OrderGenerator(QueueManager queueManager) {
        this.queueManager = queueManager;
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                // 1. 주문 생성
                Order newOrder = createRandomOrder();
                
                // 2. 각 메뉴 큐에 주문 분배 (Fork)
                for (MenuItem item : newOrder.getItems()) {
                    queueManager.getMenuQueue(item).push(newOrder);
                }

                // 3. 로그 기록 + 최근 주문 추가
                String summary = formatOrder(newOrder);
                synchronized (recentOrders) {
                    recentOrders.addFirst(summary);
                    if (recentOrders.size() > MAX_RECENT) {
                        recentOrders.removeLast();
                    }
                }
                Logger.log("신규 주문 접수: #" + newOrder.getOrderId() +
                                " (" + newOrder.getItems().size() + "개 메뉴)");

                // 4. 다음 주문까지 무작위 대기 (2초 ~ 5초)
                Thread.sleep(500 + random.nextInt(500));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private Order createRandomOrder() {
        int id = orderIdCounter.incrementAndGet(); // AtomicInteger method
        
        // 무작위로 1~3개의 메뉴 선택
        int itemCount = random.nextInt(3) + 1;
        List<MenuItem> selectedItems = new ArrayList<>();
        MenuItem[] allMenus = MenuItem.values();
        
        for (int i = 0; i < itemCount; i++) {
            selectedItems.add(allMenus[random.nextInt(allMenus.length)]);
        }

        // 무작위 주소 설정
        String[] addresses = {"강남구 역삼동", "서초구 서초동", "송파구 잠실동", "마포구 망원동", "성동구 성수동"};
        String address = addresses[random.nextInt(addresses.length)];

        return new Order(id, selectedItems, address);
    }

    private String formatOrder(Order order) {
        StringBuilder items = new StringBuilder();
        for (MenuItem item : order.getItems()) {
            if (items.length() > 0) items.append(", ");
            items.append(item.getName());
        }
        return String.format("#%d [%s] → %s", order.getOrderId(), items, order.getAddress());
    }

    public List<String> getRecentOrders() {
        synchronized (recentOrders) {
            return new ArrayList<>(recentOrders);
        }
    }
}