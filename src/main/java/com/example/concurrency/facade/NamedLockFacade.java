package com.example.concurrency.facade;

import com.example.concurrency.repository.LockRepository;
import com.example.concurrency.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Component
@RequiredArgsConstructor
public class NamedLockFacade {

    private final LockRepository lockRepository;
    private final StockService stockService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decrease(final Long id, final Long quantity) {
        try {
            lockRepository.getLock(id.toString());
            stockService.decrease(id, quantity);
        } finally {
            //락의 해제
            lockRepository.releaseLock(id.toString());
        }
    }
}