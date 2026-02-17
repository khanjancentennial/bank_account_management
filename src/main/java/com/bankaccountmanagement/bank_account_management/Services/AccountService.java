package com.bankaccountmanagement.bank_account_management.Services;

// import com.bankaccountmanagement.bank_account_management.Model.UserModel;
// import com.bankaccountmanagement.bank_account_management.Repository.UserRepository;
// import jakarta.persistence.EntityNotFoundException;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.util.logging.Level;
// import java.util.logging.Logger;

// /**
//  * Service class for handling all account-related business logic, including
//  * deposits, withdrawals, and transfers.
//  * <p>
//  * It uses the UserRepository to interact with the database and ensures that
//  * all financial operations are handled correctly and are atomic where required.
//  */
// @Service
// @RequiredArgsConstructor
// public class AccountService {

//     private final UserRepository userRepository;
//     private static final Logger logger = Logger.getLogger(AccountService.class.getName());

//     /**
//      * Handles a deposit transaction.
//      *
//      * @param accountNumber The account number of the user to deposit into.
//      * @param amount        The amount to deposit.
//      * @throws EntityNotFoundException if the account does not exist.
//      */
//     public void deposit(String accountNumber, Double amount) {
//         logger.log(Level.INFO, "Processing deposit of {0} into account {1}", new Object[]{amount, accountNumber});

//         UserModel user = userRepository.findByAccountNumber(accountNumber)
//                 .orElseThrow(() -> new EntityNotFoundException("User not found with account number: " + accountNumber));

//         double newBalance = user.getBalance() + amount;
//         user.setBalance(newBalance);
//         userRepository.save(user);

//         logger.log(Level.INFO, "Deposit successful. New balance for account {0} is {1}", new Object[]{accountNumber, newBalance});
//     }

//     /**
//      * Handles a withdrawal transaction.
//      *
//      * @param accountNumber The account number of the user to withdraw from.
//      * @param amount        The amount to withdraw.
//      * @throws EntityNotFoundException  if the account does not exist.
//      * @throws IllegalArgumentException if the account has insufficient funds.
//      */
//     public void withdraw(String accountNumber, Double amount) {
//         logger.log(Level.INFO, "Processing withdrawal of {0} from account {1}", new Object[]{amount, accountNumber});

//         UserModel user = userRepository.findByAccountNumber(accountNumber)
//                 .orElseThrow(() -> new EntityNotFoundException("User not found with account number: " + accountNumber));

//         if (user.getBalance() < amount) {
//             logger.log(Level.WARNING, "Insufficient funds for withdrawal. Account {0} balance: {1}, requested amount: {2}",
//                     new Object[]{accountNumber, user.getBalance(), amount});
//             throw new IllegalArgumentException("Insufficient funds.");
//         }

//         double newBalance = user.getBalance() - amount;
//         user.setBalance(newBalance);
//         userRepository.save(user);

//         logger.log(Level.INFO, "Withdrawal successful. New balance for account {0} is {1}", new Object[]{accountNumber, newBalance});
//     }

//     /**
//      * Handles a transfer transaction between two accounts.
//      * This method is transactional to ensure both the withdrawal and deposit
//      * happen as a single, atomic operation.
//      *
//      * @param senderAccountNumber   The account number to withdraw from.
//      * @param receiverAccountNumber The account number to deposit into.
//      * @param amount                The amount to transfer.
//      * @throws EntityNotFoundException  if either account does not exist.
//      * @throws IllegalArgumentException if the sender account has insufficient funds.
//      */
//     @Transactional
//     public void transfer(String senderAccountNumber, String receiverAccountNumber, Double amount) {
//         logger.log(Level.INFO, "Processing transfer of {0} from account {1} to {2}", new Object[]{amount, senderAccountNumber, receiverAccountNumber});

//         // Step 1: Withdraw from the sender's account
//         withdraw(senderAccountNumber, amount);

//         // Step 2: Deposit into the receiver's account
//         deposit(receiverAccountNumber, amount);

//         logger.log(Level.INFO, "Transfer successful. Transferred {0} from {1} to {2}", new Object[]{amount, senderAccountNumber, receiverAccountNumber});
//     }
// }



import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.bankaccountmanagement.bank_account_management.Execptions.InsufficientFundsException;
import com.bankaccountmanagement.bank_account_management.Model.UserModel;
import com.bankaccountmanagement.bank_account_management.Repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Service class for handling all account-related business logic.
 * Includes validation, audit logging, and concurrency control.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private static final Logger logger = Logger.getLogger(AccountService.class.getName());

    // Business rule constants
    private static final double MAX_TRANSACTION_AMOUNT = 50000.00;
    private static final double MIN_TRANSACTION_AMOUNT = 0.01;
    private static final double MIN_BALANCE = 0.00;

    /**
     * Handles a deposit transaction with validation and audit logging.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE) // Prevent race conditions
    public void deposit(String accountNumber, Double amount) {
        logger.log(Level.INFO, "Processing deposit of {0} into account {1}", 
            new Object[]{amount, accountNumber});

        // Validation
        validateTransactionAmount(amount);

        UserModel user = userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    auditService.logFailure(accountNumber, "DEPOSIT", 
                        "Account not found: " + accountNumber);
                    return new EntityNotFoundException("Account not found: " + accountNumber);
                });

        try {
            double newBalance = user.getBalance() + amount;
            user.setBalance(newBalance);
            userRepository.save(user);

            // Audit log
            auditService.logTransaction(
                user.getId(), 
                user.getUsername(), 
                "DEPOSIT", 
                accountNumber, 
                amount, 
                "SUCCESS", 
                "Deposit successful. New balance: " + newBalance
            );

            logger.log(Level.INFO, "Deposit successful. New balance: {0}", newBalance);
        } catch (Exception e) {
            auditService.logTransaction(
                user.getId(), 
                user.getUsername(), 
                "DEPOSIT", 
                accountNumber, 
                amount, 
                "FAILURE", 
                "Deposit failed: " + e.getMessage()
            );
            throw e;
        }
    }

    /**
     * Handles a withdrawal transaction with validation and audit logging.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void withdraw(String accountNumber, Double amount) {
        logger.log(Level.INFO, "Processing withdrawal of {0} from account {1}", 
            new Object[]{amount, accountNumber});

        // Validation
        validateTransactionAmount(amount);

        UserModel user = userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    auditService.logFailure(accountNumber, "WITHDRAWAL", 
                        "Account not found: " + accountNumber);
                    return new EntityNotFoundException("Account not found: " + accountNumber);
                });

        try {
            if (user.getBalance() < amount) {
                auditService.logTransaction(
                    user.getId(), 
                    user.getUsername(), 
                    "WITHDRAWAL", 
                    accountNumber, 
                    amount, 
                    "FAILURE", 
                    "Insufficient funds. Balance: " + user.getBalance()
                );
                throw new InsufficientFundsException(
                    "Insufficient funds. Available: " + user.getBalance() + ", Requested: " + amount);
            }

            double newBalance = user.getBalance() - amount;
            user.setBalance(newBalance);
            userRepository.save(user);

            // Audit log
            auditService.logTransaction(
                user.getId(), 
                user.getUsername(), 
                "WITHDRAWAL", 
                accountNumber, 
                amount, 
                "SUCCESS", 
                "Withdrawal successful. New balance: " + newBalance
            );

            logger.log(Level.INFO, "Withdrawal successful. New balance: {0}", newBalance);
        } catch (InsufficientFundsException e) {
            throw e;
        } catch (Exception e) {
            auditService.logTransaction(
                user.getId(), 
                user.getUsername(), 
                "WITHDRAWAL", 
                accountNumber, 
                amount, 
                "FAILURE", 
                "Withdrawal failed: " + e.getMessage()
            );
            throw e;
        }
    }

    /**
     * Handles a transfer transaction between two accounts.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void transfer(String senderAccountNumber, String receiverAccountNumber, Double amount) {
        logger.log(Level.INFO, "Processing transfer of {0} from {1} to {2}", 
            new Object[]{amount, senderAccountNumber, receiverAccountNumber});

        // Validation
        validateTransactionAmount(amount);

        if (senderAccountNumber.equals(receiverAccountNumber)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // Verify both accounts exist before starting transaction
        UserModel sender = userRepository.findByAccountNumber(senderAccountNumber)
                .orElseThrow(() -> new EntityNotFoundException(
                    "Sender account not found: " + senderAccountNumber));
        
        UserModel receiver = userRepository.findByAccountNumber(receiverAccountNumber)
                .orElseThrow(() -> new EntityNotFoundException(
                    "Receiver account not found: " + receiverAccountNumber));

        try {
            // Step 1: Withdraw from sender
            withdraw(senderAccountNumber, amount);

            // Step 2: Deposit to receiver
            deposit(receiverAccountNumber, amount);

            // Audit log for transfer
            auditService.logTransaction(
                sender.getId(), 
                sender.getUsername(), 
                "TRANSFER_SEND", 
                senderAccountNumber, 
                amount, 
                "SUCCESS", 
                "Transfer to " + receiverAccountNumber + " successful"
            );

            auditService.logTransaction(
                receiver.getId(), 
                receiver.getUsername(), 
                "TRANSFER_RECEIVE", 
                receiverAccountNumber, 
                amount, 
                "SUCCESS", 
                "Transfer from " + senderAccountNumber + " received"
            );

            logger.log(Level.INFO, "Transfer successful");
        } catch (Exception e) {
            auditService.logTransaction(
                sender.getId(), 
                sender.getUsername(), 
                "TRANSFER_SEND", 
                senderAccountNumber, 
                amount, 
                "FAILURE", 
                "Transfer failed: " + e.getMessage()
            );
            throw e;
        }
    }

    /**
     * Validates transaction amount against business rules
     */
    private void validateTransactionAmount(Double amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Transaction amount cannot be null");
        }
        if (amount < MIN_TRANSACTION_AMOUNT) {
            throw new IllegalArgumentException(
                "Transaction amount must be at least " + MIN_TRANSACTION_AMOUNT);
        }
        if (amount > MAX_TRANSACTION_AMOUNT) {
            throw new IllegalArgumentException(
                "Transaction amount cannot exceed " + MAX_TRANSACTION_AMOUNT);
        }
    }
}