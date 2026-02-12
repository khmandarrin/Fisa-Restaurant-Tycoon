package model;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Order {
	private final int orderId;
	private final List<MenuItem> items; // 이 주문에 포함된 메뉴들
	private final String address;
	private final long timestamp; // 주문 생성 시간

	// 동시성 제어를 위한 원자적 카운터
	private final AtomicInteger completedCount = new AtomicInteger(0);
	private final int totalItems;

	public Order(int orderId, List<MenuItem> items, String address) {
		this.orderId = orderId;
		this.items = items;
		this.address = address;
		this.totalItems = items.size();
		this.timestamp = System.currentTimeMillis();
	}

	/**
	 * 요리사 스레드가 조리를 마칠 때마다 호출함
	 * 
	 * @return true이면 모든 메뉴 조리 완료 (배달 가능 상태)
	 */
	public boolean addItemComplete() {
		// 카운트를 1 올리고, 그 값이 전체 아이템 수와 같은지 확인
		int current = completedCount.incrementAndGet();
		return current == totalItems;
	}

	// 대시보드 표시를 위한 게이지 계산 (0~100%)
	public int getProgressPercent() {
		return (int) ((completedCount.get() / (double) totalItems) * 100);
	}

	// Getters
	public int getOrderId() {
		return orderId;
	}

	public List<MenuItem> getItems() {
		return items;
	}

	public String getAddress() {
		return address;
	}

	public int getTotalItems() {
		return totalItems;
	}

	public int getCompletedCount() {
		return completedCount.get();
	}
}