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
//                    Thread.sleep(100);  // 일감 없으면 잠깐 대기
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
    
    private Order findWork() {
        // 메뉴 큐 순회 (커피 → 샐러드 → 피자 → 파스타 → 뇨끼)
        for (MenuItem menu : MenuItem.values()) {
            OrderQueue queue = queueManager.getMenuQueue(menu);
            Order order = queue.poll();  // non-blocking
            
            if (order != null) {
                currentOrder = order;
                currentMenu = menu;
                Logger.log("[요리사#" + id + "] 주문#" + order.getOrderId() + " " + menu.getName() + " 조리 시작");
                return order;
            }
        }
        return null;
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