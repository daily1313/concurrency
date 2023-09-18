package com.example.concurrency.facade;

import com.example.concurrency.repository.RedisLockRepository;
import com.example.concurrency.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LettuceLockStockFacade  {

    private final RedisLockRepository redisLockRepository;
    private final StockService stockService;

    public void decrease(final Long key, final Long quantity) throws InterruptedException {
        // Lock 획득 시도
        while (!redisLockRepository.lock(key)) {
            //SpinLock 방식이 redis 에게 주는 부하를 줄여주기 위한 sleep
            Thread.sleep(100);
        }

        // lock 획득 성공시
        try{
            stockService.decrease(key,quantity);
        }finally {
            //락 해제
            redisLockRepository.unlock(key);
        }
    }
}
