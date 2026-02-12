import core.DeliveryCenter;
import core.Kitchen;
import core.OrderGenerator;
import core.QueueManager;
import view.Dashboard;

public class Main {
	public static void main(String[] args) {

	        // 1. 기본값 설정
	        int chefCount = 3;
	        int riderCount = 2;

	        // 2. 인자 파싱 (Loop를 돌며 키워드 확인)
	        for (int i = 0; i < args.length; i++) {
	            try {
	                if ("--chefCount".equals(args[i]) && i + 1 < args.length) {
	                    chefCount = Integer.parseInt(args[i + 1]);
	                    i++; // 값까지 읽었으므로 다음 인덱스 건너뜀
	                } else if ("--riderCount".equals(args[i]) && i + 1 < args.length) {
	                    riderCount = Integer.parseInt(args[i + 1]);
	                    i++;
	                }
	            } catch (NumberFormatException e) {
	                System.err.println("인자 값이 숫자가 아닙니다. 기본값을 유지합니다.");
	            }
	        }

	        System.out.println("설정된 요리사 수: " + chefCount);
	        System.out.println("설정된 배달원 수: " + riderCount);

	        // 3. 시스템 초기화 및 의존성 주입
	        int menuQueueSize = 10;
	        int deliveryQueueSize = 2; // fix queue size
	        QueueManager queueManager = new QueueManager(menuQueueSize, deliveryQueueSize);
	        Kitchen kitchen = new Kitchen(chefCount, queueManager);
	        DeliveryCenter deliveryCenter = new DeliveryCenter(riderCount, queueManager);
	        OrderGenerator orderGenerator = new OrderGenerator(queueManager);
	        Dashboard dashboard = new Dashboard(kitchen, deliveryCenter, queueManager, orderGenerator);

	        // 4. 스레드 가동
	        kitchen.startOperations();
	        deliveryCenter.startOperations();
	        
	        new Thread(orderGenerator, "OrderGenerator").start();
	        new Thread(dashboard, "Dashboard").start();
	    
	}
}
