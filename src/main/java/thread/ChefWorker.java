package thread;

import core.QueueManager;
import model.MenuItem;
import model.Order;
import model.OrderQueue;

public class ChefWorker implements Runnable {

	private final int id;
	private final QueueManager queueManager;
	private volatile boolean running = true;
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ChefWorker.class);

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
					logger.info("[요리사#" + id + "] 주문#" + currentOrder.getOrderId() + " 조리 완료 → 배달 큐");
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
     * 메뉴 큐를 탐색하여 최적의 조리 작업을 결정하고 인출.
     * * 우선순위 정책:
     * 1. 큐 포화도 기반 긴급 처리: 임계치를 초과하여 쌓인 메뉴를 최우선으로 처리
     * 2. 주문 번호 기반 순차 처리: 대기 중인 모든 메뉴 중 주문 번호가 가장 빠른 것을 선택
     * * 동기화 처리: {@code queueManager} 객체를 통한 전역 동기화
     * * @return 결정된 조리 작업(Order), 대기 중인 작업이 없을 경우 null
     */
	private Order findWork() {
		synchronized (queueManager) {
			// 1. 큐 포화도 기반 긴급 작업 탐색
			Order urgentOrder = findUrgentOrder();
			if (urgentOrder != null) {
				return urgentOrder;
			}

			// 2. 주문 번호 기반 일반 작업 탐색
			return findEarliestOrder();
		}
	}


	// 설정된 임계치(80% 이상)를 초과한 큐가 있는지 확인하고 가장 먼저 발견된 긴급 작업을 반환
	private Order findUrgentOrder() {
		for (MenuItem menu : MenuItem.values()) {
			OrderQueue queue = queueManager.getMenuQueue(menu);

			// 큐의 크기가 8개 이상인 경우 긴급 건으로 간주
			if (queue.size() >= 8) {
				Order order = queue.poll();
				if (order != null) {
					updateCurrentStatus(order, menu);
					logger.warn("[긴급 조리] 큐 포화로 인한 우선 처리: {}", menu.getName());
					return order;
				}
			}
		}
		return null;
	}


	// 모든 메뉴 큐를 순회하여 대기 중인 주문 중 주문 번호(Order ID)가 가장 빠른 작업을 찾아 반환
	private Order findEarliestOrder() {
		Order earliestOrder = null;
		MenuItem earliestMenu = null;
		OrderQueue earliestQueue = null;

		for (MenuItem menu : MenuItem.values()) {
			OrderQueue queue = queueManager.getMenuQueue(menu);
			Order order = queue.peek();

			if (order != null) {
				// 가장 낮은 주문 번호를 가진 작업 탐색
				if (earliestOrder == null || order.getOrderId() < earliestOrder.getOrderId()) {
					earliestOrder = order;
					earliestMenu = menu;
					earliestQueue = queue;
				}
			}
		}

		if (earliestQueue != null) {
			earliestQueue.poll();
			updateCurrentStatus(earliestOrder, earliestMenu);
			logger.info("[요리사#{}] 주문#{} {} 조리 시작", id, earliestOrder.getOrderId(), earliestMenu.getName());
			return earliestOrder;
		}

		return null;
	}

	// 현재 요리사가 작업 중인 주문과 메뉴 상태를 업데이트
	private void updateCurrentStatus(Order order, MenuItem menu) {
		this.currentOrder = order;
		this.currentMenu = menu;
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
			return String.format("요리사#%d: 주문#%d %s 조리중 [%d%%]", id, currentOrder.getOrderId(), currentMenu.getName(),
					progress);
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