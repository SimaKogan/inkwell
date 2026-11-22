package com.inkwell.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Search relies on PostgreSQL lower-casing Cyrillic text inside full-text and trigram indexes.
 * That only works when the database was created with a UTF-8 locale; with the "C" locale the index
 * silently keeps "Мастер" with a capital letter, so a query for "мастер" finds nothing.
 * This check tests the actual behaviour (not the locale name) and says so loudly at startup.
 */
@Component
public class DatabaseLocaleCheck implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DatabaseLocaleCheck.class);

    private final JdbcTemplate jdbc;

    public DatabaseLocaleCheck(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String probe = jdbc.queryForObject("SELECT to_tsvector('simple', 'Ж')::text", String.class);
            if (probe != null && probe.contains("\u0416")) {
                log.warn("The database does not lower-case Cyrillic text (probably created with the \"C\" locale). "
                        + "Search for Russian words will miss results. Recreate the database with "
                        + "TEMPLATE template0 ENCODING 'UTF8' LC_COLLATE 'en_US.UTF-8' LC_CTYPE 'en_US.UTF-8'. "
                        + "See README, section \"Run locally\".");
            }
        } catch (RuntimeException e) {
            log.debug("Locale check skipped: {}", e.getMessage());
        }
    }
}
