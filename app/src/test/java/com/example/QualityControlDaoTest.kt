package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.DuplicateResolver
import com.example.data.WeldDatabase
import com.example.data.WeldJoint
import com.example.data.WeldJointDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Requêtes Room du Contrôle Qualité, exécutées sur une vraie base SQLite en mémoire. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QualityControlDaoTest {

    private lateinit var db: WeldDatabase
    private lateinit var dao: WeldJointDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WeldDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.weldJointDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun insert(iso: String, spool: String, joint: String, rt: String = "N/A"): Long =
        dao.insertWeld(WeldJoint(isoNumber = iso, spoolNumber = spool, jointNumber = joint, rt = rt))

    @Test
    fun `compteur = fiches en trop, pas fiches impliquees`() = runTest {
        repeat(3) { insert("ISO-200", "SP-A", "J5") }
        repeat(2) { insert("ISO-201", "SP-B", "J9") }
        insert("ISO-202", "SP-C", "J1")
        assertEquals(3, dao.getDuplicateCount().first())
        assertEquals(5, dao.getDuplicateWeldsSnapshot().size)
    }

    @Test
    fun `casse et espaces ignores par la requete`() = runTest {
        insert("ISO-300", "SP-01", "W1")
        insert("iso-300", "sp-01", "w1")
        insert("ISO-300 ", "SP-01", " W1")
        assertEquals(2, dao.getDuplicateCount().first())
        assertEquals(3, dao.getDuplicateWelds().first().size)
    }

    @Test
    fun `numeros vides exclus`() = runTest {
        repeat(3) { insert("ISO-600", "SP-01", "") }
        assertEquals(0, dao.getDuplicateCount().first())
        assertTrue(dao.getDuplicateWeldsSnapshot().isEmpty())
    }

    @Test
    fun `separateur dans un numero - pas de faux positif`() = runTest {
        insert("A___B", "C", "J1")
        insert("A___B", "C", "J1")
        insert("A", "B___C", "J1")
        assertEquals(1, dao.getDuplicateCount().first())
        assertEquals(2, dao.getDuplicateWeldsSnapshot().size)
    }

    @Test
    fun `nettoyage complet - garde la fiche RT acceptee`() = runTest {
        val pendingId = insert("ISO-500", "SP-01", "W7", rt = "PENDING")
        val acceptedId = insert("ISO-500", "SP-01", "W7", rt = "ACCEPTED")
        val groups = DuplicateResolver.group(dao.getDuplicateWeldsSnapshot())
        val deleted = dao.deleteWeldsByIds(DuplicateResolver.idsToDelete(groups))
        assertEquals(1, deleted)
        assertEquals(null, dao.getWeldByIdOnce(pendingId))
        assertEquals("ACCEPTED", dao.getWeldByIdOnce(acceptedId)?.rt)
        assertEquals(0, dao.getDuplicateCount().first())
    }

    @Test
    fun `suppression par lots au-dela de la limite SQLite`() = runTest {
        val ids = (1..1200).map { insert("ISO-900", "SP-01", "W$it") }
        assertEquals(1200, dao.deleteWeldsByIds(ids))
        assertEquals(0, dao.getCount())
    }

    @Test
    fun `findDuplicates normalise aussi`() = runTest {
        val id = insert("ISO-1", "SP-1", "W1")
        insert("iso-1", "sp-1", "w1 ")
        assertEquals(1, dao.findDuplicates("ISO-1", "SP-1", "W1", excludeId = id).size)
    }
}
