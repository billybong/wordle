package se.billy;

import se.billy.wordle.NotAWordException;
import se.billy.wordle.AttemptResult;
import se.billy.wordle.WordleClientImpl;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class WordleBot implements Closeable {

    private final GameState gameState;
    private final WordleClientImpl wordleClient;

    public WordleBot() {
        gameState = new GameState(loadWords());
        wordleClient = new WordleClientImpl();
    }

    public static void main(String[] args) {
        var bot = new WordleBot();
        bot.play();
    }

    private void play() {
        for (int i = 0; i < Config.ATTEMPTS; i++) {
            final AttemptResult result = retry(() -> wordleClient.attemptWord(gameState.suggestWord()));
            if (result.isCorrect()) {
                System.out.println("You won at attempt " + (i + 1) + "! Correct word is: " + result.attemptedWord());
                return;
            }

            gameState.update(result);
        }
        System.err.println("You lost. Words not tried: " + gameState.getDictionary());
    }

    private AttemptResult retry(WordAttempt attempt) {
        while (true) {
            try {
                return attempt.run();
            } catch (NotAWordException e) {
                gameState.removeWord(e.word);
            }
        }
    }

    private Stream<String> loadWords() {
        try {
            return Files.lines(Paths.get("src/main/resources/words.txt"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        wordleClient.close();
    }

    private interface WordAttempt {
        AttemptResult run() throws NotAWordException;
    }
}
