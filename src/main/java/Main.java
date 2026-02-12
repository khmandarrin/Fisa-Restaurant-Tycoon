import core.OrderGenerator;
import core.QueueManager;

public class Main {
	public static void main(String[] args) {
		QueueManager queueManager = new QueueManager();
		OrderGenerator orderGenerator = new OrderGenerator(queueManager);
		Thread thread = new Thread(orderGenerator);
		thread.start();
	}
}
