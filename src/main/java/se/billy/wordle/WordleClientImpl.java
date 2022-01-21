package se.billy.wordle;

import com.microsoft.playwright.*;
import se.billy.Config;
import se.billy.fact.Fact;

import java.util.*;
import java.util.stream.Stream;

public class WordleClientImpl implements AutoCloseable {

    private final Playwright playwright;
    private final Page page;

    public WordleClientImpl() {
        playwright = Playwright.create();
        Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        BrowserContext context = browser.newContext();
        page = context.newPage();
        page.navigate("https://www.powerlanguage.co.uk/wordle/");
        page.click(".close-icon");
    }

    public AttemptResult attemptWord(String word) throws NotAWordException {
        for (char c : word.toLowerCase().toCharArray()) {
            selectLetter(c);
        }
        enter();
        System.out.println("Attempted word: " + word);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            return new AttemptResult(word, determineLetterState());
        } catch (NotAWordException e) {
            clearRow();
            throw new NotAWordException(word);
        }
    }

    private void clearRow() {
        for (int i = 0; i < Config.WORD_LENGTH; i++) {
            selectLetter('←');
        }
    }

    public List<Fact> determineLetterState() throws NotAWordException {
        Set<Fact.NotInWord> absentLetters = new HashSet<>();
        Set<Fact.Present> presentLetters = new HashSet<>();
        List<Fact.Correct> correctLetters = new ArrayList<>();

        // Find all elements with a "class=row" attribute within the <div id="board"> wrapper.
        List<ElementHandle> elementHandles = page.querySelectorAll("#board .row");
        Collections.reverse(elementHandles);
        for (ElementHandle elementHandle : elementHandles) {
            int position = 0;
            // For each of those rows, search all the elements with a "class=tile" attribute.
            for (ElementHandle handle : elementHandle.querySelectorAll(".tile")) {
                // Read the text content of that element.
                // From inspecting the HTML, we know it only contains a single letter.
                final String textContent = handle.textContent();
                if (textContent.length() == 0) {
                    break;
                }
                char letter = textContent.charAt(0);

                // Read the "data-state" attribute, which contains the result of that letter.
                // Then add that letter to the correct result bucket.
                switch (handle.getAttribute("data-state")) {
                    case "tbd" -> throw new NotAWordException("");
                    case "present" -> {
                        final Fact.Present f = presentLetters.stream()
                                .filter(fact -> fact.letter() == letter)
                                .findFirst()
                                .orElse(new Fact.Present(letter, new ArrayList<>()));
                        f.invalidPositions().add(position);
                        presentLetters.add(f);
                    }
                    case "absent" -> absentLetters.add(new Fact.NotInWord(letter));
                    case "correct" -> correctLetters.add(new Fact.Correct(letter, position));
                }
                position += 1;
            }
            List<Fact> result = new ArrayList<>();
            absentLetters.removeIf(absentFact -> Stream.concat(presentLetters.stream(), correctLetters.stream()).anyMatch(presentFact -> absentFact.letter() == presentFact.letter()));
            result.addAll(absentLetters);
            result.addAll(presentLetters);
            result.addAll(correctLetters);

            if (!result.isEmpty()) {
                return result;
            }
        }

        throw new IllegalStateException("Did not find any answers!");
    }

    private void selectLetter(char character){
        page.click("button[data-key=%s]".formatted(character));
    }

    private void enter() {
        // This uses a "Text Selector" to click the enter button.
        page.click("text=Enter");
    }

    @Override
    public void close() {
        if (page != null) {
            page.close();
        }
        playwright.close();
    }
}
