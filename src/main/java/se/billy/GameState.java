package se.billy;

import se.billy.fact.Fact;
import se.billy.wordle.AttemptResult;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static se.billy.Config.WORD_LENGTH;

public class GameState {

    private final List<String> dictionary;

    public GameState(Stream<String> dictionary) {
        this.dictionary = dictionary
                .filter(it -> it.length() == WORD_LENGTH)
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
    }

    public String suggestWord() {
        if (dictionary.isEmpty()) {
            throw new IllegalStateException("No more words to attempt");
        }

        final ArrayList<String> tmp = new ArrayList<>(dictionary);
        dictionary.clear();
        dictionary.addAll(sort(tmp));
        System.out.println("Dictionary contains " + dictionary.size() + " words");
        return dictionary.remove(0);
    }

    //For hooking in sorting strategies
    private List<String> sort(List<String> dictionary) {
        Collections.shuffle(dictionary);
        return dictionary;
    }

    public void update(AttemptResult attemptResult) {
        attemptResult.charFacts().forEach(fact -> {
            switch (fact) {
                case Fact.Present f -> updatePresent(f);
                case Fact.NotInWord f -> dictionary.removeIf(word -> word.indexOf(f.letter()) != -1);
                case Fact.Correct f -> dictionary.removeIf(word -> {
                    final char[] chars = word.toCharArray();
                    return chars[f.position()] != f.letter();
                });
            }
            ;
        });
    }

    private void updatePresent(Fact.Present f) {
        char letter = f.letter();
        List<Integer> invalidPositions = f.invalidPositions();

        final String[] w = new String[]{""};
        final boolean removed = dictionary.removeIf(word -> {
            w[0] = word;
            return word.indexOf(letter) == -1 || invalidPositions.stream().anyMatch(index -> {
                final char indexInWord = word.charAt(index);
                return indexInWord == letter;
            });
        });

        if (removed && w[0].equals("prick")) {
            System.out.println("removing prick");
        }
    }

    @Override
    public String toString() {
        return "GameState{" +
                "dictionary=" + dictionary +
                '}';
    }

    public void removeWord(String word) {
        dictionary.remove(word);
    }

    public List<String> getDictionary() {
        return dictionary;
    }
}
