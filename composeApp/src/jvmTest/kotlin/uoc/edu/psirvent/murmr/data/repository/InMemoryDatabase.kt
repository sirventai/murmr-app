package uoc.edu.psirvent.murmr.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import uoc.edu.psirvent.murmr.db.MurmrDatabase

internal fun newInMemoryDatabase(): MurmrDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    MurmrDatabase.Schema.create(driver)
    return MurmrDatabase(driver)
}
