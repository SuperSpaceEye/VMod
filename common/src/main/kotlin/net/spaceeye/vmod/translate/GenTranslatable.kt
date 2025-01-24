package net.spaceeye.vmod.translate

import java.nio.file.Paths
import kotlin.io.path.writeText

fun main() {
    var result = "{"
    var lastKey = ""

    registeredComponents.forEach {
        val key = it.key.split(".").let { it[it.size-2] }
        val doNewline = key != lastKey
        lastKey = key
        result += "${if (doNewline) "\n" else ""}  \"${it.key}\": \"${it.enTranslation.replace("\n", "\\n")}\",\n"
    }

    val constData = """
  "key.vmod.gui_open_or_close": "Open or Close Toolgun GUI",
  "key.vmod.remove_top_constraint": "Remove Top Constraint",
  "key.vmod.reset_constraint_mode": "Reset Constraint Mode",
  "vmod.keymappings_name": "VMod Toolgun",
  
  "item.valkyrien_mod.toolgun": "Toolgun",
  "item.valkyrien_mod.physgun": "Physgun",
  
  "block.valkyrien_mod.simple_messager": "Simple Messager",
  
  "itemGroup.valkyrien_mod.vmod_tab": "VMod"
"""
    result += constData
    result += "}"

    Paths.get("common/src/main/resources/assets/valkyrien_mod/lang/en_us.json").writeText(result)

    println(result)
}