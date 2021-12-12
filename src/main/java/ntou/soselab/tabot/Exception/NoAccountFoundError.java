package ntou.soselab.tabot.Exception;

public class NoAccountFoundError extends Exception{
    public NoAccountFoundError(String errorMsg){
        super(errorMsg);
    }
}
