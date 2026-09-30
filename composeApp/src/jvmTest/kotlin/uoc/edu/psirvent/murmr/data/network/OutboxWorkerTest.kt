package uoc.edu.psirvent.murmr.data.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutboxWorkerTest {

    @Test
    fun backoffEmpiezaEn1Segundo() {
        assertEquals(1_000L, OutboxWorker.nextRetryDelayMs(0))
    }

    @Test
    fun backoffDuplicaEnCadaIntento() {
        assertEquals(2_000L, OutboxWorker.nextRetryDelayMs(1))
        assertEquals(4_000L, OutboxWorker.nextRetryDelayMs(2))
        assertEquals(8_000L, OutboxWorker.nextRetryDelayMs(3))
        assertEquals(16_000L, OutboxWorker.nextRetryDelayMs(4))
    }

    @Test
    fun backoffSeMantieneCrecienteHastaElIntento8() {
        assertEquals(256_000L, OutboxWorker.nextRetryDelayMs(8))
    }

    @Test
    fun backoffNoCreceMasAlladelIntento8() {

        assertEquals(256_000L, OutboxWorker.nextRetryDelayMs(9))
        assertEquals(256_000L, OutboxWorker.nextRetryDelayMs(15))
        assertEquals(256_000L, OutboxWorker.nextRetryDelayMs(100))
    }

    @Test
    fun backoffEsMonotonoCrecienteHastaElCap() {
        var anterior = 0L
        for (n in 0..8) {
            val actual = OutboxWorker.nextRetryDelayMs(n)
            assertTrue(actual > anterior, "Debería crecer: attempts=$n actual=$actual previo=$anterior")
            anterior = actual
        }
    }

    @Test
    fun maxAttemptsBeforeDropEs10() {

        assertEquals(10, OutboxWorker.MAX_ATTEMPTS_BEFORE_DROP)
    }
}
