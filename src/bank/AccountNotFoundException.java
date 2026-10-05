package bank;

/** Счёт с таким номером не найден. */
public class AccountNotFoundException extends BankException {
    private static final long serialVersionUID = 1L;

    public AccountNotFoundException(String number) {
        super("Счёт не найден: " + number);
    }
}
