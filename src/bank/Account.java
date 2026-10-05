package bank;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Базовый банковский счёт.
 *
 * Деньги хранятся в BigDecimal, а не в double, чтобы не было ошибок округления.
 * Изменять баланс можно только через методы счёта, поэтому каждая операция
 * обязательно попадает в историю.
 */
public abstract class Account implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String number;
    private final Customer owner;
    private BigDecimal balance = BigDecimal.ZERO.setScale(2);
    private final List<Transaction> transactions = new ArrayList<>();
    private long nextTransactionId = 1;

    protected Account(String number, Customer owner) {
        this.number = number;
        this.owner = owner;
    }

    /** Название типа счёта для вывода. */
    public abstract String getTypeName();

    /** Сколько можно потратить прямо сейчас (у разных типов счетов по-разному). */
    public abstract BigDecimal getAvailableFunds();

    public String getNumber() {
        return number;
    }

    public Customer getOwner() {
        return owner;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public void deposit(BigDecimal amount) {
        credit(amount, TransactionType.DEPOSIT, "Пополнение счёта");
    }

    public void withdraw(BigDecimal amount) {
        debit(amount, TransactionType.WITHDRAWAL, "Снятие средств");
    }

    /** Зачисление. Доступно только внутри пакета (используется банком для переводов). */
    void credit(BigDecimal amount, TransactionType type, String description) {
        BigDecimal value = normalizeAmount(amount);
        balance = balance.add(value);
        record(type, value, description);
    }

    /** Списание. Если средств не хватает, баланс не меняется. */
    void debit(BigDecimal amount, TransactionType type, String description) {
        BigDecimal value = normalizeAmount(amount);
        if (value.compareTo(getAvailableFunds()) > 0) {
            throw new InsufficientFundsException(number, getAvailableFunds(), value);
        }
        balance = balance.subtract(value);
        record(type, value, description);
    }

    private void record(TransactionType type, BigDecimal amount, String description) {
        transactions.add(new Transaction(
                nextTransactionId++, type, amount, balance, LocalDateTime.now(), description));
    }

    /** Проверяет сумму: больше нуля, не более двух знаков после запятой. */
    static BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Сумма должна быть больше нуля");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new InvalidAmountException("Допускается не более двух знаков после запятой");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    @Override
    public String toString() {
        return getTypeName() + " " + number + " (" + owner.fullName() + "): " + balance.toPlainString() + " тг";
    }
}
