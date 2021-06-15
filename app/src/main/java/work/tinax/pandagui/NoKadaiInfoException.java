package work.tinax.pandagui;

public class NoKadaiInfoException extends PandAAPIException {
    public NoKadaiInfoException(String string, Throwable cause) {
        super(string, cause);
    }

    public NoKadaiInfoException(String msg) {
        super(msg);
    }
}
