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

## 3. 시퀀스 다이어그램 

<img src="./assets/sequence.png" width="800" alt="시퀀스다이어그램">

## 4. 핵심 기술 설계 

### 4.1. Producer-Consumer 
본 프로젝트는 두 단계의 생산자-소비자 연결 구조를 가집니다.
- 1차: `OrderGenerator` (생산자) → `ChefWorker` (소비자)
- 2차: `ChefWorker` (생산자) → `RiderWorker` (소비자)
- `ChefWorker`는 메뉴를 소비하는 주체임과 동시에, 주문을 완성시켜 배달 큐에 넣는 생산자의 역할을 겸합니다.

### 4.2 동기화


1. AtomicInteger
이 프로젝트에서 하나의 주문(`Order`)은 여러 개의 메뉴(`List`)로 구성됩니다. 각 메뉴는 서로 다른 요리사들이 병렬로 조리할 수 있습니다.
주문에 포함된 모든 메뉴가 조리되었는지 판별하기 위한 장치가 필요합니다. 조리가 끝날 때마다 카운트를 증가시켜, 이 값이 전체 메뉴 개수와 일치하는 순간에만 해당 주문을 '배달 가능' 상태로 전환하여 배달 큐로 보냅니다.

* 일반 `int` 변수를 사용하면 두 요리사가 동시에 `count++`를 할 때, CPU 캐시 메모리 문제로 인해 2가 올라가야 할 상황에서 1만 올라가는 **경합 조건(Race Condition)**이 발생할 수 있습니다.
* 이를 해결하기 위해 `AtomicInteger`를 사용하였습니다. 이는 하드웨어 수준의 **CAS(Compare-And-Swap)** 연산을 이용하여, 다른 스레드가 개입하지 못하도록 원자적으로 값을 증가시킵니다.

2. Synchronized
`ChefWorker`의 `findWork()`는 요리사가 조리를 시작하기 전, 모든 메뉴 큐를 훑으며 자신이 처리할 최적의 작업(일감)을 결정하고 가져오는 탐색 메서드입니다.
이 메서드에서 `synchronized(queueManager)`를 통해 주방의 모든 자원 관리 권한을 한 명의 요리사가 독점하도록 합니다.
어떤 일감이 있는지 확인(`peek`)하고 그 일감을 집어가는(`poll`) 과정 사이에 다른 요리사가 개입하는 것을 차단합니다. 이를 통해 한 요리사가 락(Lock)을 획득하여 주문을 확정하는 동안 다른 요리사는 해당 코드 영역에 진입하지 못하고 대기(Blocked)하게 되어, 작업의 유일성이 보장됩니다.

3. BlockingQueue
생산자와 소비자의 작업 속도가 다를 때 시스템의 실행을 조율합니다.
`BlockingQueue`의 `put()`과 `take()`(또는 `poll`)은 내부적으로 이미 동기화되어 있어, 한 번에 하나의 스레드만 안전하게 데이터를 넣거나 뺄 수 있음을 보장합니다.

- 생산자(OrderGenerator): 주문이 너무 빨리 생성되어 큐가 꽉 차면, put() 메서드는 빈 공간이 생길 때까지 생산자 스레드를 자동으로 대기(Blocking)시킵니다.
- 소비자(Chef/Rider): 처리할 일감이 없으면 poll()이나 take()가 대기 상태로 들어가, 일감이 들어오는 순간 즉시 깨어나 작업을 시작합니다.





### 4.3 스케줄링 
효율적인 주방 운영을 위해 셰프(ChefWorker)가 요리할 메뉴를 선택하는 기준에는 다음과 같은 우선순위를 적용합니다.

1. **긴급 처리 (Backpressure)**: 특정 메뉴 큐의 크기가 임계치(80%)를 초과할 경우, 주문 번호와 상관없이 해당 큐를 최우선으로 처리하여 시스템 병목을 해소합니다.
2. **순차 처리 (FCFS)**: 긴급 상황이 아닐 시, 모든 큐를 전수 조사하여 주문 번호(Order ID)가 가장 낮은 작업을 선택함으로써 선입선출 원칙을 준수합니다.


## 5. 실행 방법 

프로그램 실행 시 인자로 요리사의 수와 배달원의 수를 전달합니다.

```bash
mvn exec:java -Dexec.mainClass="Main" -Dexec.args="--chefCount 3 --riderCount 2"
```

