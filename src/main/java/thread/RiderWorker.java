package thread;

import model.Order;
import model.OrderQueue;

public class RiderWorker implements Runnable{

	private final int riderId;
	private final OrderQueue deliveryQueue;
  
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RiderWorker.class);

	private volatile Order currentOrder;
	private volatile boolean delivering;
	private volatile int lastCompletedOrderId;
	private volatile long completedAt;

	public RiderWorker(int riderId, OrderQueue deliveryQueue) {
		this.riderId = riderId;
		this.deliveryQueue = deliveryQueue;
	}

	@Override
	public void run() {

		while(true) {

			try {
				// 1. 배달 큐(deliveryQueue)에서 완성된 Order를 꺼냄 (pop)
				Order order = deliveryQueue.pop();
				currentOrder = order;
				delivering = true;

				// 2. 배달 시작 로그 기록
				logger.info("#" + riderId + "번 배달 출발 (주소: " + order.getAddress() + ")");

				// 3. 배달 시간 시뮬레이션: SLEEP(랜덤 10~15초)
				int deliveryTime = 10000 + (int)(Math.random() * 5000);
				Thread.sleep(deliveryTime);

				// 4. 배달 완료 로그 기록
				logger.info("#" + riderId + "번 배달 완료!");

				// 5. 완료 표시 후 잠시 대기
				lastCompletedOrderId = order.getOrderId();
				completedAt = System.currentTimeMillis();
				delivering = false;
				currentOrder = null;
				Thread.sleep(1000);

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}

		}

	}

	public int getRiderId() {
		return riderId;
	}

	public Order getCurrentOrder() {
		return currentOrder;
	}

	public boolean isDelivering() {
		return delivering;
	}

	public boolean isJustCompleted() {
		return !delivering && completedAt > 0
			&& (System.currentTimeMillis() - completedAt) < 1000;
	}

	public String getStatusString() {
		if (delivering && currentOrder != null) {
			return String.format("배달원#%d: 주문#%d 배달중 → %s",
				riderId, currentOrder.getOrderId(), currentOrder.getAddress());
		}
		if (isJustCompleted()) {
			return String.format("배달원#%d: 주문#%d 배달 완료!", riderId, lastCompletedOrderId);
		}
		return String.format("배달원#%d: 대기중", riderId);
	}

}
