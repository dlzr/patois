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
-- DATABASE_VERSION fielf in ro.undef.patois.PatoisDatabase, and make sure you
-- handle upgrading from previous versions of the database schema.

CREATE TABLE languages (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    num_words INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE words (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    language_id INTEGER NOT NULL
);

CREATE TABLE translations (
    word_id1 INTEGER NOT NULL,
    word_id2 INTEGER NOT NULL,
    UNIQUE(word_id1, word_id2) ON CONFLICT IGNORE
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
