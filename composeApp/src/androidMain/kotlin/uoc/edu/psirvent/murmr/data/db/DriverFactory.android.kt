package uoc.edu.psirvent.murmr.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import uoc.edu.psirvent.murmr.db.MurmrDatabase

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver = AndroidSqliteDriver(
        schema = MurmrDatabase.Schema,
        context = context,
        name = DB_NAME,
    )

    private companion object {
        const val DB_NAME = "murmr.db"
    }
}
