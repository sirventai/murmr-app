package uoc.edu.psirvent.murmr.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import uoc.edu.psirvent.murmr.db.MurmrDatabase
import java.io.File

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val home = System.getProperty("user.home") ?: "."
        val profile = System.getenv("MURMR_PROFILE")?.takeIf { it.isNotBlank() }
        val dir = if (profile != null) {
            File(home, ".murmr/$profile").apply { mkdirs() }
        } else {
            File(home, ".murmr").apply { mkdirs() }
        }
        val dbFile = File(dir, "murmr.db")
        val isNew = !dbFile.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        if (isNew) {
            MurmrDatabase.Schema.create(driver)
        }
        return driver
    }
}
