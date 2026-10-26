package com.farmconnect.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BackupSupportTest {
    @Test void postgresUrlsAreUnderstood() {
        BackupSupport.PgTarget t = BackupSupport.parsePgUrl("jdbc:postgresql://db.example.com:6543/farmconnect?sslmode=require");
        assertEquals("db.example.com", t.host());
        assertEquals(6543, t.port());
        assertEquals("farmconnect", t.database());
        assertEquals(5432, BackupSupport.parsePgUrl("jdbc:postgresql://localhost/farmconnect").port());
        assertNull(BackupSupport.parsePgUrl("jdbc:h2:file:./data/farmconnect"));
        assertNull(BackupSupport.parsePgUrl(null));
        assertTrue(BackupSupport.isH2("jdbc:h2:file:./data/farmconnect;AUTO_SERVER=TRUE"));
        assertFalse(BackupSupport.isH2("jdbc:postgresql://localhost/x"));
    }

    @Test void fileNamesSortByTime() {
        assertEquals("farmconnect-db-20261025-0230.dump", BackupSupport.fileName("farmconnect-db", LocalDateTime.of(2026, 10, 25, 2, 30), "dump"));
    }

    @Test void onlyTheNewestCopiesOfEachKindAreKept() {
        List<String> names = List.of("farmconnect-db-20261001-0230.dump", "farmconnect-db-20261002-0230.dump", "farmconnect-db-20261003-0230.dump",
                "farmconnect-uploads-20261001-0230.zip", "farmconnect-uploads-20261002-0230.zip", "notes.txt", "other-20260101-0000.bin");
        List<String> gone = BackupSupport.expired(names, 2);
        assertTrue(gone.contains("farmconnect-db-20261001-0230.dump"));
        assertTrue(gone.contains("other-20260101-0000.bin") == false);
        assertFalse(gone.contains("farmconnect-db-20261003-0230.dump"));
        assertFalse(gone.contains("farmconnect-uploads-20261001-0230.zip"));
        assertFalse(gone.contains("notes.txt"));
        assertEquals(1, gone.size());
    }

    @Test void keepIsNeverLessThanOne() {
        List<String> names = List.of("farmconnect-db-20261001-0230.dump", "farmconnect-db-20261002-0230.dump");
        assertEquals(List.of("farmconnect-db-20261001-0230.dump"), BackupSupport.expired(names, 0));
        assertTrue(BackupSupport.expired(List.of(), 14).isEmpty());
    }

    @Test void lowDiskMeansUnderOneGigabyteOrTenPercent() {
        long gb = 1024L * 1024 * 1024;
        assertTrue(BackupSupport.diskLow(gb / 2, 100 * gb));
        assertTrue(BackupSupport.diskLow(9 * gb, 100 * gb));
        assertFalse(BackupSupport.diskLow(50 * gb, 100 * gb));
        assertFalse(BackupSupport.diskLow(0, 0));
    }

    @Test void loginSpikeNeedsThreshold() {
        assertTrue(BackupSupport.loginSpike(200, 200));
        assertFalse(BackupSupport.loginSpike(199, 200));
        assertFalse(BackupSupport.loginSpike(5000, 0));
    }

    @Test void sameAlertIsNotRepeatedInsideTheWindow() {
        Map<String, Long> sent = new HashMap<>();
        assertTrue(BackupSupport.shouldSend(sent, "disk", 1_000, 10_000));
        assertFalse(BackupSupport.shouldSend(sent, "disk", 5_000, 10_000));
        assertTrue(BackupSupport.shouldSend(sent, "other", 5_000, 10_000));
        assertTrue(BackupSupport.shouldSend(sent, "disk", 11_001, 10_000));
    }

    @Test void sizesAreReadable() {
        assertEquals("512 B", BackupSupport.humanSize(512));
        assertEquals("1.5 KB", BackupSupport.humanSize(1536));
        assertEquals("2.0 MB", BackupSupport.humanSize(2 * 1024 * 1024));
    }
}
