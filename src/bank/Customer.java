package bank;

import java.io.Serializable;

/** Клиент банка. */
public record Customer(long id, String fullName, String phone) implements Serializable {
}
