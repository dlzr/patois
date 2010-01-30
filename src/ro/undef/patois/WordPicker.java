package ro.undef.patois;

public class WordPicker {

    public static Word pickWord(PatoisDatabase db) {
        // TODO: Implement me.
        // NOTE: Don't pick untranslated words.
        return new Word(3, "m\u0103m\u0103lig\u0103", new Language(1, "ro", "Romanian", 1), 10);
    }
}
