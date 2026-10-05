package bank;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Текущий счёт. Может иметь овердрафт (разрешённый уход в минус). */
public class CurrentAccount extends Account {
    private static final long serialVersionUID = 1L;

    private final BigDecimal overdraftLimit;

    public CurrentAccount(String number, Customer owner, BigDecimal overdraftLimit) {
        super(number, owner);
        if (overdraftLimit == null || overdraftLimit.signum() < 0) {
            throw new InvalidAmountException("Овердрафт не может быть отрицательным");
        }
        this.overdraftLimit = overdraftLimit.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    @Override
    public String getTypeName() {
        return "Текущий";
    }

    @Override
    public BigDecimal getAvailableFunds() {
        return getBalance().add(overdraftLimit);
    }
}
