package model;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class OrderQueue {
	private final String queueName;
	private final BlockingQueue<Order> queue = new LinkedBlockingQueue<>();

	public OrderQueue(String queueName) {
		this.queueName = queueName;
	}

	public void push(Order order) {
		queue.offer(order);
	}

	public Order pop() throws InterruptedException {
		return queue.take(); // 작업이 없으면 스레드가 여기서 대기함
	}

	public int size() {
		return queue.size();
	}

	public String getQueueName() {
		return queueName;
	}
}
