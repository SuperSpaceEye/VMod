package net.spaceeye.vmod

import net.spaceeye.vmod.config.AbstractConfigBuilder
import net.spaceeye.vmod.config.ConfigSubDirectory

object VMConfig {
    lateinit var server_config_holder: AbstractConfigBuilder
    lateinit var client_config_holder: AbstractConfigBuilder
    lateinit var common_config_holder: AbstractConfigBuilder

    val SERVER = Server()
    val CLIENT = Client()
    val COMMON = Common()

    class Client: ConfigSubDirectory()
    class Common: ConfigSubDirectory()
    class Server: ConfigSubDirectory()
}