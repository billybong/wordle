package se.billy.wordle;

public class NotAWordException extends Throwable {
    public final String word;

    public NotAWordException(final String word) {
        super(word + " is not a word");
        this.word = word;
    }
}
