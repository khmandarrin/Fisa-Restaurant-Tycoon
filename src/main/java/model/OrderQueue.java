package model;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class OrderQueue {
	private final String queueName;
	private final BlockingQueue<Order> queue;

	public OrderQueue(String queueName, int size) {
		this.queueName = queueName;
		this.queue = new LinkedBlockingQueue<>(size);
	}

	public void push(Order order) throws InterruptedException {
		queue.put(order);
	}

	public Order pop() throws InterruptedException {
		return queue.take(); // 작업이 없으면 스레드가 여기서 대기함
	}
	
    public Order poll() {
        return queue.poll();  // non-blocking, 없으면 null
    }

	public int size() {
		return queue.size();
	}

	public String getQueueName() {
		return queueName;
	}
	
	public Order peek() {
	    return queue.peek();  // 꺼내지 않고 맨 앞 확인
	}
	
}
