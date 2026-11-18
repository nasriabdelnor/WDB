package com.example.data

/**
 * Règles de détection et de nettoyage des doublons (écran Contrôle Qualité).
 *
 * Un joint est identifié par ISO + Spool + N° de joint, comparés sans tenir compte
 * de la casse ni des espaces de bord. Les joints sans numéro ne sont jamais
 * considérés comme des doublons.
 *
 * Dans un groupe, la fiche conservée est la plus avancée (CND, statut, nombre de
 * champs renseignés, date de mise à jour). Une autre fiche n'est supprimée que si
 * elle est « couverte » par la fiche conservée : chacun de ses champs est vide,
 * identique, ou à un stade CND / statut antérieur. Sinon le groupe est en conflit
 * et doit être arbitré manuellement — aucun résultat de contrôle n'est supprimé
 * silencieusement.
 */
object DuplicateResolver {

    fun normalize(value: String): String = value.trim().uppercase()

    fun keyOf(w: WeldJoint): String =
        "${normalize(w.isoNumber)}|${normalize(w.spoolNumber)}|${normalize(w.jointNumber)}"

    /** Regroupe les fiches en double (groupes de 2 fiches ou plus). */
    fun group(welds: List<WeldJoint>): List<DuplicateWeldGroup> =
        welds.asSequence()
            .filter { it.jointNumber.isNotBlank() }
            .groupBy { keyOf(it) }
            .filterValues { it.size > 1 }
            .map { (key, list) -> buildGroup(key, list.sortedBy { it.id }) }
            .sortedBy { it.key }

    /** Ids des fiches que le nettoyage automatique peut supprimer sans perte d'information. */
    fun idsToDelete(groups: List<DuplicateWeldGroup>): List<Long> =
        groups.filter { it.conflicts.isEmpty() }
            .flatMap { g -> g.welds.filter { it.id != g.keeperId }.map { it.id } }

    private fun buildGroup(key: String, welds: List<WeldJoint>): DuplicateWeldGroup {
        val keeper = pickKeeper(welds)
        val conflicts = welds.filter { it.id != keeper.id }
            .flatMap { conflictingFields(keeper, it) }
            .distinct()
        return DuplicateWeldGroup(
            key = key,
            lineNo = keeper.isoNumber,
            spoolNo = keeper.spoolNumber,
            jointNo = keeper.jointNumber,
            count = welds.size,
            welds = welds,
            keeperId = keeper.id,
            conflicts = conflicts
        )
    }

    fun pickKeeper(welds: List<WeldJoint>): WeldJoint =
        welds.sortedWith(
            compareByDescending<WeldJoint> { progressScore(it) }
                .thenByDescending { filledFieldCount(it) }
                .thenByDescending { it.updatedAt }
                .thenBy { it.id }
        ).first()

    // --- Stades CND / statut -------------------------------------------------

    private enum class Ndt { NONE, PENDING, ACCEPTED, REJECTED }

    private fun ndt(value: String): Ndt {
        val v = normalize(value)
        return when {
            v.isEmpty() || v == "N/A" || v == "NA" || v == "-" -> Ndt.NONE
            v.contains("REJ") || v.contains("NON CONF") || v == "NC" -> Ndt.REJECTED
            v.contains("ACC") || v.contains("CONF") || v == "OK" -> Ndt.ACCEPTED
            v.contains("PEND") || v.contains("ATTENTE") -> Ndt.PENDING
            else -> Ndt.PENDING
        }
    }

    private fun ndtLevel(n: Ndt): Int = when (n) {
        Ndt.NONE -> 0
        Ndt.PENDING -> 1
        Ndt.ACCEPTED, Ndt.REJECTED -> 2
    }

    private fun statusLevel(status: String): Int = when (normalize(status)) {
        "", "N/A" -> 0
        "IN_PROGRESS" -> 1
        "PENDING_NDT" -> 2
        "COMPLETED", "REPAIR_REQUIRED" -> 3
        else -> 1
    }

    private fun progressScore(w: WeldJoint): Int =
        listOf(w.visual, w.rt, w.pt, w.ut).sumOf { ndtLevel(ndt(it)) } + statusLevel(w.status)

    // --- Champs descriptifs ---------------------------------------------------

    private val textFields: List<Pair<String, (WeldJoint) -> String>> = listOf(
        "Soudeur" to { it.welder },
        "Nom soudeur" to { it.welderName },
        "Date" to { it.date },
        "Coulée 1" to { it.part1HeatNumber },
        "Coulée 2" to { it.part2HeatNumber },
        "Composant 1" to { it.part1Description },
        "Composant 2" to { it.part2Description },
        "Matière 1" to { it.part1Material },
        "Matière 2" to { it.part2Material },
        "WPS" to { it.wps },
        "Procédé" to { it.process },
        "Type" to { it.weldType },
        "Plan" to { it.drawingNo },
        "Rapport CND" to { it.ndtReportNo },
        "Notes" to { it.notes }
    )

    private val ndtFields: List<Pair<String, (WeldJoint) -> String>> = listOf(
        "VT" to { it.visual },
        "RT" to { it.rt },
        "PT" to { it.pt },
        "UT" to { it.ut }
    )

    private fun filledFieldCount(w: WeldJoint): Int =
        textFields.count { (_, get) -> get(w).isNotBlank() }

    /**
     * Champs pour lesquels [other] porte une information que [keeper] contredit ou n'a pas.
     * Liste vide = [other] peut être supprimée sans perte.
     */
    fun conflictingFields(keeper: WeldJoint, other: WeldJoint): List<String> {
        val out = mutableListOf<String>()
        for ((label, get) in textFields) {
            val o = normalize(get(other))
            if (o.isNotEmpty() && o != normalize(get(keeper))) out += label
        }
        for ((label, get) in ndtFields) {
            val k = ndt(get(keeper)); val o = ndt(get(other))
            val compatible = o == k || ndtLevel(o) < ndtLevel(k)
            if (!compatible) out += label
        }
        val ks = normalize(keeper.status); val os = normalize(other.status)
        val statusCompatible = os == ks ||
            (statusLevel(os) < statusLevel(ks) && os != "REPAIR_REQUIRED")
        if (!statusCompatible) out += "Statut"
        if (other.diameterInch != keeper.diameterInch) out += "Diamètre"
        if (other.thicknessMm != keeper.thicknessMm) out += "Épaisseur"
        return out
    }
}
