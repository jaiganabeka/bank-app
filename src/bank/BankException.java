package bank;

/** Базовое исключение для всех ошибок банковских операций. */
public class BankException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BankException(String message) {
        super(message);
    }
}
