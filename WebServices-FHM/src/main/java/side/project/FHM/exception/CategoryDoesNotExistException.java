package side.project.FHM.exception;

public class CategoryDoesNotExistException extends Exception {
    public CategoryDoesNotExistException() {
    }

    public CategoryDoesNotExistException(String message) {
        super(message);
    }

    public CategoryDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

    public CategoryDoesNotExistException(Throwable cause) {
        super(cause);
    }

    public CategoryDoesNotExistException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
