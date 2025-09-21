package com.bankaccountmanagement.bank_account_management.Services;

import com.bankaccountmanagement.bank_account_management.Model.UserModel;
import com.bankaccountmanagement.bank_account_management.Repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class for handling all account-related business logic, including
 * deposits, withdrawals, and transfers.
 * <p>
 * It uses the UserRepository to interact with the database and ensures that
 * all financial operations are handled correctly and are atomic where required.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private static final Logger logger = Logger.getLogger(AccountService.class.getName());

    /**
     * Handles a deposit transaction.
     *
     * @param accountNumber The account number of the user to deposit into.
     * @param amount        The amount to deposit.
     * @throws EntityNotFoundException if the account does not exist.
     */
    public void deposit(String accountNumber, Double amount) {
        logger.log(Level.INFO, "Processing deposit of {0} into account {1}", new Object[]{amount, accountNumber});

        UserModel user = userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new EntityNotFoundException("User not found with account number: " + accountNumber));

        double newBalance = user.getBalance() + amount;
        user.setBalance(newBalance);
        userRepository.save(user);

        logger.log(Level.INFO, "Deposit successful. New balance for account {0} is {1}", new Object[]{accountNumber, newBalance});
    }

    /**
     * Handles a withdrawal transaction.
     *
     * @param accountNumber The account number of the user to withdraw from.
     * @param amount        The amount to withdraw.
     * @throws EntityNotFoundException  if the account does not exist.
     * @throws IllegalArgumentException if the account has insufficient funds.
     */
    public void withdraw(String accountNumber, Double amount) {
        logger.log(Level.INFO, "Processing withdrawal of {0} from account {1}", new Object[]{amount, accountNumber});

        UserModel user = userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new EntityNotFoundException("User not found with account number: " + accountNumber));

        if (user.getBalance() < amount) {
            logger.log(Level.WARNING, "Insufficient funds for withdrawal. Account {0} balance: {1}, requested amount: {2}",
                    new Object[]{accountNumber, user.getBalance(), amount});
            throw new IllegalArgumentException("Insufficient funds.");
        }

        double newBalance = user.getBalance() - amount;
        user.setBalance(newBalance);
        userRepository.save(user);

        logger.log(Level.INFO, "Withdrawal successful. New balance for account {0} is {1}", new Object[]{accountNumber, newBalance});
    }

    /**
     * Handles a transfer transaction between two accounts.
     * This method is transactional to ensure both the withdrawal and deposit
     * happen as a single, atomic operation.
     *
     * @param senderAccountNumber   The account number to withdraw from.
     * @param receiverAccountNumber The account number to deposit into.
     * @param amount                The amount to transfer.
     * @throws EntityNotFoundException  if either account does not exist.
     * @throws IllegalArgumentException if the sender account has insufficient funds.
     */
    @Transactional
    public void transfer(String senderAccountNumber, String receiverAccountNumber, Double amount) {
        logger.log(Level.INFO, "Processing transfer of {0} from account {1} to {2}", new Object[]{amount, senderAccountNumber, receiverAccountNumber});

        // Step 1: Withdraw from the sender's account
        withdraw(senderAccountNumber, amount);

        // Step 2: Deposit into the receiver's account
        deposit(receiverAccountNumber, amount);

        logger.log(Level.INFO, "Transfer successful. Transferred {0} from {1} to {2}", new Object[]{amount, senderAccountNumber, receiverAccountNumber});
    }
}
