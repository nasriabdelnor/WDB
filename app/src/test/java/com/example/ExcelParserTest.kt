package com.example

import com.example.data.ExcelParser
import com.example.data.WeldJoint
import org.junit.Assert.assertEquals
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
    fun testMergedCellsFillDown() {
        // Simulates Excel vertical merged cells where Line and Spool only appear on the first row of each group
        val csv = """
            N° ISO;Rep. Spool;N° Joint;Soudeur;Diamètre;Epaisseur
            P-920-003-3;SP01;W-01;S-10;4.0;6.02
            ;;W-02;S-10;4.0;6.02
            ;;W-03;S-11;4.0;6.02
            ;SP02;W-04;S-12;4.0;6.02
            ;;W-05;S-12;4.0;6.02
            P-920-003-4;SP01;W-01;S-15;6.0;7.11
            ;;W-02;S-15;6.0;7.11
        """.trimIndent()

        val welds = ExcelParser.parseCsvOrTsv(csv.toByteArray(Charsets.UTF_8))
        assertEquals(7, welds.size)

        // First isometric line
        assertEquals("P-920-003-3", welds[0].lineNo)
        assertEquals("SP01", welds[0].spoolNo)
        assertEquals("W-01", welds[0].jointNo)

        // Second weld inherits line P-920-003-3 and spool SP01
        assertEquals("P-920-003-3", welds[1].lineNo)
        assertEquals("SP01", welds[1].spoolNo)
        assertEquals("W-02", welds[1].jointNo)

        // Third weld inherits line P-920-003-3 and spool SP01
        assertEquals("P-920-003-3", welds[2].lineNo)
        assertEquals("SP01", welds[2].spoolNo)
        assertEquals("W-03", welds[2].jointNo)

        // Fourth weld gets new spool SP02 on same line P-920-003-3
        assertEquals("P-920-003-3", welds[3].lineNo)
        assertEquals("SP02", welds[3].spoolNo)
        assertEquals("W-04", welds[3].jointNo)

        // Fifth weld inherits SP02 on line P-920-003-3
        assertEquals("P-920-003-3", welds[4].lineNo)
        assertEquals("SP02", welds[4].spoolNo)
        assertEquals("W-05", welds[4].jointNo)

        // Sixth weld starts new line P-920-003-4 with spool SP01
        assertEquals("P-920-003-4", welds[5].lineNo)
        assertEquals("SP01", welds[5].spoolNo)
        assertEquals("W-01", welds[5].jointNo)

        // Seventh weld inherits new line P-920-003-4
        assertEquals("P-920-003-4", welds[6].lineNo)
        assertEquals("SP01", welds[6].spoolNo)
        assertEquals("W-02", welds[6].jointNo)
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

    @Test
    fun testParsePartsAndHeatNumbers() {
        val csv = """
            N° ISO;Rep. Spool;N° Joint;Partie 1;N° Coulée 1;Partie 2;N° Coulée 2;Soudeur;Diamètre
            ISO-A;SP-01;W-01;Tube 4" Sch 40;H-88231;Coude 90° 4";H-99412;W-10;4.0
            ;;W-02;Tube 4";H-88231;Bride WN 4";H-77103;W-10;4.0
        """.trimIndent()

        val welds = ExcelParser.parseCsvOrTsv(csv.toByteArray(Charsets.UTF_8))
        assertEquals(2, welds.size)
        assertEquals("Tube 4\" Sch 40", welds[0].part1)
        assertEquals("H-88231", welds[0].heatNo1)
        assertEquals("Coude 90° 4\"", welds[0].part2)
        assertEquals("H-99412", welds[0].heatNo2)

        assertEquals("Tube 4\"", welds[1].part1)
        assertEquals("H-88231", welds[1].heatNo1)
        assertEquals("Bride WN 4\"", welds[1].part2)
        assertEquals("H-77103", welds[1].heatNo2)
    }
}
