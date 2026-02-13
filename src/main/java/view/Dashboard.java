package view;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	private static final int COL_INNER = 22;
	private static final int BAR_WIDTH = 10;
	private static final int CHEF_BAR_WIDTH = 8;
	private static final int LEFT_COL = 55;

	// ANSI 색상
	private static final String RESET   = "\033[0m";
	private static final String BOLD    = "\033[1m";
	private static final String RED     = "\033[31m";
	private static final String GREEN   = "\033[32m";
	private static final String YELLOW  = "\033[33m";
	private static final String MAGENTA = "\033[35m";
	private static final String CYAN    = "\033[36m";
	private static final String GRAY    = "\033[90m";

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
		line(sb, "║  🍳 " + BOLD + "Restaurant Tycoon" + RESET + " 🍳");
		line(sb, "╠" + sep);

		// ── 주문 접수 (왼쪽) + 조리중 (오른쪽) 2열 ──
		List<String> recentLines = new ArrayList<>();
		for (String order : orderGenerator.getRecentOrders()) {
			recentLines.add("🔔 " + order);
		}

		List<String> cookingLines = buildCookingOrders();

		line(sb, "║  📋 " + CYAN + "주문 접수" + RESET
			+ padRight("", LEFT_COL - 15)
			+ " ║  🔥 " + MAGENTA + "조리중" + RESET);

		for (int i = 0; i < 5; i++) {
			String left = i < recentLines.size() ? recentLines.get(i) : "";
			String right = i < cookingLines.size() ? cookingLines.get(i) : "";
			line(sb, "║    " + padRight(left, LEFT_COL - 4) + "║    " + right);
		}

		line(sb, "╠" + sep);
		line(sb, "║  👨‍🍳 " + BOLD + "주방" + RESET);
		line(sb, "║");

		// ── 조리대 박스 ──

		// 상단 테두리
		StringBuilder row = new StringBuilder("║  ");
		for (int i = 0; i < cols; i++) {
			if (i > 0) row.append(" ");
			row.append("┌").append("─".repeat(COL_INNER)).append("┐");
		}
		line(sb, row.toString());

		// 메뉴 이름 (이모지 포함)
		row = new StringBuilder("║  ");
		for (int i = 0; i < cols; i++) {
			if (i > 0) row.append(" ");
			row.append("│").append(centerPad(BOLD + menuLabel(menus[i]) + RESET, COL_INNER)).append("│");
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

		// ── 조리대 아래: 요리사 정보 ──
		List<ChefWorker> allChefs = kitchen.getChiefs();
		int maxChefRows = allChefs.size();

		@SuppressWarnings("unchecked")
		List<ChefWorker>[] chefsPerMenu = new List[cols];
		for (int i = 0; i < cols; i++) {
			chefsPerMenu[i] = findChefsForMenu(menus[i]);
		}

		for (int r = 0; r < maxChefRows; r++) {
			row = new StringBuilder("║  ");
			for (int i = 0; i < cols; i++) {
				if (i > 0) row.append(" ");
				if (r < chefsPerMenu[i].size()) {
					ChefWorker chef = chefsPerMenu[i].get(r);
					Order order = chef.getCurrentOrder();
					String bar = chefProgressBar(chef.getProgress());
					String info = YELLOW + "👨‍🍳#" + chef.getId() + RESET
						+ " #" + order.getOrderId() + " " + bar;
					row.append(padRight(info, colTotal));
				} else if (r == 0 && chefsPerMenu[i].isEmpty()) {
					row.append(padRight(GRAY + "💤 비어있음" + RESET, colTotal));
				} else {
					row.append(padRight("", colTotal));
				}
			}
			line(sb, row.toString());
		}

		line(sb, "║");
		line(sb, "╠" + sep);

		// ── 배달 ──
		int dqSize = queueManager.getDeliveryQueue().size();
		line(sb, "║  🛵 " + BOLD + "배달" + RESET + "  📦 완성 대기: " + dqSize + "/5");
		for (RiderWorker rider : deliveryCenter.getRiderStatus()) {
			if (rider.isDelivering()) {
				line(sb, "║    🟢 " + GREEN + rider.getStatusString() + RESET);
			} else if (rider.isJustCompleted()) {
				line(sb, "║    ✅ " + CYAN + rider.getStatusString() + RESET);
			} else {
				line(sb, "║    💤 " + GRAY + rider.getStatusString() + RESET);
			}
		}

		line(sb, "╚" + sep);

		lastLineCount = sb.toString().split("\n", -1).length - 1;
		System.out.print(sb);
		System.out.flush();
	}

	// ── 조리중 주문 목록 생성 ──
	private List<String> buildCookingOrders() {
		List<String> result = new ArrayList<>();
		Map<Integer, List<String>> orderChefs = new LinkedHashMap<>();

		for (ChefWorker chef : kitchen.getChiefs()) {
			if (chef.isWorking() && chef.getCurrentOrder() != null && chef.getCurrentMenu() != null) {
				int id = chef.getCurrentOrder().getOrderId();
				orderChefs.computeIfAbsent(id, k -> new ArrayList<>());
				orderChefs.get(id).add(chef.getCurrentMenu().getName() + "(#" + chef.getId() + ")");
			}
		}

		for (Map.Entry<Integer, List<String>> entry : orderChefs.entrySet()) {
			result.add("🍳 #" + entry.getKey() + " " + String.join(", ", entry.getValue()));
		}
		return result;
	}

	private String menuLabel(MenuItem menu) {
		switch (menu) {
			case COFFEE:  return "☕ " + menu.getName();
			case SALAD:   return "🥗 " + menu.getName();
			case PIZZA:   return "🍕 " + menu.getName();
			case PASTA:   return "🍝 " + menu.getName();
			case GNOCCHI: return "🥟 " + menu.getName();
			default:      return menu.getName();
		}
	}

	private List<ChefWorker> findChefsForMenu(MenuItem menu) {
		List<ChefWorker> result = new ArrayList<>();
		for (ChefWorker chef : kitchen.getChiefs()) {
			if (chef.isWorking() && chef.getCurrentMenu() == menu && chef.getCurrentOrder() != null) {
				result.add(chef);
			}
		}
		return result;
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

	// 표시 너비 계산 (한글 2칸, 이모지 2칸, ASCII 1칸)
	private int displayWidth(String s) {
		String stripped = s.replaceAll("\033\\[[0-9;]*m", "");
		int width = 0;
		for (int i = 0; i < stripped.length(); ) {
			int cp = stripped.codePointAt(i);
			if (cp >= 0xAC00 && cp <= 0xD7AF) {          // 한글 음절
				width += 2;
			} else if (cp >= 0x1F000 && cp <= 0x1FFFF) {  // 이모지 (보충 평면)
				width += 2;
			} else if (cp >= 0x2600 && cp <= 0x27BF) {    // 기호/딩뱃
				width += 2;
			} else if (cp == 0x200D) {                     // ZWJ (zero width joiner)
				// 폭 0
			} else if (cp >= 0xFE00 && cp <= 0xFE0F) {    // 변이 선택자
				// 폭 0
			} else {
				width += 1;
			}
			i += Character.charCount(cp);
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
