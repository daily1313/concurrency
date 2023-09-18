## Concurrency Problem

### Multi-Thread env Test

- ExecutorService
  - 병렬 작업 시 여러 개의 작업을 효율적으로 처리하기 위해 제공되는 library
  - 손쉽게 ThreadPool을 구성하고 Task를 실행하고 관리할 수 있는 역할
  - Executors를 사용하여 ExecutorService 객체를 생성하며, ThreadPool의 개수 및 종류를 지정할 수 있는 메소드 제공
- CountDownLatch
  - thread가 다른 thread에서 작업이 완료될 때 까지 기다릴 수 있도록 해주는 클래스
  - CountDownLatch를 이용하여 multi-Thread가 100개의 요청을 처리한 후, 테스트 하도록 기다리게 해줍니다.
  - new CountDownLatch(100) : Latch할 개수 지정
  - countDown() : Latch 숫자 1 감소
  - await() : Latch의 숫자가 0이 될 때 까지 대기
- Multi-thread env에서 Test 진행 시 Race Condition 발생
  - 공유된 자원을 동시에 변경하려고 하기 때문에 발생
  - Race Condition: 두 개 이상의 Thread 및 Session이 공유된 자원에 액세스할 때 생기는 문제

### Race Condition을 해결하는 방법
- 하나의 thread가 작업이 끝난 이후 다른 thread가 관여하도록 해야 합니다
1. Synchronized 이용
   - Synchronized를 메서드 반환 타입 앞에 명시해주면 하나의 thread만 접근 가능
   - multi-thread 환경에서 thread간 데이터 동기화를 시켜주기 위해 자바에서 제공하는 키워드
   - 현재 데이터를 사용하고 있는 해당 thread를 제외하고 나머지 thread 들은 데이터 접근을 막음으로써, 순차적인 데이터 접근이 가능해짐
   - 하지만 하나의 프로세스 안에서만 보장이 되기에 한계가 존재
2. Pessimistic Lock
   - DB가 제공하는 lock 기능을 이용해 엔티티를 영속 상태로 올릴 때부터 다른 세션에서 조회하지 못하도록 Lock을 걸어두는 것
   - Shared Lock(공유락, 다른 트랜잭션에서 읽기만 가능)
   - Exclusive Lock(베타락, 다른 트랜잭션에서 읽기, 쓰기 둘 다 불가능)
   - @Lock(value = LockModeType.PESSIMISTIC_WRITE)
   - 장점: 충돌이 빈번하게 일어나는 상황에서 rollback 연산이 줄어들기 때문에, 부정확한 시스템을 경험하게 될 확률 감소
   - 단점: 데이터 자체에 별도에 락을 잡기에 동시성이 떨어지고, 읽기가 많은 작업에서 손해볼 가능성이 높음
3. Optimistic Lock 
   - 자원에 Lock을 걸지 않고 충돌이 발생했을 때, 이를 처리하는 방법
   - 공통된 자원에 @Version 어노테이션을 추가하여 구현하고, 만약 초기에 commit한 version과 커밋할 때의 version이 다르다면, rollback 처리
   - @Lock(value = LockModeType.OPTIMISTIC)
   - 장점: 충돌이 안나는 경우, Lock을 잡지 않기에 Pessimistic Lock보다 성능적 이점을 가지게 됨
   - 단점: update가 실패한 경우, 재시도 로직을 개발자가 직접 작성해야 함
4. Named Lock
   - 이름을 가진 metadata Lock
   - 이름을 가진 락을 힉득한 후, 해지될 때 까지 다른 세션은 이 락을 얻을 수 없습니다
   - 트랜잭션 종료 후, 별도로 락을 해지해줘야 하는 과정이 필요합니다.
   - 획득: @Query(value = "select get_lock(:key, 3000)", nativeQuery = true)
   - 반납: @Query(value = "select release_lock(:key, key)", nativeQuery = true)
   - 부모의 트랜잭션과 별도로 실행되기 위해 자식 트랜잭션에 @Transactional(propagation = Propagation.REQUIRES_NEW) 옵션 지정
5. Redis 
    - Lettuce
      - setnx(set if not exist) 명령어(setnx (key)(value))를 활용하여 분산락 구현 (key와 value를 set할 때 기존에 값이 없을 때만 set 하는 명령어)
      - setnx는 Spin Lock 방식이므로 Lock을 획득할 때 까지 재시도하는 로직을 개발자가 직접 작성
      - Spin Lock: Lock을 획득하려는 thread가 Lock을 획득할 수 있는지 확인하면서 반복적으로 시도하는 과정
      - 구현이 간단하고, 별도의 library가 필요하지 않습니다. Spin Lock 방식이기 대문에 동시에 많은 thread가 대기한다면, redis에 부하가 갈 수 있습니다.
    - Redisson
      - Pub/Sub 기반 Lock 구현 제공
      - 채널을 하나 만들고, 락을 획득한 스레드가 락을 해제한 경우에 대기중인 스레드에게 알려주면, 대기중인 스레드가 락 점유를 시도하는 방식
      - 별도의 Retry 로직을 작성할 필요가 없습니다.
      - 락 획득 재시도를 기본으로 제공하고, 별도의 라이브러리를 사용해야 합니다.
      - pub/sub 방식으로 구현되어 있기에 lettuce와 비교해서 redis에 부하가 덜 간다.