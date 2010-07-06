-- This file contains the database schema for Patois.
--
-- The Java class android.database.SQLiteDatabase only only allows the
-- execution of one SQL statement at a time.  Since we can't split a file into
-- individual SQL statements (short of implementing a full SQL parser), the
-- following conventions must be followed:
--
--   - comments start with '--' and last until the end-of-line.  Lines that
--     start with '--' are never passed to the SQLite3 engine.
--
--   - SQL statements must be separated by an empty line.  If you add empty
--     lines in the middle of an SQL statement, the parser will try to execute
--     the two parts as separate statements, and it's unlikely to do what you
--     want.  Conversely, if you don't separate the statements by empty lines,
--     two or more statements will be passed to the SQLite3 engine at the same
--     time, and that won't work.
--
--   - The SQL statements must contain valid SQLite3 syntax.  See
--     http://www.sqlite.org/lang.html for details.
--
-- This should ensure that both the Patois code and the command-line tool
-- 'sqlite3' will be able to process this file as-is.
--
-- If you modify the database schema, make sure to increment the
-- DATABASE_VERSION field in ro.undef.patois.Database, and make sure you handle
-- upgrading from previous versions of the database schema.

CREATE TABLE languages (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    -- Short name of the language (e.g., 'en', 'ro').
    code TEXT NOT NULL,
    -- Full name of the language (e.g., 'English', 'Romanian').
    name TEXT NOT NULL,
    -- The number of words in this language.
    num_words INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE words (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    -- The actual word.
    name TEXT NOT NULL,
    -- The ID of the language this word is in.
    language_id INTEGER NOT NULL,
    -- The number of translations for this word.
    num_translations INTEGER NOT NULL DEFAULT 0,
    -- The UNIX timestamp in UTC when the word was first added to the database.
    timestamp INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    -- How well the word is known (the higher, the better) when translating
    -- from this word to its synonyms (the "from" direction).  This dictates
    -- how to increment the next_practice timestamp.
    level_from INTEGER NOT NULL,
    -- When was the last time this word was up for practice in the "from"
    -- direction.
    last_practice_from INTEGER NOT NULL DEFAULT 0,
    -- When is the earliest time to show this word for practice, in the "from"
    -- direction.
    next_practice_from INTEGER NOT NULL,
    -- Same as level_from, but when translating from synonyms to this word.
    level_to INTEGER NOT NULL,
    -- Same as last_practice_from, but in the "to" direction.
    last_practice_to INTEGER NOT NULL DEFAULT 0,
    -- Same as next_practice_from, but in the "to" direction.
    next_practice_to INTEGER NOT NULL
);

CREATE TABLE translations (
    word_id1 INTEGER NOT NULL,
    word_id2 INTEGER NOT NULL,
    UNIQUE(word_id1, word_id2)
);

CREATE TABLE practice_log (
    -- The version of the trainer algorithm that saved this trial.
    trainer INTEGER NOT NULL,
    -- The ID of the word being tested.
    word_id INTEGER NOT NULL,
    -- Direction: 0 if the test was "from" this word, 1 if it was "to" this word.
    direction INTEGER NOT NULL,
    -- Successful: 1 if the user "knew it", 0 otherwise.
    successful INTEGER NOT NULL,
    -- The UNIX timestamp in UTC of the trial.
    timestamp INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);

CREATE TRIGGER delete_words_when_deleting_language DELETE ON languages
    BEGIN
        DELETE FROM words
            WHERE language_id = OLD._id;
    END;

CREATE TRIGGER delete_translations_when_deleting_word DELETE ON words
    BEGIN
        DELETE FROM translations
            WHERE word_id1 = OLD._id OR word_id2 = OLD._id;
    END;

CREATE TRIGGER count_words_on_insert INSERT ON words
    BEGIN
        UPDATE languages
            SET num_words = num_words + 1
            WHERE _id = NEW.language_id;
    END;

CREATE TRIGGER count_words_on_update UPDATE ON words
    WHEN OLD.language_id != NEW.language_id
    BEGIN
        UPDATE languages
            SET num_words = num_words - 1
            WHERE _id = OLD.language_id;
        UPDATE languages
            SET num_words = num_words + 1
            WHERE _id = NEW.language_id;
    END;

CREATE TRIGGER count_words_on_delete DELETE ON words
    BEGIN
        UPDATE languages
            SET num_words = num_words - 1
            WHERE _id = OLD.language_id;
    END;

CREATE TRIGGER count_translations_on_insert INSERT ON translations
    BEGIN
        UPDATE words
            SET num_translations = num_translations + 1
            WHERE _id = NEW.word_id1;
    END;

CREATE TRIGGER count_translations_on_update UPDATE ON translations
    WHEN OLD.word_id1 != NEW.word_id1
    BEGIN
        UPDATE words
            SET num_translations = num_translations - 1
            WHERE _id = OLD.word_id1;
        UPDATE words
            SET num_translations = num_translations + 1
            WHERE _id = NEW.word_id1;
    END;

CREATE TRIGGER count_translations_on_delete DELETE ON translations
    BEGIN
        UPDATE words
            SET num_translations = num_translations - 1
            WHERE _id = OLD.word_id1;
    END;

CREATE TRIGGER delete_practice_info_when_deleting_word DELETE ON words
    BEGIN
        DELETE FROM practice_log
            WHERE word_id = OLD._id;
    END;
