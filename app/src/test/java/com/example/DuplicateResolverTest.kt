package com.example

import com.example.data.DuplicateResolver
import com.example.data.WeldJoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exemples du Contrôle Qualité (doublons ISO + Spool + Joint).
 * Les cas E2…E9 reprennent ceux de la revue de l'écran.
 */
class DuplicateResolverTest {

    private fun weld(
        id: Long, iso: String, spool: String, joint: String,
        welder: String = "", rt: String = "N/A", status: String = "COMPLETED",
        updatedAt: Long = 0L
    ) = WeldJoint(
        id = id, isoNumber = iso, spoolNumber = spool, jointNumber = joint,
        welder = welder, rt = rt, status = status, updatedAt = updatedAt
    )

    @Test
    fun `E1 base saine - aucun groupe`() {
        val groups = DuplicateResolver.group(
            listOf(weld(1, "ISO-100", "SP-01", "W1"), weld(2, "ISO-100", "SP-01", "W2"), weld(3, "ISO-100", "SP-02", "W1"))
        )
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `E2 paire exacte - une fiche supprimee`() {
        val groups = DuplicateResolver.group(
            listOf(weld(1, "ISO-100", "SP-01", "W1", welder = "S01"), weld(2, "ISO-100", "SP-01", "W1", welder = "S01"))
        )
        assertEquals(1, groups.size)
        assertEquals(1L, groups[0].keeperId)
        assertEquals(listOf(2L), DuplicateResolver.idsToDelete(groups))
    }

    @Test
    fun `E3 triplet et paire - trois fiches supprimees`() {
        val welds = (1L..3L).map { weld(it, "ISO-200", "SP-A", "J5") } +
            (4L..5L).map { weld(it, "ISO-201", "SP-B", "J9") } + weld(6, "ISO-202", "SP-C", "J1")
        val groups = DuplicateResolver.group(welds)
        assertEquals(2, groups.size)
        assertEquals(listOf(2L, 3L, 5L), DuplicateResolver.idsToDelete(groups).sorted())
    }

    @Test
    fun `E4 E5 casse et espaces ignores`() {
        val groups = DuplicateResolver.group(
            listOf(weld(1, "ISO-300", "SP-01", "W1"), weld(2, "iso-300", "sp-01", "w1"), weld(3, "ISO-300 ", "SP-01", " W1"))
        )
        assertEquals(1, groups.size)
        assertEquals(3, groups[0].count)
    }

    @Test
    fun `E6 la fiche avec RT accepte est conservee`() {
        val pending = weld(1, "ISO-500", "SP-01", "W7", welder = "S03", rt = "PENDING", status = "PENDING_NDT")
        val accepted = weld(2, "ISO-500", "SP-01", "W7", welder = "S03", rt = "ACCEPTED", status = "COMPLETED")
        val groups = DuplicateResolver.group(listOf(pending, accepted))
        assertEquals(2L, groups[0].keeperId)
        assertTrue(groups[0].conflicts.isEmpty())
        assertEquals(listOf(1L), DuplicateResolver.idsToDelete(groups))
    }

    @Test
    fun `resultats contradictoires - groupe en conflit, rien supprime`() {
        val groups = DuplicateResolver.group(
            listOf(
                weld(1, "ISO-510", "SP-01", "W1", rt = "ACCEPTED"),
                weld(2, "ISO-510", "SP-01", "W1", rt = "REJECTED", status = "REPAIR_REQUIRED")
            )
        )
        assertTrue("RT" in groups[0].conflicts)
        assertTrue(DuplicateResolver.idsToDelete(groups).isEmpty())
    }

    @Test
    fun `soudeurs differents - groupe en conflit`() {
        val groups = DuplicateResolver.group(
            listOf(weld(1, "ISO-520", "SP-01", "W1", welder = "S01"), weld(2, "ISO-520", "SP-01", "W1", welder = "S02"))
        )
        assertEquals(listOf("Soudeur"), groups[0].conflicts)
        assertTrue(DuplicateResolver.idsToDelete(groups).isEmpty())
    }

    @Test
    fun `fiche moins renseignee couverte par la fiche conservee`() {
        val groups = DuplicateResolver.group(
            listOf(weld(1, "ISO-530", "SP-01", "W1"), weld(2, "ISO-530", "SP-01", "W1", welder = "S04"))
        )
        assertEquals(2L, groups[0].keeperId)
        assertEquals(listOf(1L), DuplicateResolver.idsToDelete(groups))
    }

    @Test
    fun `E7 joints sans numero jamais consideres comme doublons`() {
        val groups = DuplicateResolver.group(
            listOf(weld(1, "ISO-600", "SP-01", ""), weld(2, "ISO-600", "SP-01", ""), weld(3, "ISO-600", "SP-01", "  "))
        )
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `E8b separateur dans un numero - pas de fusion`() {
        val groups = DuplicateResolver.group(
            listOf(weld(1, "A___B", "C", "J1"), weld(2, "A___B", "C", "J1"), weld(3, "A", "B___C", "J1"))
        )
        assertEquals(1, groups.size)
        assertEquals(listOf(1L, 2L), groups[0].welds.map { it.id })
    }

    @Test
    fun `egalite parfaite - la fiche au plus petit id est conservee`() {
        val groups = DuplicateResolver.group(
            listOf(weld(7, "ISO-1", "SP", "W1"), weld(3, "ISO-1", "SP", "W1"))
        )
        assertEquals(3L, groups[0].keeperId)
    }
}
