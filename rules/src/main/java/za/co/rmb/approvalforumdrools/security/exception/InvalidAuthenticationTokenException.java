package za.co.rmb.approvalforumdrools.security.exception;

public class InvalidAuthenticationTokenException extends RuntimeException {

    public InvalidAuthenticationTokenException(String message, Throwable cause) {
        super(message, cause);
    }

}
