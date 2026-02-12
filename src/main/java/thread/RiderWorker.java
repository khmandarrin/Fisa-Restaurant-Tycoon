package thread;

import model.Order;
import model.OrderQueue;
import view.Logger;

public class RiderWorker implements Runnable{
	
	private final int riderId;
	private final OrderQueue deliveryQueue;
	private final Logger logger;

	public RiderWorker(int riderId, OrderQueue deliveryQueue, Logger logger) {
		this.riderId = riderId;
		this.deliveryQueue = deliveryQueue;
		this.logger = logger;
	}

	@Override
	public void run() {
		
		while(true) {

			try {
				// 1. 배달 큐(deliveryQueue)에서 완성된 Order를 꺼냄 (pop)
				Order order = deliveryQueue.pop();
				
				// 2. 배달 시작 로그 기록: "#N번 배달 출발 (주소: XXX)"
				// logger.log();
				
				// 3. 배달 시간 시뮬레이션: SLEEP(랜덤 2~5초)
				int deliveryTime = 2000 + (int)(Math.random() * 3000);
				Thread.sleep(deliveryTime);
				
				// 4. 배달 완료 로그 기록: "#N번 배달 완료!"
				// logger.log();
				
				// 5. 다음 배달을 위해 대기
				// -> OrderQueue 에서 take() 하면 자동으로 WAINTING 상태로 처리됨.
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		
	}

}


