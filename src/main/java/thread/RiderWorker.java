package thread;

import model.Order;
import model.OrderQueue;
import view.Logger;

public class RiderWorker implements Runnable{

	private final int riderId;
	private final OrderQueue deliveryQueue;
	private volatile Order currentOrder;
	private volatile boolean delivering;

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
				Logger.log("#" + riderId + "번 배달 출발 (주소: " + order.getAddress() + ")");

				// 3. 배달 시간 시뮬레이션: SLEEP(랜덤 2~5초)
				int deliveryTime = 2000 + (int)(Math.random() * 3000);
				Thread.sleep(deliveryTime);

				// 4. 배달 완료 로그 기록
				Logger.log("#" + riderId + "번 배달 완료!");

				// 5. 상태 초기화
				delivering = false;
				currentOrder = null;

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

	public String getStatusString() {
		if (delivering && currentOrder != null) {
			return String.format("배달원#%d: 주문#%d 배달중 → %s",
				riderId, currentOrder.getOrderId(), currentOrder.getAddress());
		}
		return String.format("배달원#%d: 대기중", riderId);
	}

}
