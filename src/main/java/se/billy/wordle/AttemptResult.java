package se.billy.wordle;

import se.billy.fact.Fact;

import java.util.List;

public record AttemptResult(String attemptedWord, List<Fact> charFacts){
    public boolean isCorrect() {
        return charFacts.stream().allMatch(it -> it instanceof Fact.Correct);
    }
}
