package bank;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/** Консольное меню банковского приложения. */
public class Main {
    private static final Path DATA_FILE = Path.of("data", "bank.dat");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static Scanner in;
    private static Bank bank;

    public static void main(String[] args) {
        // Кодировка ввода: на Windows она может отличаться от UTF-8
        String encoding = System.getProperty("stdin.encoding", Charset.defaultCharset().name());
        in = new Scanner(System.in, encoding);
        bank = loadBank();

        System.out.println("=== Банковское приложение ===");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = ask("Выберите пункт");
            if (choice == null) {
                break; // конец ввода
            }
            try {
                switch (choice) {
                    case "1" -> createCustomerAndAccount();
                    case "2" -> deposit();
                    case "3" -> withdraw();
                    case "4" -> transfer();
                    case "5" -> printStatement();
                    case "6" -> listAccounts();
                    case "7" -> applyInterest();
                    case "0" -> running = false;
                    default -> System.out.println("Нет такого пункта");
                }
                if (!choice.equals("0")) {
                    saveBank();
                }
            } catch (BankException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
        System.out.println("До свидания!");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("1. Новый клиент и счёт");
        System.out.println("2. Пополнить счёт");
        System.out.println("3. Снять со счёта");
        System.out.println("4. Перевод между счетами");
        System.out.println("5. Выписка по счёту");
        System.out.println("6. Все счета");
        System.out.println("7. Начислить проценты по накопительным счетам");
        System.out.println("0. Выход");
    }

    private static void createCustomerAndAccount() {
        String name = ask("ФИО клиента");
        String phone = ask("Телефон");
        Customer customer = bank.registerCustomer(name, phone);
        String type = ask("Тип счёта: 1 - текущий, 2 - накопительный");
        Account account;
        if ("2".equals(type)) {
            BigDecimal percent = readAmount("Ставка, % годовых (например, 8)");
            account = bank.openSavingsAccount(customer, percent.movePointLeft(2));
        } else {
            String limit = ask("Лимит овердрафта (Enter = 0)");
            BigDecimal overdraft = (limit == null || limit.isBlank()) ? BigDecimal.ZERO : parseAmount(limit);
            account = bank.openCurrentAccount(customer, overdraft);
        }
        System.out.println("Счёт открыт: " + account);
    }

    private static void deposit() {
        String number = ask("Номер счёта");
        bank.deposit(number, readAmount("Сумма"));
        System.out.println("Готово. " + bank.getAccount(number));
    }

    private static void withdraw() {
        String number = ask("Номер счёта");
        bank.withdraw(number, readAmount("Сумма"));
        System.out.println("Готово. " + bank.getAccount(number));
    }

    private static void transfer() {
        String from = ask("Со счёта");
        String to = ask("На счёт");
        bank.transfer(from, to, readAmount("Сумма"));
        System.out.println("Перевод выполнен");
        System.out.println(bank.getAccount(from));
        System.out.println(bank.getAccount(to));
    }

    private static void printStatement() {
        Account account = bank.getAccount(ask("Номер счёта"));
        System.out.println(account);
        List<Transaction> list = account.getTransactions();
        if (list.isEmpty()) {
            System.out.println("Операций пока нет");
            return;
        }
        System.out.printf("%-3s %-16s %-22s %12s %12s%n", "№", "Дата", "Операция", "Сумма", "Баланс");
        for (Transaction t : list) {
            System.out.printf("%-3d %-16s %-22s %12s %12s%n",
                    t.id(), t.time().format(TIME_FORMAT), t.type().getTitle(),
                    t.amount().toPlainString(), t.balanceAfter().toPlainString());
        }
    }

    private static void listAccounts() {
        List<Account> accounts = bank.getAccounts();
        if (accounts.isEmpty()) {
            System.out.println("Счетов пока нет");
        }
        accounts.forEach(System.out::println);
    }

    private static void applyInterest() {
        System.out.println("Начислено процентов всего: " + bank.applyMonthlyInterest().toPlainString() + " тг");
    }

    // --- ввод и сохранение ---

    private static String ask(String prompt) {
        System.out.print(prompt + ": ");
        return in.hasNextLine() ? in.nextLine().trim() : null;
    }

    private static BigDecimal readAmount(String prompt) {
        return parseAmount(ask(prompt));
    }

    private static BigDecimal parseAmount(String text) {
        try {
            return new BigDecimal(text.replace(',', '.').replace(" ", ""));
        } catch (NumberFormatException | NullPointerException e) {
            throw new InvalidAmountException("Введите число, например 1500 или 1500.50");
        }
    }

    private static Bank loadBank() {
        if (Files.exists(DATA_FILE)) {
            try {
                Bank loaded = Bank.load(DATA_FILE);
                System.out.println("Данные загружены из " + DATA_FILE);
                return loaded;
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Не удалось прочитать " + DATA_FILE + ", начинаем с чистого листа");
            }
        }
        return new Bank();
    }

    private static void saveBank() {
        try {
            Bank.save(bank, DATA_FILE);
        } catch (IOException e) {
            System.out.println("Не удалось сохранить данные: " + e.getMessage());
        }
    }
}
