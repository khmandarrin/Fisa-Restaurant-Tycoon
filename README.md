# ☕ 미니 식당 타이쿤 (Mini Restaurant Tycoon)

자바 멀티스레딩 환경에서 생산자-소비자(Producer-Consumer) 패턴을 구현한 식당 운영 시뮬레이션 프로젝트입니다.

## 1. 프로젝트 개요

이 프로젝트는 복수의 요리사(Cooker)와 배달원(Rider) 스레드가 공유 자원인 주문 큐(OrderQueue)를 통해 데이터를 주고받는 시스템을 구축하는 데 목적이 있습니다. 특히 각 주문이 여러 개의 메뉴로 구성될 때, 여러 요리사가 동시에 한 주문의 구성 요소들을 처리하고 이를 다시 하나로 병합하는 동기화 로직을 핵심으로 합니다.

## 2. 기술 스택 및 아키텍처 (Tech Stack & Architecture)

- Language: Java 17
- Concurrency: Thread, Runnable, BlockingQueue, AtomicInteger, ExecutorService

**시스템 구조**
  - Main: 시스템 초기화 및 인자(args) 처리
  - QueueManager: 중앙 자원 관리 허브
  - Workers: CookerWorker, RiderWorker (비동기 작업 수행)
  - View: ANSI 코드를 이용한 실시간 콘솔 대시보드

```text
src/main/java/com/tycoon/
├── Main.java                 # Entry Point: 인자 처리 및 시스템 가동
├── core/                     # 시스템 제어 로직
│   ├── Kitchen.java          # 요리사 관리 및 메뉴별 큐 소유
│   ├── DeliveryCenter.java   # 배달원 관리 및 배달 큐 소유
│   ├── OrderGenerator.java   # Producer: 무작위 주문 생성 및 분배
│   └── QueueManager.java     # Hub: 모든 큐 인스턴스 중앙 관리
├── model/                    # 데이터 객체 및 통로
│   ├── Order.java            # 주문 객체
│   ├── MenuItem.java         # Enum: 메뉴별 조리 시간 정의
│   └── OrderQueue.java       # BlockingQueue 래퍼 클래스
├── thread/                   # 실제 작업 수행(Worker)
│   ├── CookerWorker.java     # Consumer 1: 라운드 로빈 조리 수행
│   └── RiderWorker.java      # Consumer 2: 배달 처리 수행
└── view/                     # 사용자 인터페이스
    ├── Dashboard.java        # 실시간 시스템 현황 출력
    └── Logger.java           # 시스템 이벤트 로그 보관소

```

## 3. 클래스 다이어그램 (Class Diagram)

## 4. 핵심 기술 설계 (Design Decisions)

### 4.1 의존성 주입 (Dependency Injection)

`QueueManager`를 `Main`에서 단일 인스턴스로 생성한 후, 이를 필요로 하는 각 관리소(`Kitchen`, `DeliveryCenter`) 및 생성기(`OrderGenerator`)에 생성자를 통해 주입합니다. 이는 객체 간의 결합도를 낮추고, 모든 스레드가 동일한 데이터 통로를 공유하고 있음을 보장하기 위한 설계입니다.

### 4.2 스케줄링 
`CookerWorker`는 특정 메뉴 큐에 고착되지 않도록 라운드 로빈(Round Robin) 방식을 사용합니다.

* `lastCheckedIndex` 변수를 사용하여 마지막으로 확인한 큐의 다음 인덱스부터 조사를 시작합니다.
* 모든 메뉴 큐에 대해 공평한 처리 기회를 부여하여 특정 메뉴의 대기열이 무한정 길어지는 현상을 방지합니다.

### 4.3 원자적 주문 완료 처리 (Atomic Operation)

하나의 주문이 여러 개의 메뉴를 가질 때, 여러 요리사 스레드가 동시에 `Order` 객체의 상태를 업데이트합니다.

* `AtomicInteger`를 사용하여 `completedCount`를 관리함으로써 경합 조건(Race Condition) 없이 안전하게 완료된 메뉴 수를 카운트합니다.
* 카운트가 주문 총량과 일치하는 순간을 감지한 마지막 요리사 스레드가 해당 주문을 배달 큐로 이동시키는 역할을 수행합니다.

### 4.4 비차단 및 차단 대기 (Blocking Queue)

* **Producer:** 큐가 가득 차면 공간이 생길 때까지 대기합니다.
* **Consumer:** 큐가 비어 있으면 데이터가 들어올 때까지 대기(Wait) 상태로 전환되어 불필요한 CPU 자원 소모를 방지합니다.

## 5. 실행 방법 (Usage)

프로그램 실행 시 인자로 요리사의 수와 배달원의 수를 전달합니다.

```bash
# 요리사 3명, 배달원 2명 실행 예시
java 

```
