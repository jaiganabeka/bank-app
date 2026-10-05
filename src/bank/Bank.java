package bank;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Банк: хранит клиентов и счета и выполняет операции между ними. */
public class Bank implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Long, Customer> customers = new LinkedHashMap<>();
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private long nextCustomerId = 1;
    private long nextAccountNumber = 1;

    public Customer registerCustomer(String fullName, String phone) {
        if (fullName == null || fullName.isBlank()) {
            throw new BankException("Имя клиента не может быть пустым");
        }
        Customer customer = new Customer(nextCustomerId++, fullName.trim(), phone == null ? "" : phone.trim());
        customers.put(customer.id(), customer);
        return customer;
    }

    public CurrentAccount openCurrentAccount(Customer customer, BigDecimal overdraftLimit) {
        CurrentAccount account = new CurrentAccount(newAccountNumber(), customer, overdraftLimit);
        accounts.put(account.getNumber(), account);
        return account;
    }

    public SavingsAccount openSavingsAccount(Customer customer, BigDecimal annualRate) {
        SavingsAccount account = new SavingsAccount(newAccountNumber(), customer, annualRate);
        accounts.put(account.getNumber(), account);
        return account;
    }

    public Account getAccount(String number) {
        Account account = accounts.get(number == null ? "" : number.trim());
        if (account == null) {
            throw new AccountNotFoundException(number);
        }
        return account;
    }

    public void deposit(String number, BigDecimal amount) {
        getAccount(number).deposit(amount);
    }

    public void withdraw(String number, BigDecimal amount) {
        getAccount(number).withdraw(amount);
    }

    /**
     * Перевод между счетами. Сначала проверяются оба счёта и сумма, затем идёт списание.
     * Если списание не удалось (не хватило средств), у получателя ничего не зачисляется.
     */
    public void transfer(String fromNumber, String toNumber, BigDecimal amount) {
        Account from = getAccount(fromNumber);
        Account to = getAccount(toNumber);
        if (from == to) {
            throw new BankException("Нельзя перевести деньги на тот же счёт");
        }
        Account.normalizeAmount(amount);
        from.debit(amount, TransactionType.TRANSFER_OUT, "Перевод на счёт " + to.getNumber());
        to.credit(amount, TransactionType.TRANSFER_IN, "Перевод со счёта " + from.getNumber());
    }

    /** Начисляет проценты по всем накопительным счетам. Возвращает общую сумму начисленного. */
    public BigDecimal applyMonthlyInterest() {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Account account : accounts.values()) {
            if (account instanceof SavingsAccount savings) {
                total = total.add(savings.applyMonthlyInterest());
            }
        }
        return total;
    }

    public List<Account> getAccounts() {
        return new ArrayList<>(accounts.values());
    }

    private String newAccountNumber() {
        return String.format("KZ%08d", nextAccountNumber++);
    }

    // --- сохранение в файл (Java-сериализация) ---

    public static void save(Bank bank, Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(bank);
        }
    }

    public static Bank load(Path file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            return (Bank) in.readObject();
        }
    }
}
