package com.example

import com.example.data.ExcelParser
import com.example.data.WeldJoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcelParserTest {

    @Test
    fun testParseCsvSimple() {
        val csv = """
            Joint No,Line No,Spool No,Welder ID,Material,Diameter,Thickness,Status
            W-101,ISO-01,SP-01,S-10,A106 Gr.B,6.0,7.11,COMPLETED
            W-102,ISO-01,SP-02,S-11,SS 316L,4.0,5.54,IN_PROGRESS
        """.trimIndent()

        val welds = ExcelParser.parseCsvOrTsv(csv.toByteArray(Charsets.UTF_8))
        assertEquals(2, welds.size)
        assertEquals("W-101", welds[0].jointNo)
        assertEquals("ISO-01", welds[0].lineNo)
        assertEquals("S-10", welds[0].welderId)
        assertEquals(6.0, welds[0].diameterInch, 0.01)
        assertEquals("COMPLETED", welds[0].status)
    }

    @Test
    fun testParseCsvFrenchHeaders() {
        val csv = """
            N° Joint;Ligne;Tronçon;Soudeur;Matériau;Diamètre;Epaisseur;Statut;Résultat CND
            W-201;08-CS-150;TR-01;Marc Dupont;A106;8;8.18;Conforme;Accepté
        """.trimIndent()

        val welds = ExcelParser.parseCsvOrTsv(csv.toByteArray(Charsets.UTF_8))
        assertEquals(1, welds.size)
        assertEquals("W-201", welds[0].jointNo)
        assertEquals("08-CS-150", welds[0].lineNo)
        assertEquals(8.0, welds[0].diameterInch, 0.01)
        assertEquals("ACCEPTED", welds[0].ndtResult)
    }

    @Test
    fun testExportToCsv() {
        val welds = listOf(
            WeldJoint(
                jointNo = "W-001",
                lineNo = "ISO-TEST",
                welderId = "S-99",
                diameterInch = 4.0,
                thicknessMm = 6.02,
                status = "COMPLETED"
            )
        )

        val csvString = ExcelParser.exportToCsv(welds)
        assertTrue(csvString.contains("Joint No,Line No"))
        assertTrue(csvString.contains("W-001"))
        assertTrue(csvString.contains("ISO-TEST"))
    }
}
