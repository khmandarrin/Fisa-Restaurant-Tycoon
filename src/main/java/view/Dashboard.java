package view;

import java.util.List;

import core.DeliveryCenter;
import core.Kitchen;
import core.OrderGenerator;
import core.QueueManager;
import model.MenuItem;
import model.Order;
import thread.ChefWorker;
import thread.RiderWorker;

public class Dashboard implements Runnable {

	private static final int REFRESH_MS = 500;
	private static final int COL_INNER = 14;
	private static final int BAR_WIDTH = 8;
	private static final int CHEF_BAR_WIDTH = 6;

	// ANSI 색상
	private static final String RESET  = "\033[0m";
	private static final String BOLD   = "\033[1m";
	private static final String RED    = "\033[31m";
	private static final String GREEN  = "\033[32m";
	private static final String YELLOW = "\033[33m";
	private static final String CYAN   = "\033[36m";
	private static final String GRAY   = "\033[90m";

	private final Kitchen kitchen;
	private final DeliveryCenter deliveryCenter;
	private final QueueManager queueManager;
	private final OrderGenerator orderGenerator;
	private int lastLineCount = 0;

	public Dashboard(Kitchen kitchen, DeliveryCenter deliveryCenter, QueueManager queueManager, OrderGenerator orderGenerator) {
		this.kitchen = kitchen;
		this.deliveryCenter = deliveryCenter;
		this.queueManager = queueManager;
		this.orderGenerator = orderGenerator;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(REFRESH_MS);
				render();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	private void render() {
		try {
			renderFrame();
		} catch (Exception e) {
			// 멀티스레드 race condition 시 프레임 스킵
		}
	}

	private void renderFrame() {
		StringBuilder sb = new StringBuilder();
		if (lastLineCount > 0) {
			sb.append(String.format("\033[%dA", lastLineCount));
		}

		MenuItem[] menus = MenuItem.values();
		int cols = menus.length;
		int colTotal = COL_INNER + 2;
		String sep = "═".repeat(cols * colTotal + (cols - 1) + 2);

		// ── 타이틀 ──
		line(sb, "╔" + sep);
		line(sb, "║  " + BOLD + "Restaurant Tycoon" + RESET);
		line(sb, "╠" + sep);

		// ── 최근 주문 (5줄 고정) ──
		line(sb, "║  " + CYAN + "[주문 접수]" + RESET);
		List<String> recent = orderGenerator.getRecentOrders();
		for (int i = 0; i < 5; i++) {
			if (i < recent.size()) {
				line(sb, "║    " + recent.get(i));
			} else {
				line(sb, "║");
			}
		}

		line(sb, "╠" + sep);
		line(sb, "║  " + BOLD + "[주방]" + RESET);
		line(sb, "║");

		// ── 조리대 박스 ──

		// 상단 테두리
		StringBuilder row = new StringBuilder("║  ");
		for (int i = 0; i < cols; i++) {
			if (i > 0) row.append(" ");
			row.append("┌").append("─".repeat(COL_INNER)).append("┐");
		}
		line(sb, row.toString());

		// 메뉴 이름
		row = new StringBuilder("║  ");
		for (int i = 0; i < cols; i++) {
			if (i > 0) row.append(" ");
			row.append("│").append(centerPad(BOLD + menus[i].getName() + RESET, COL_INNER)).append("│");
		}
		line(sb, row.toString());

		// 큐 게이지바
		row = new StringBuilder("║  ");
		for (int i = 0; i < cols; i++) {
			if (i > 0) row.append(" ");
			int size = queueManager.getQueueSize(menus[i]);
			String bar = coloredQueueBar(size, 10);
			row.append("│").append(centerPad(bar, COL_INNER)).append("│");
		}
		line(sb, row.toString());

		// 큐 수량
		row = new StringBuilder("║  ");
		for (int i = 0; i < cols; i++) {
			if (i > 0) row.append(" ");
			int size = queueManager.getQueueSize(menus[i]);
			row.append("│").append(centerPad(size + "/10", COL_INNER)).append("│");
		}
		line(sb, row.toString());

		// 하단 테두리
		row = new StringBuilder("║  ");
		for (int i = 0; i < cols; i++) {
			if (i > 0) row.append(" ");
			row.append("└").append("─".repeat(COL_INNER)).append("┘");
		}
		line(sb, row.toString());

		// ── 조리대 아래: 요리사 정보 (2줄 고정) ──
		ChefWorker[] stationChefs = new ChefWorker[cols];
		for (int i = 0; i < cols; i++) {
			stationChefs[i] = findChefForMenu(menus[i]);
		}

		// 줄 1: 요리사 이름 + 주문번호
		row = new StringBuilder("║  ");
		for (int i = 0; i < cols; i++) {
			if (i > 0) row.append(" ");
			ChefWorker chef = stationChefs[i];
			if (chef != null) {
				Order order = chef.getCurrentOrder();
				String info = YELLOW + "요리사#" + chef.getId() + RESET + " #" + order.getOrderId();
				row.append(padRight(info, colTotal));
			} else {
				row.append(padRight(GRAY + "비어있음" + RESET, colTotal));
			}
		}
		line(sb, row.toString());

		// 줄 2: 조리 진행바
		row = new StringBuilder("║  ");
		for (int i = 0; i < cols; i++) {
			if (i > 0) row.append(" ");
			ChefWorker chef = stationChefs[i];
			if (chef != null) {
				row.append(padRight(chefProgressBar(chef.getProgress()), colTotal));
			} else {
				row.append(padRight("", colTotal));
			}
		}
		line(sb, row.toString());

		line(sb, "║");
		line(sb, "╠" + sep);

		// ── 배달 ──
		int dqSize = queueManager.getDeliveryQueue().size();
		line(sb, "║  " + BOLD + "[배달]" + RESET + "  완성 대기: " + dqSize + "/20");
		for (RiderWorker rider : deliveryCenter.getRiderStatus()) {
			if (rider.isDelivering()) {
				line(sb, "║    " + GREEN + rider.getStatusString() + RESET);
			} else {
				line(sb, "║    " + GRAY + rider.getStatusString() + RESET);
			}
		}

		line(sb, "╚" + sep);

		lastLineCount = sb.toString().split("\n", -1).length - 1;
		System.out.print(sb);
		System.out.flush();
	}

	private ChefWorker findChefForMenu(MenuItem menu) {
		for (ChefWorker chef : kitchen.getChiefs()) {
			if (chef.isWorking() && chef.getCurrentMenu() == menu && chef.getCurrentOrder() != null) {
				return chef;
			}
		}
		return null;
	}

	private String coloredQueueBar(int current, int max) {
		int filled = (max == 0) ? 0 : (current * BAR_WIDTH) / max;
		if (filled > BAR_WIDTH) filled = BAR_WIDTH;
		String color;
		int percent = (max == 0) ? 0 : (current * 100) / max;
		if (percent >= 80) color = RED;
		else if (percent >= 50) color = YELLOW;
		else color = GREEN;
		return color + "[" + "█".repeat(filled) + "░".repeat(BAR_WIDTH - filled) + "]" + RESET;
	}

	private String chefProgressBar(int progress) {
		int filled = (progress * CHEF_BAR_WIDTH) / 100;
		if (filled > CHEF_BAR_WIDTH) filled = CHEF_BAR_WIDTH;
		return YELLOW + "[" + "█".repeat(filled) + "░".repeat(CHEF_BAR_WIDTH - filled) + "]" + RESET + " " + progress + "%";
	}

	// 한글 2칸, ASCII 1칸 기준 표시 너비 계산
	private int displayWidth(String s) {
		String stripped = s.replaceAll("\033\\[[0-9;]*m", "");
		int width = 0;
		for (char c : stripped.toCharArray()) {
			if (c >= 0xAC00 && c <= 0xD7AF) {
				width += 2;
			} else {
				width += 1;
			}
		}
		return width;
	}

	private String centerPad(String s, int targetWidth) {
		int current = displayWidth(s);
		if (current >= targetWidth) return s;
		int total = targetWidth - current;
		int left = total / 2;
		int right = total - left;
		return " ".repeat(left) + s + " ".repeat(right);
	}

	private String padRight(String s, int targetWidth) {
		int current = displayWidth(s);
		if (current >= targetWidth) return s;
		return s + " ".repeat(targetWidth - current);
	}

	private void line(StringBuilder sb, String text) {
		sb.append(text).append("\033[K\n");
	}

}
