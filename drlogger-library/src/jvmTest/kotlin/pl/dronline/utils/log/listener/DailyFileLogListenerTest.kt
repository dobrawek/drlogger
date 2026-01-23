package pl.dronline.utils.log.listener

import kotlinx.coroutines.runBlocking
import pl.dronline.utils.datetime.toString
import pl.dronline.utils.filesystem.bytes
import pl.dronline.utils.log.ILogListener
import java.io.File
import java.nio.file.Files
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DailyFileLogListenerTest {

    private lateinit var tempDir: File
    private lateinit var listener: DailyFileLogListener

    @BeforeTest
    fun setup() {
        // Tworzymy tymczasowy katalog dla testów
        tempDir = Files.createTempDirectory("log-test-").toFile()
        println("\n=== Test Setup ===")
        println("Created temp directory: ${tempDir.absolutePath}")

        listener = DailyFileLogListener().apply {
            path = tempDir.absolutePath
            namePrefix = "test-"
            enabled = true
        }
    }

    @AfterTest
    fun tearDown() {
        // Czyścimy po testach
        println("\n=== Test Cleanup ===")
        println("Deleting temp directory: ${tempDir.absolutePath}")
        tempDir.deleteRecursively()
    }

    @Test
    fun `should create log file when writing`() {
        // Given
        val testMessage = "Test log message"
        println("\n=== Test: Create log file ===")

        // When
        println("Writing log message: '$testMessage'")
        listener.writeLog(
            timestamp = Clock.System.now(),
            level = ILogListener.Level.INFO,
            type = "TEST",
            message = testMessage,
            t = null
        )

        // Then
        val logFiles = tempDir.listFiles { file ->
            file.name.startsWith("test-") && file.name.endsWith(".log")
        }

        println("Found log files: ${logFiles?.size ?: 0}")
        logFiles?.forEach { file ->
            println("  - ${file.name} (${file.length()} bytes)")
            println("    Content preview: ${file.readText().take(100).trim()}")
        }

        assertNotNull(logFiles)
        assertEquals(1, logFiles.size)
        assertTrue(logFiles[0].readText().contains(testMessage))
    }

    @Test
    fun `should delete files older than maxFileAgeDays`() {
        // Given
        listener.maxFileAgeDays = 7 // Pliki starsze niż 7 dni

        // Tworzymy stare pliki
        val oldFile1 = createLogFile("test-20240101.log", 30) // 30 dni temu
        val oldFile2 = createLogFile("test-20240115.log", 15) // 15 dni temu
        val recentFile = createLogFile("test-20240201.log", 3) // 3 dni temu

        println("Created test files:")
        println("  - ${oldFile1.name} (30 days old) exists: ${oldFile1.exists()}")
        println("  - ${oldFile2.name} (15 days old) exists: ${oldFile2.exists()}")
        println("  - ${recentFile.name} (3 days old) exists: ${recentFile.exists()}")

        // When
        println("\nRunning cleanup with maxFileAgeDays=${listener.maxFileAgeDays}...")
        listener.performCleanup()

        // Then
        println("\nAfter cleanup:")
        println("  - ${oldFile1.name} exists: ${oldFile1.exists()}")
        println("  - ${oldFile2.name} exists: ${oldFile2.exists()}")
        println("  - ${recentFile.name} exists: ${recentFile.exists()}")

        assertFalse(oldFile1.exists(), "Old file 1 should be deleted")
        assertFalse(oldFile2.exists(), "Old file 2 should be deleted")
        assertTrue(recentFile.exists(), "Recent file should still exist")
    }

    @Test
    fun `should keep only maxFileCount files`() {
        // Given
        listener.maxFileCount = 3
        listener.maxFileAgeDays = 999 // Nie usuwamy ze względu na wiek

        println("Test: Keep only ${listener.maxFileCount} files")

        // Tworzymy 5 plików
        val files = (1..5).map { i ->
            createLogFile("test-2024010$i.log", 5 - i)
        }

        println("\nCreated ${files.size} test files:")
        files.forEachIndexed { index, file ->
            println("  - ${file.name} (${5 - index} days old)")
        }

        // When
        println("\nRunning cleanup...")
        listener.performCleanup()

        // Then
        val remainingFiles = tempDir.listFiles { file ->
            file.name.startsWith("test-") && file.name.endsWith(".log")
        }

        println("\nAfter cleanup - remaining files: ${remainingFiles?.size ?: 0}")
        remainingFiles?.forEach { file ->
            println("  - ${file.name}")
        }

        assertEquals(3, remainingFiles?.size ?: 0)

        // Sprawdzamy że zostały 3 najnowsze pliki
        assertTrue(files[2].exists(), "File 3 should exist")
        assertTrue(files[3].exists(), "File 4 should exist")
        assertTrue(files[4].exists(), "File 5 should exist")

        // A 2 najstarsze zostały usunięte
        assertFalse(files[0].exists(), "File 1 should be deleted")
        assertFalse(files[1].exists(), "File 2 should be deleted")
    }

    @Test
    fun `should handle empty directory`() {
        // Given - pusty katalog

        // When & Then - nie powinno rzucić wyjątku
        assertDoesNotThrow {
            listener.performCleanup()
        }
    }

    @Test
    fun `should handle non-existent directory`() {
        // Given
        listener.path = "/non/existent/path"

        // When & Then - nie powinno rzucić wyjątku
        assertDoesNotThrow {
            listener.performCleanup()
        }
    }

    @Test
    fun `should ignore non-log files`() {
        // Given
        val logFile = createLogFile("test-20240101.log", 10)
        val otherFile = File(tempDir, "other-file.txt").apply {
            writeText("Not a log file")
        }
        val wrongPrefixFile = File(tempDir, "wrong-20240101.log").apply {
            writeText("Wrong prefix")
        }

        listener.maxFileAgeDays = 5 // Wszystkie pliki są "stare"

        // When
        listener.performCleanup()

        // Then
        assertFalse(logFile.exists(), "Log file should be deleted")
        assertTrue(otherFile.exists(), "Non-log file should not be deleted")
        assertTrue(wrongPrefixFile.exists(), "File with wrong prefix should not be deleted")
    }

    @Test
    fun `should roll file when maxFileSize exceeded`() {
        // Given
        listener.maxFileSize = 100.bytes // 100 bytes limit

        println("\n=== Test: Roll file when size exceeded ===")

        // Write enough data to exceed the limit
        repeat(10) { i ->
            listener.writeLog(
                timestamp = Clock.System.now(),
                level = ILogListener.Level.INFO,
                type = "TEST",
                message = "Message number $i with some padding to make it longer",
                t = null
            )
        }

        // Then
        val logFiles = tempDir.listFiles { file ->
            file.name.startsWith("test-") && file.name.endsWith(".log")
        }?.sortedBy { it.name }

        println("Found log files: ${logFiles?.size ?: 0}")
        logFiles?.forEach { file ->
            println("  - ${file.name} (${file.length()} bytes)")
        }

        assertNotNull(logFiles)
        assertTrue(logFiles.size >= 2, "Should have created at least 2 files due to size rolling")
    }

    @Test
    fun `should create indexed files in sequence`() {
        // Given
        listener.maxFileSize = 50.bytes // Very small limit to force multiple rolls

        println("\n=== Test: Create indexed files in sequence ===")

        // Write enough data to create multiple files
        repeat(20) { i ->
            listener.writeLog(
                timestamp = Clock.System.now(),
                level = ILogListener.Level.INFO,
                type = "TEST",
                message = "Message $i",
                t = null
            )
        }

        // Then
        val logFiles = tempDir.listFiles { file ->
            file.name.startsWith("test-") && file.name.endsWith(".log")
        }?.sortedBy { it.name }

        println("Found log files: ${logFiles?.size ?: 0}")
        logFiles?.forEach { file ->
            println("  - ${file.name} (${file.length()} bytes)")
        }

        assertNotNull(logFiles)
        assertTrue(logFiles.size >= 2, "Should have multiple files")

        // Verify naming convention
        val today = Clock.System.now().toString("yyyyMMdd")
        assertTrue(logFiles.any { it.name == "test-$today.log" }, "Main file should exist")
        assertTrue(logFiles.any { it.name == "test-$today.1.log" }, "First indexed file should exist")
    }

    @Test
    fun `should cleanup indexed files based on age`() {
        // Given
        listener.maxFileAgeDays = 5
        listener.maxFileSize = 100.bytes // Enable size rolling

        // Create old indexed files
        val oldMainFile = createLogFile("test-20240101.log", 10)
        val oldIndexedFile1 = createLogFile("test-20240101.1.log", 10)
        val oldIndexedFile2 = createLogFile("test-20240101.2.log", 10)
        val recentFile = createLogFile("test-20240201.log", 2)

        println("\n=== Test: Cleanup indexed files based on age ===")
        println("Created files:")
        println("  - ${oldMainFile.name} (10 days old)")
        println("  - ${oldIndexedFile1.name} (10 days old)")
        println("  - ${oldIndexedFile2.name} (10 days old)")
        println("  - ${recentFile.name} (2 days old)")

        // When
        listener.performCleanup()

        // Then
        println("\nAfter cleanup:")
        println("  - ${oldMainFile.name} exists: ${oldMainFile.exists()}")
        println("  - ${oldIndexedFile1.name} exists: ${oldIndexedFile1.exists()}")
        println("  - ${oldIndexedFile2.name} exists: ${oldIndexedFile2.exists()}")
        println("  - ${recentFile.name} exists: ${recentFile.exists()}")

        assertFalse(oldMainFile.exists(), "Old main file should be deleted")
        assertFalse(oldIndexedFile1.exists(), "Old indexed file 1 should be deleted")
        assertFalse(oldIndexedFile2.exists(), "Old indexed file 2 should be deleted")
        assertTrue(recentFile.exists(), "Recent file should still exist")
    }

    @Test
    fun `should cleanup indexed files based on count`() {
        // Given
        listener.maxFileCount = 3
        listener.maxFileAgeDays = 999 // Don't delete based on age
        listener.maxFileSize = 100.bytes // Enable size rolling

        println("\n=== Test: Cleanup indexed files based on count ===")

        // Create files (including indexed ones) - 5 total
        val files = listOf(
            createLogFile("test-20240101.log", 5),
            createLogFile("test-20240101.1.log", 5),
            createLogFile("test-20240102.log", 4),
            createLogFile("test-20240103.log", 3),
            createLogFile("test-20240103.1.log", 3)
        )

        println("Created ${files.size} files:")
        files.forEach { println("  - ${it.name}") }

        // When
        listener.performCleanup()

        // Then
        val remainingFiles = tempDir.listFiles { file ->
            file.name.startsWith("test-") && file.name.endsWith(".log")
        }

        println("\nAfter cleanup - remaining files: ${remainingFiles?.size ?: 0}")
        remainingFiles?.forEach { println("  - ${it.name}") }

        assertEquals(3, remainingFiles?.size ?: 0, "Should keep only 3 files")
    }

    @Test
    fun `should not roll when maxFileSize is null`() {
        // Given
        listener.maxFileSize = null // Disabled (default)

        println("\n=== Test: No rolling when maxFileSize is null ===")

        // Write many messages
        repeat(50) { i ->
            listener.writeLog(
                timestamp = Clock.System.now(),
                level = ILogListener.Level.INFO,
                type = "TEST",
                message = "Message number $i with some extra padding to make it longer",
                t = null
            )
        }

        // Then
        val logFiles = tempDir.listFiles { file ->
            file.name.startsWith("test-") && file.name.endsWith(".log")
        }

        println("Found log files: ${logFiles?.size ?: 0}")
        logFiles?.forEach { file ->
            println("  - ${file.name} (${file.length()} bytes)")
        }

        assertNotNull(logFiles)
        assertEquals(1, logFiles.size, "Should have only one file when size rolling is disabled")
    }

    @Test
    fun `cleanup should be called on start`() = runBlocking {
        // Given
        listener.maxFileAgeDays = 1
        val oldFile = createLogFile("test-20240101.log", 10)

        // When
        listener.startListening(this)

        // Then - plik powinien zostać usunięty bo onStart wywołuje performCleanup
        assertFalse(oldFile.exists(), "Old file should be deleted on start")

        // Cleanup
        listener.stopListening()
    }

    // Helper functions

    private fun createLogFile(name: String, daysAgo: Int): File {
        val file = File(tempDir, name)
        file.writeText("Test log content\n")

        // Ustawiamy datę modyfikacji pliku
        val modifiedTime = Clock.System.now() - daysAgo.days
        file.setLastModified(modifiedTime.toEpochMilliseconds())

        return file
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}