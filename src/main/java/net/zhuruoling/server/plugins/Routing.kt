package net.zhuruoling.server.plugins

import com.google.gson.GsonBuilder
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.zhuruoling.server.routes.announcementRouting
import org.slf4j.LoggerFactory

fun Application.configureRouting() {
    val logger = LoggerFactory.getLogger("HTTPServer")
    routing{
        announcementRouting()
    }
}