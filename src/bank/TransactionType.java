package bank;

/** Тип операции по счёту. */
public enum TransactionType {
    DEPOSIT("Пополнение"),
    WITHDRAWAL("Снятие"),
    TRANSFER_IN("Перевод (входящий)"),
    TRANSFER_OUT("Перевод (исходящий)"),
    INTEREST("Начисление процентов");

    private final String title;

    TransactionType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
