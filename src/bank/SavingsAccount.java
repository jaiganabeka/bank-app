package bank;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Накопительный счёт: в минус уходить нельзя, раз в месяц начисляются проценты. */
public class SavingsAccount extends Account {
    private static final long serialVersionUID = 1L;

    private final BigDecimal annualRate; // например, 0.08 = 8% годовых

    public SavingsAccount(String number, Customer owner, BigDecimal annualRate) {
        super(number, owner);
        if (annualRate == null || annualRate.signum() < 0 || annualRate.compareTo(BigDecimal.ONE) > 0) {
            throw new InvalidAmountException("Ставка должна быть от 0 до 100% годовых");
        }
        this.annualRate = annualRate;
    }

    public BigDecimal getAnnualRate() {
        return annualRate;
    }

    @Override
    public String getTypeName() {
        return "Накопительный";
    }

    @Override
    public BigDecimal getAvailableFunds() {
        return getBalance();
    }

    /** Начисляет проценты за месяц и возвращает начисленную сумму (0, если начислять нечего). */
    public BigDecimal applyMonthlyInterest() {
        BigDecimal interest = getBalance()
                .multiply(annualRate)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        if (interest.signum() > 0) {
            credit(interest, TransactionType.INTEREST, "Проценты за месяц");
        }
        return interest;
    }
}
