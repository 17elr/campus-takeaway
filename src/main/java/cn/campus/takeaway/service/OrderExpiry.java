package cn.campus.takeaway.service;

import cn.campus.takeaway.mapper.AppMapper;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Component
@ConditionalOnProperty(name="app.expiry-enabled", havingValue="true", matchIfMissing=true)
public class OrderExpiry {
    private final AppMapper mapper;
    private final TransactionTemplate transactions;
    public OrderExpiry(AppMapper mapper, PlatformTransactionManager manager) {
        this.mapper = mapper;
        this.transactions = new TransactionTemplate(manager);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(fixedDelay=30000, initialDelay=30000)
    public void scan() {
        // Keyset batches shrink after each committed conditional update, including startup recovery.
        for (var ids = mapper.expiredOrderIds(); !ids.isEmpty(); ids = mapper.expiredOrderIds()) {
            for (long id : ids) {
                try {
                    transactions.executeWithoutResult(status -> {
                        if (mapper.expireOrder(id) == 1) mapper.insertTimeoutEvent(id);
                    });
                } catch (RuntimeException failure) {
                    LoggerFactory.getLogger(getClass()).error("Timeout cancellation rolled back for order {}", id, failure);
                    return;
                }
            }
        }
    }
}
