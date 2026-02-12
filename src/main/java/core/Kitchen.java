package core;

import java.util.ArrayList;
import java.util.List;

import thread.ChefWorker;
import view.Logger;

public class Kitchen {
    private int chefCount;
    private final List<ChefWorker> chefs = new ArrayList<>();
    private final List<Thread> chiefThreads = new ArrayList<>();
    private QueueManager queueManager;

    
    public Kitchen(int chefCount, QueueManager queueManager) {
		super();
		this.chefCount = chefCount;
		this.queueManager = queueManager;
	}

	public void startOperations() {
        // 요리사 투입
        for (int i = 0; i < chefCount; i++) {
            ChefWorker chef = new ChefWorker(i, queueManager);
            Thread thread = new Thread(chef, "요리사#" + i);
            thread.start();
            
            chefs.add(chef);
            chiefThreads.add(thread);
        }
        
        Logger.log("[주방] 요리사 " + chefCount + "명 투입 완료");
    }

    public List<String> getStatusReport() {
        List<String> report = new ArrayList<>();
        
        for (ChefWorker chef : chefs) {
            report.add(chef.getStatusString());
        }
        
        return report;
    }

    public void stop() {
        for (ChefWorker chief : chefs) {
        	chief.stop();
        }
        Logger.log("[주방] 영업 종료");
    }

    public List<ChefWorker> getChiefs() {
        return chefs;
    }
}