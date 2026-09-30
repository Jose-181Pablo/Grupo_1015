package mx.grupo1015.chatia.exception;

public class IaServiceException extends RuntimeException {

    public IaServiceException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}