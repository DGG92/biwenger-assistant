package com.artajerjes.biwengerassistant.credential;

public class InvalidBiwengerCredentialException
        extends RuntimeException {

    public InvalidBiwengerCredentialException(Throwable cause) {
        super("Invalid Biwenger credential", cause);
    }
}