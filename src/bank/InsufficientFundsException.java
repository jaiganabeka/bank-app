package bank;

import java.math.BigDecimal;

/** На счёте недостаточно средств. */
public class InsufficientFundsException extends BankException {
    private static final long serialVersionUID = 1L;

    public InsufficientFundsException(String accountNumber, BigDecimal available, BigDecimal requested) {
        super("Недостаточно средств на счёте " + accountNumber
                + ": доступно " + available.toPlainString()
                + ", требуется " + requested.toPlainString());
    }
}
