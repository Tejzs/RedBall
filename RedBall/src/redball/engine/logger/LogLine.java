package redball.engine.logger;

public class LogLine {
    private String message;
    private boolean isError;

    public LogLine(String message, boolean isError) {
        this.message = message;
        this.isError = isError;
    }

    public boolean isError() {
        return isError;
    }

    public String getMessage() {
        return message;
    }
}
