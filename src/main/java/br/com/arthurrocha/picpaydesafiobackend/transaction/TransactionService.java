package br.com.arthurrocha.picpaydesafiobackend.transaction;

import br.com.arthurrocha.picpaydesafiobackend.authorization.AuthorizeService;
import br.com.arthurrocha.picpaydesafiobackend.notification.NotificationService;
import br.com.arthurrocha.picpaydesafiobackend.wallet.Wallet;
import br.com.arthurrocha.picpaydesafiobackend.wallet.WalletRepository;
import br.com.arthurrocha.picpaydesafiobackend.wallet.WalletType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final AuthorizeService authorizeService;
    private final NotificationService notificationService;


    public TransactionService(TransactionRepository transactionRepository,
                              WalletRepository walletRepository, AuthorizeService authorizeService, NotificationService notificationService) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.authorizeService = authorizeService;
        this.notificationService = notificationService;
    }

    @Transactional //se uma operação falhar, será feito rollback de todas as operações feitas em BD
    public Transaction create(Transaction transaction){
        //1 - validar
        validate(transaction);


        //2 - criar a transação
        var newTransaction = transactionRepository.save(transaction);


        //3 - debitar da carteira
        var walletPayer = walletRepository.findById(transaction.payer()).get();
        var walletPayee = walletRepository.findById(transaction.payer()).get();
        walletRepository.save(walletPayer.debit(transaction.value()));
        walletRepository.save(walletPayee.credit(transaction.value()));


        //4 - chamar serviços externos
        //authorize transaction
        authorizeService.authorize(transaction);


        // notificação
        notificationService.notify(transaction);

        return newTransaction;
    }


    private void validate(Transaction transaction) {
        walletRepository.findById(transaction.payee())
                .map(payee -> walletRepository.findById(transaction.payer())
                        .map(payer -> isTransactionValid(transaction, payer) ? transaction : null)
                        .orElseThrow(() -> new InvalidTransactionException("Invalid transaction - %s"
                                .formatted(transaction) )))
                .orElseThrow(() -> new InvalidTransactionException("Invalid transaction - %s"
                        .formatted(transaction)));
    }

    private boolean isTransactionValid(Transaction transaction, Wallet payer) {
        return payer.type() == WalletType.COMUM.getValue() &&
                payer.balance().compareTo(transaction.value()) >= 0 &&
                !payer.id().equals(transaction.payee());
    }

    public List<Transaction> list() {
        return transactionRepository.findAll();
    }
}
