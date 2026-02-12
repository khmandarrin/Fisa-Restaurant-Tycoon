# 👨🏻‍🍳 미니 식당 타이쿤 (Mini Restaurant Tycoon)

자바 멀티스레딩 환경에서 생산자-소비자(Producer-Consumer) 패턴을 구현한 식당 운영 시뮬레이션 프로젝트입니다.

## 🎮 실행 화면

![데모](assets/demo.gif)

## 1. 프로젝트 개요

복수의 요리사(Chef)와 배달원(Rider) 스레드가 공유 자원인 주문 큐를 통해 데이터를 처리하는 시스템입니다. 
하나의 주문이 여러 개의 메뉴로 구성될 때, 다수의 요리사가 이를 병렬로 처리하고 최종적으로 하나의 주문으로 병합하여 배달 단계로 넘기는 동기화 메커니즘으로 설계되었습니다.

## 2. 기술 스택 및 아키텍처

- Language: Java 17
- Logging: SLF4J, Logback

```text
src/main/java/com/tycoon/
├── Main.java                 # Entry Point: 인자 처리 및 시스템 가동
├── core/                     
│   ├── Kitchen.java          # 요리사 관리 및 메뉴별 큐 소유
│   ├── DeliveryCenter.java   # 배달원 관리 및 배달 큐 소유
│   ├── OrderGenerator.java   # Producer: 무작위 주문 생성 및 분배
│   └── QueueManager.java     # Hub: 모든 큐 인스턴스 중앙 관리
├── model/                    
│   ├── Order.java            # 주문 객체
│   ├── MenuItem.java         # Enum: 메뉴별 조리 시간 정의
│   └── OrderQueue.java       # BlockingQueue 래퍼 클래스
├── thread/                   
│   ├── ChefWorker.java     # Consumer 1: 라운드 로빈 조리 수행
│   └── RiderWorker.java      # Consumer 2: 배달 처리 수행
└── view/                     
    └── Dashboard.java        # 콘솔 출력

```

## 3. 클래스 다이어그램 

시퀀스 다이어그램? 


## 4. 핵심 기술 설계 

### 4.1. Producer-Consumer 
본 프로젝트는 두 단계의 생산자-소비자 연결 구조를 가집니다.
- 1차: `OrderGenerator` (생산자) → `ChefWorker` (소비자)
- 2차: `ChefWorker` (생산자) → `RiderWorker` (소비자)
- `ChefWorker`는 메뉴를 소비하는 주체임과 동시에, 주문을 완성시켜 배달 큐에 넣는 생산자의 역할을 겸합니다.

### 4.2 동기화
* **AtomicInteger**: `Order` 객체 내의 `completedCount`를 관리하여, 여러 요리사가 동시에 메뉴를 완료하더라도 경합 조건(Race Condition) 없이 안전하게 전체 주문 완료 여부를 판별합니다.
* **BlockingQueue**: 큐가 가득 차면 생산자를 대기시키고, 비어 있으면 소비자를 대기시켜 CPU 자원 낭비를 방지합니다.
* **Global Locking**: `QueueManager` 객체에 대한 `synchronized` 블록을 사용하여 `peek()`과 `poll()` 사이의 원자성을 확보, 중복 수주를 방지합니다.



### 4.3 스케줄링 
효율적인 주방 운영을 위해 셰프(ChefWorker)가 요리할 메뉴를 선택하는 기준에는 다음과 같은 우선순위를 적용합니다.

1. **긴급 처리 (Backpressure)**: 특정 메뉴 큐의 크기가 임계치(80%)를 초과할 경우, 주문 번호와 상관없이 해당 큐를 최우선으로 처리하여 시스템 병목을 해소합니다.
2. **순차 처리 (FCFS)**: 긴급 상황이 아닐 시, 모든 큐를 전수 조사하여 주문 번호(Order ID)가 가장 낮은 작업을 선택함으로써 선입선출 원칙을 준수합니다.


## 5. 실행 방법 

프로그램 실행 시 인자로 요리사의 수와 배달원의 수를 전달합니다.

```bash
mvn exec:java -Dexec.mainClass="Main" -Dexec.args="--chefCount 3 --riderCount 2"
```

