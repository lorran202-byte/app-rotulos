package com.lorran.lupanutri.domain

/** Nomes em português dos aditivos mais comuns em rótulos brasileiros (código INS). */
object Additives {

    /** Aditivos que pedem mais atenção (corantes artificiais, nitritos, alguns adoçantes e emulsificantes). */
    val ATTENTION = setOf(
        "e102", "e104", "e110", "e122", "e124", "e129", "e133", "e150c", "e150d", "e171",
        "e249", "e250", "e251", "e252", "e320", "e321", "e407", "e433", "e466",
        "e621", "e950", "e951", "e952", "e954", "e955",
    )

    val SWEETENERS = setOf(
        "e420", "e421", "e950", "e951", "e952", "e953", "e954", "e955", "e957",
        "e960", "e961", "e962", "e965", "e966", "e967", "e968",
    )

    private val NAMES = mapOf(
        "e100" to "Cúrcuma", "e101" to "Riboflavina", "e102" to "Tartrazina", "e104" to "Amarelo de quinoleína",
        "e110" to "Amarelo crepúsculo", "e120" to "Carmim", "e122" to "Azorrubina", "e124" to "Ponceau 4R",
        "e129" to "Vermelho 40", "e133" to "Azul brilhante", "e140" to "Clorofila", "e141" to "Clorofila cúprica",
        "e150a" to "Caramelo I", "e150b" to "Caramelo II", "e150c" to "Caramelo III", "e150d" to "Caramelo IV",
        "e160a" to "Betacaroteno", "e160b" to "Urucum", "e160c" to "Páprica (extrato)", "e162" to "Beterraba (vermelho)",
        "e171" to "Dióxido de titânio",
        "e200" to "Ácido sórbico", "e202" to "Sorbato de potássio", "e210" to "Ácido benzoico", "e211" to "Benzoato de sódio",
        "e220" to "Dióxido de enxofre", "e223" to "Metabissulfito de sódio", "e249" to "Nitrito de potássio",
        "e250" to "Nitrito de sódio", "e251" to "Nitrato de sódio", "e252" to "Nitrato de potássio",
        "e270" to "Ácido lático", "e290" to "Dióxido de carbono", "e296" to "Ácido málico",
        "e300" to "Ácido ascórbico (vit. C)", "e301" to "Ascorbato de sódio", "e306" to "Tocoferóis (vit. E)",
        "e316" to "Eritorbato de sódio", "e319" to "TBHQ", "e320" to "BHA", "e321" to "BHT",
        "e322" to "Lecitina", "e322i" to "Lecitina", "e325" to "Lactato de sódio", "e327" to "Lactato de cálcio",
        "e330" to "Ácido cítrico", "e331" to "Citrato de sódio", "e332" to "Citrato de potássio", "e334" to "Ácido tartárico",
        "e338" to "Ácido fosfórico", "e339" to "Fosfato de sódio", "e340" to "Fosfato de potássio", "e341" to "Fosfato de cálcio",
        "e350" to "Malato de sódio",
        "e400" to "Ácido algínico", "e401" to "Alginato de sódio", "e406" to "Ágar", "e407" to "Carragena",
        "e410" to "Goma alfarroba", "e412" to "Goma guar", "e414" to "Goma arábica", "e415" to "Goma xantana",
        "e418" to "Goma gelana", "e420" to "Sorbitol", "e421" to "Manitol", "e422" to "Glicerol",
        "e433" to "Polissorbato 80", "e440" to "Pectina", "e450" to "Difosfatos", "e451" to "Trifosfatos",
        "e452" to "Polifosfatos", "e460" to "Celulose", "e466" to "Carboximetilcelulose",
        "e471" to "Mono e diglicerídeos", "e472e" to "Ésteres de mono e diglicerídeos", "e476" to "PGPR",
        "e481" to "Estearoil lactilato de sódio", "e491" to "Monoestearato de sorbitana",
        "e500" to "Bicarbonato de sódio", "e500ii" to "Bicarbonato de sódio", "e503" to "Bicarbonato de amônio",
        "e508" to "Cloreto de potássio", "e509" to "Cloreto de cálcio", "e551" to "Dióxido de silício",
        "e575" to "Glucona-delta-lactona",
        "e621" to "Glutamato monossódico", "e627" to "Guanilato dissódico", "e631" to "Inosinato dissódico",
        "e635" to "Ribonucleotídeos",
        "e900" to "Dimetilpolissiloxano", "e950" to "Acessulfame K", "e951" to "Aspartame", "e952" to "Ciclamato",
        "e953" to "Isomalte", "e954" to "Sacarina", "e955" to "Sucralose", "e957" to "Taumatina",
        "e960" to "Estévia", "e960a" to "Estévia", "e961" to "Neotame", "e965" to "Maltitol", "e967" to "Xilitol",
        "e968" to "Eritritol", "e1422" to "Amido modificado", "e1442" to "Amido modificado",
    )

    /** "en:e330" -> "e330" */
    fun key(tag: String): String = tag.substringAfter(":").lowercase()

    fun base(key: String): String = Regex("""^e\d+""").find(key)?.value ?: key

    fun isAttention(tag: String) = key(tag).let { it in ATTENTION || base(it) in ATTENTION }
    fun isSweetener(tag: String) = key(tag).let { it in SWEETENERS || base(it) in SWEETENERS }
    fun isColorant(tag: String): Boolean {
        val n = base(key(tag)).removePrefix("e").toIntOrNull() ?: return false
        return n in 100..199
    }

    /** Nome legível com o código INS. Ex.: "Ácido cítrico (INS 330)". */
    fun label(tag: String): String {
        val k = key(tag)
        val ins = "INS " + k.removePrefix("e")
        val name = NAMES[k] ?: NAMES[base(k)]
        return if (name != null) "$name ($ins)" else ins
    }
}
