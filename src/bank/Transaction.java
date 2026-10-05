package bank;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Неизменяемая запись об операции (record). */
public record Transaction(
        long id,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        LocalDateTime time,
        String description) implements Serializable {
}
