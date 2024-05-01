package br.com.arthurrocha.picpaydesafiobackend.authorization;

public class UnauthorizedTransactionException extends RuntimeException{
    public UnauthorizedTransactionException(String message) {
        super(message);
    }
}
