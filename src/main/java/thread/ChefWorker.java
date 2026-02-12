package thread;

import core.QueueManager;
import model.MenuItem;
import model.Order;
import model.OrderQueue;
import view.Logger;

public class ChefWorker implements Runnable {
	
    private final int id;
    private final QueueManager queueManager;
    private volatile boolean running = true;
    
    // 현재 상태 (대시보드용)
    private Order currentOrder;
    private MenuItem currentMenu;
    private int progress;

    public ChefWorker(int id, QueueManager queueManager) {
        this.id = id;
        this.queueManager = queueManager;
    }
    
    @Override
    public void run() {
        while (running) {
            try {
                // 1. 메뉴 큐 순회하며 일감 찾기
                Order order = findWork();
                
                if (order == null) {
                    Thread.sleep(100);  // 일감 없으면 잠깐 대기
                    continue;
                }
                
                // 2. 조리 수행
                cook();
                
                // 3. 조리 완료 처리
                if (currentOrder.addItemComplete()) {
                    // 주문의 모든 메뉴 완료 → 배달 큐로
                    queueManager.getDeliveryQueue().push(currentOrder);
                    Logger.log("[요리사#" + id + "] 주문#" + currentOrder.getOrderId() + " 조리 완료 → 배달 큐");
                }
                
                // 4. 상태 초기화
                currentOrder = null;
                currentMenu = null;
                progress = 0;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * 모든 메뉴 큐를 순회하며 가장 빠른 주문번호를 가진 작업을 찾아 가져온다.
     * 
     * <p>여러 요리사 스레드가 동시에 호출할 수 있으므로 synchronized로 동기화하여
     * 같은 주문을 중복으로 가져가는 것을 방지한다.</p>
     * 
     * @return 가장 빠른 주문, 없으면 null
     */
    private Order findWork() {
        synchronized (queueManager) {  // 락 걸기
            Order earliestOrder = null;
            MenuItem earliestMenu = null;
            OrderQueue earliestQueue = null;
            
            for (MenuItem menu : MenuItem.values()) {
                OrderQueue queue = queueManager.getMenuQueue(menu);
                Order order = queue.peek();
                
                if (order != null) {
                    if (earliestOrder == null || order.getOrderId() < earliestOrder.getOrderId()) {
                        earliestOrder = order;
                        earliestMenu = menu;
                        earliestQueue = queue;
                    }
                }
            }
            
            if (earliestQueue != null) {
                earliestQueue.poll();
                currentOrder = earliestOrder;
                currentMenu = earliestMenu;
                Logger.log("[요리사#" + id + "] 주문#" + earliestOrder.getOrderId() + " " + earliestMenu.getName() + " 조리 시작");
                return earliestOrder;
            }
            
            return null;
        }
    }
    
    private void cook() throws InterruptedException {
        int cookTime = currentMenu.getCookTime();
        int step = cookTime / 10;
        
        for (int p = 0; p <= 100; p += 10) {
            progress = p;
            Thread.sleep(step);
        }
    }
    
    public void stop() {
        running = false;
    }
    
    public String getStatusString() {
        if (isWorking()) {
            return String.format("요리사#%d: 주문#%d %s 조리중 [%d%%]", 
                id, currentOrder.getOrderId(), currentMenu.getName(), progress);
        } else {
            return String.format("요리사#%d: 대기중", id);
        }
    }
    
    // 대시보드용 Getter
    public int getId() {
        return id;
    }
    
    public Order getCurrentOrder() {
        return currentOrder;
    }
    
    public MenuItem getCurrentMenu() {
        return currentMenu;
    }
    
    public int getProgress() {
        return progress;
    }
    
    public boolean isWorking() {
        return currentOrder != null;
    }
}