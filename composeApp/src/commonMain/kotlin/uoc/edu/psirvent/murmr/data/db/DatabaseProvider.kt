package uoc.edu.psirvent.murmr.data.db

import uoc.edu.psirvent.murmr.db.MurmrDatabase

object DatabaseProvider {
    fun create(factory: DriverFactory): MurmrDatabase {
        val driver = factory.createDriver()
        return MurmrDatabase(driver)
    }
}
