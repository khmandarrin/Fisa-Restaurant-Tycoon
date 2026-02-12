import core.DeliveryCenter;
import core.Kitchen;
import core.OrderGenerator;
import core.QueueManager;
import view.Dashboard;

public class Main {
	public static void main(String[] args) {
		QueueManager queueManager = new QueueManager(10, 20);
		
		// 주방 가동 (요리사 3명)
		Kitchen kitchen = new Kitchen();
		kitchen.start(3, queueManager);

		// 배달 센터 가동 (배달원 2명)
		DeliveryCenter deliveryCenter = new DeliveryCenter(2, queueManager);
		deliveryCenter.startOperations();
		
		// 주문 생성 시작
		OrderGenerator orderGenerator = new OrderGenerator(queueManager);
		new Thread(orderGenerator).start();

		// 대시보드 시작
		Thread dashboardThread = new Thread(new Dashboard(kitchen, deliveryCenter, queueManager, orderGenerator));
		dashboardThread.setDaemon(true);
		dashboardThread.start();
	}
}
