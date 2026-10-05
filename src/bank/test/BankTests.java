package bank.test;

import bank.AccountNotFoundException;
import bank.Account;
import bank.Bank;
import bank.BankException;
import bank.CurrentAccount;
import bank.Customer;
import bank.InsufficientFundsException;
import bank.InvalidAmountException;
import bank.SavingsAccount;
import bank.Transaction;
import bank.TransactionType;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Простые тесты без внешних библиотек.
 * Запуск: java -cp out bank.test.BankTests
 */
public class BankTests {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        test("пополнение увеличивает баланс", BankTests::depositIncreasesBalance);
        test("снятие уменьшает баланс", BankTests::withdrawDecreasesBalance);
        test("нельзя снять больше, чем есть", BankTests::cannotOverdrawWithoutLimit);
        test("овердрафт на текущем счёте работает", BankTests::overdraftWorks);
        test("накопительный счёт не уходит в минус", BankTests::savingsCannotGoNegative);
        test("отрицательная и нулевая суммы запрещены", BankTests::invalidAmountsRejected);
        test("более двух знаков после запятой запрещены", BankTests::tooManyDecimalsRejected);
        test("перевод меняет оба счёта и пишет историю", BankTests::transferWorks);
        test("неудачный перевод ничего не меняет", BankTests::failedTransferChangesNothing);
        test("перевод на тот же счёт запрещён", BankTests::transferToSelfRejected);
        test("несуществующий счёт", BankTests::unknownAccount);
        test("проценты по накопительному счёту", BankTests::interestIsCalculated);
        test("точность денег (0.1 + 0.2 = 0.3)", BankTests::moneyIsExact);
        test("история операций нумеруется и неизменяема", BankTests::historyIsOrderedAndReadOnly);
        test("сохранение и загрузка", BankTests::saveAndLoad);

        System.out.println();
        System.out.println("Пройдено: " + passed + ", упало: " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    // --- сами тесты ---

    private static void depositIncreasesBalance() {
        Account a = newCurrent("0");
        a.deposit(money("1500.50"));
        assertMoney("1500.50", a.getBalance());
    }

    private static void withdrawDecreasesBalance() {
        Account a = newCurrent("0");
        a.deposit(money("1000"));
        a.withdraw(money("250.25"));
        assertMoney("749.75", a.getBalance());
    }

    private static void cannotOverdrawWithoutLimit() {
        Account a = newCurrent("0");
        a.deposit(money("100"));
        assertThrows(InsufficientFundsException.class, () -> a.withdraw(money("100.01")));
        assertMoney("100.00", a.getBalance());
    }

    private static void overdraftWorks() {
        Account a = newCurrent("500");
        a.deposit(money("100"));
        a.withdraw(money("600"));
        assertMoney("-500.00", a.getBalance());
        assertThrows(InsufficientFundsException.class, () -> a.withdraw(money("0.01")));
    }

    private static void savingsCannotGoNegative() {
        Bank bank = new Bank();
        SavingsAccount s = bank.openSavingsAccount(bank.registerCustomer("Тест", ""), money("0.08"));
        s.deposit(money("100"));
        assertThrows(InsufficientFundsException.class, () -> s.withdraw(money("100.01")));
    }

    private static void invalidAmountsRejected() {
        Account a = newCurrent("0");
        assertThrows(InvalidAmountException.class, () -> a.deposit(money("0")));
        assertThrows(InvalidAmountException.class, () -> a.deposit(money("-5")));
        assertThrows(InvalidAmountException.class, () -> a.deposit(null));
    }

    private static void tooManyDecimalsRejected() {
        Account a = newCurrent("0");
        assertThrows(InvalidAmountException.class, () -> a.deposit(money("10.005")));
        a.deposit(money("10.500")); // лишний ноль на конце допустим
        assertMoney("10.50", a.getBalance());
    }

    private static void transferWorks() {
        Bank bank = new Bank();
        Customer c = bank.registerCustomer("Алия", "");
        Account from = bank.openCurrentAccount(c, money("0"));
        Account to = bank.openCurrentAccount(c, money("0"));
        from.deposit(money("1000"));
        bank.transfer(from.getNumber(), to.getNumber(), money("300"));
        assertMoney("700.00", from.getBalance());
        assertMoney("300.00", to.getBalance());
        List<Transaction> out = from.getTransactions();
        List<Transaction> in = to.getTransactions();
        assertEquals(TransactionType.TRANSFER_OUT, out.get(out.size() - 1).type());
        assertEquals(TransactionType.TRANSFER_IN, in.get(in.size() - 1).type());
    }

    private static void failedTransferChangesNothing() {
        Bank bank = new Bank();
        Customer c = bank.registerCustomer("Алия", "");
        Account from = bank.openCurrentAccount(c, money("0"));
        Account to = bank.openCurrentAccount(c, money("0"));
        from.deposit(money("100"));
        assertThrows(InsufficientFundsException.class,
                () -> bank.transfer(from.getNumber(), to.getNumber(), money("500")));
        assertMoney("100.00", from.getBalance());
        assertMoney("0.00", to.getBalance());
        assertEquals(0, to.getTransactions().size());
    }

    private static void transferToSelfRejected() {
        Bank bank = new Bank();
        Account a = bank.openCurrentAccount(bank.registerCustomer("Тест", ""), money("0"));
        a.deposit(money("100"));
        assertThrows(BankException.class, () -> bank.transfer(a.getNumber(), a.getNumber(), money("10")));
    }

    private static void unknownAccount() {
        Bank bank = new Bank();
        assertThrows(AccountNotFoundException.class, () -> bank.getAccount("KZ99999999"));
        assertThrows(AccountNotFoundException.class, () -> bank.deposit("nope", money("10")));
    }

    private static void interestIsCalculated() {
        Bank bank = new Bank();
        SavingsAccount s = bank.openSavingsAccount(bank.registerCustomer("Тест", ""), money("0.12"));
        s.deposit(money("12000"));
        assertMoney("120.00", s.applyMonthlyInterest()); // 12000 * 12% / 12
        assertMoney("12120.00", s.getBalance());
        SavingsAccount empty = bank.openSavingsAccount(bank.registerCustomer("Пусто", ""), money("0.12"));
        assertMoney("0.00", empty.applyMonthlyInterest());
        assertEquals(0, empty.getTransactions().size());
    }

    private static void moneyIsExact() {
        Account a = newCurrent("0");
        a.deposit(money("0.1"));
        a.deposit(money("0.2"));
        assertMoney("0.30", a.getBalance());
    }

    private static void historyIsOrderedAndReadOnly() {
        Account a = newCurrent("0");
        a.deposit(money("10"));
        a.deposit(money("20"));
        List<Transaction> h = a.getTransactions();
        assertEquals(1L, h.get(0).id());
        assertEquals(2L, h.get(1).id());
        assertMoney("30.00", h.get(1).balanceAfter());
        assertThrows(UnsupportedOperationException.class, () -> h.remove(0));
    }

    private static void saveAndLoad() throws IOException, ClassNotFoundException {
        Bank bank = new Bank();
        Account a = bank.openCurrentAccount(bank.registerCustomer("Бекзат", "+7 700 000 00 00"), money("0"));
        a.deposit(money("777.70"));
        Path file = Files.createTempFile("bank-test", ".dat");
        try {
            Bank.save(bank, file);
            Bank loaded = Bank.load(file);
            assertMoney("777.70", loaded.getAccount(a.getNumber()).getBalance());
            assertEquals("Бекзат", loaded.getAccount(a.getNumber()).getOwner().fullName());
            // нумерация счетов продолжается, а не начинается заново
            Account next = loaded.openCurrentAccount(loaded.registerCustomer("Новый", ""), money("0"));
            assertEquals(false, next.getNumber().equals(a.getNumber()));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    // --- вспомогательные методы ---

    private static Account newCurrent(String overdraft) {
        Bank bank = new Bank();
        CurrentAccount account = bank.openCurrentAccount(bank.registerCustomer("Тест", ""), money(overdraft));
        return account;
    }

    private static BigDecimal money(String s) {
        return new BigDecimal(s);
    }

    private interface Check {
        void run() throws Exception;
    }

    private static void test(String name, Check check) {
        try {
            check.run();
            passed++;
            System.out.println("OK    " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("FAIL  " + name + " -> " + t);
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("ожидалось " + expected + ", получено " + actual);
        }
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        if (new BigDecimal(expected).compareTo(actual) != 0) {
            throw new AssertionError("ожидалось " + expected + ", получено " + actual.toPlainString());
        }
    }

    private static void assertThrows(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) {
                return;
            }
            throw new AssertionError("ожидалось " + type.getSimpleName() + ", получено " + t);
        }
        throw new AssertionError("ожидалось исключение " + type.getSimpleName() + ", но его не было");
    }
}
