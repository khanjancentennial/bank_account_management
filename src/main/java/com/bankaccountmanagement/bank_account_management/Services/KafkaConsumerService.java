package com.bankaccountmanagement.bank_account_management.Services;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.bankaccountmanagement.bank_account_management.Event.TransactionEvent;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final AccountService accountService;
    private static final Logger logger = Logger.getLogger(KafkaConsumerService.class.getName());

    @KafkaListener(topics = "${kafka.topic.transaction}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTransactionEvent(TransactionEvent event) {
        logger.log(Level.INFO, "Consumed transaction event: {0}", event);

        try {
            switch (event.getTransactionType().toUpperCase()) {
                case "DEPOSIT" -> {
                    accountService.deposit(event.getReceiverAccountNumber(), event.getAmount());
                    logger.log(Level.INFO, "Deposit of {0} successful for account: {1}",
                            new Object[]{event.getAmount(), event.getReceiverAccountNumber()});
                }
                case "WITHDRAWAL" -> {
                    accountService.withdraw(event.getSenderAccountNumber(), event.getAmount());
                    logger.log(Level.INFO, "Withdrawal of {0} successful from account: {1}",
                            new Object[]{event.getAmount(), event.getSenderAccountNumber()});
                }
                case "TRANSFER" -> {
                    accountService.transfer(event.getSenderAccountNumber(), event.getReceiverAccountNumber(), event.getAmount());
                    logger.log(Level.INFO, "Transfer of {0} from account {1} to {2} successful",
                            new Object[]{event.getAmount(), event.getSenderAccountNumber(), event.getReceiverAccountNumber()});
                }
                default -> logger.log(Level.WARNING, "Unknown transaction type: {0} for event: {1}",
                        new Object[]{event.getTransactionType(), event});
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing transaction event: {0}", e.getMessage());
            // You may want to implement a Dead Letter Queue (DLQ) here for failed events.
        }
    }
}