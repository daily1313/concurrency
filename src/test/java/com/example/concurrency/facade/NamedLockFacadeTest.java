package com.example.concurrency.facade;

import com.example.concurrency.domain.Stock;
import com.example.concurrency.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NamedLockFacadeTest {

    @Autowired
    private NamedLockFacade stockFacade;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    public void before() {
        Stock stock = new Stock(1L, 100L);
        stockRepository.saveAndFlush(stock);
    }

    @AfterEach
    public void after() {
        stockRepository.deleteAll();
    }


    @Test
    @DisplayName("Pessimistic LOCK 동시에_100개의_요청")
    public void Pessimistic_requests_100_AtTheSameTime() throws InterruptedException {
        int threadCount = 100;

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                        try {
                            stockFacade.decrease(1L, 1L);
                        } finally {
                            latch.countDown();
                        }
                    }
            );
        }

        latch.await();

        Stock stock = stockRepository.findById(1L).orElseThrow();

        //100 - (1*100) = 0
        assertThat(stock.getQuantity()).isEqualTo(0L);
    }
}