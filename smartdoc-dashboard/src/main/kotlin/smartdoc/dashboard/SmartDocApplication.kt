package smartdoc.dashboard

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

/**
 *
 * RestDoc Springboot Application
 *
 * @since 1.0
 */
@SpringBootApplication
@Import()
open class SmartDocApplication

/**
 * Bootstrap main fun
 */
fun main(args: Array<String>) {
    runApplication<SmartDocApplication>(*args)
}


