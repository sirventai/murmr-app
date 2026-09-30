package uoc.edu.psirvent.murmr.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import uoc.edu.psirvent.murmr.db.MurmrDatabase

actual class DriverFactory {
    actual fun createDriver(): SqlDriver = NativeSqliteDriver(
        schema = MurmrDatabase.Schema,
        name = "murmr.db",
    )
}
