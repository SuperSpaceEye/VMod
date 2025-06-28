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
  "key.vmod.remove_top_ventity": "Remove Top VEntity",
  "key.vmod.reset_ventity_mode": "Reset VEntity Mode",
  "key.vmod.toggle_hud": "Toggle HUD",
  "key.vmod.toggle_hud_info": "Toggle HUD Info",
  "vmod.keymappings_name": "VMod Toolgun",
  
  "item.the_vmod.toolgun": "Toolgun",
  "item.the_vmod.physgun": "Physgun",
  
  "block.the_vmod.simple_messager": "Simple Messager",
  
  "itemGroup.the_vmod.vmod_tab": "VMod"
"""
    result += constData
    result += "}"

    Paths.get("common/src/main/resources/assets/the_vmod/lang/en_us.json").writeText(result)

    println(result)
}