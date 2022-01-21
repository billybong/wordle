package se.billy.fact;

import java.util.List;

public sealed interface Fact permits Fact.Correct, Fact.NotInWord, Fact.Present {
    char letter();

    record NotInWord(char letter) implements Fact {
    }

    record Correct(char letter, int position) implements Fact {
    }

    record Present(char letter, List<Integer> invalidPositions) implements Fact {
    }
}