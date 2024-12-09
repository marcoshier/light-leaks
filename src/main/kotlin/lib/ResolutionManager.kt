package lib

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.defaultFontMap
import org.openrndr.events.Event
import org.openrndr.extra.textwriter.writer
import org.openrndr.shape.Rectangle


class ResolutionManager(program: Program) {

    data class Resolution(val width: Int, val height: Int)

    val resolutions = listOf(
        Resolution(1920, 1080),
        Resolution(1280, 720),
        Resolution(640, 480),
        Resolution(480, 480)
    )

    var choice = 0
    var delivered = false

    val resolutionSelected = Event<Resolution>()

    fun deliver() {
        resolutionSelected.trigger(resolutions[choice])
        delivered = true
    }

    init {
        program.keyboard.keyUp.listen {
            if (!delivered) {
                when(it.key) {
                    KEY_ARROW_LEFT, KEY_ARROW_UP -> choice = (choice - 1).coerceIn(0, resolutions.lastIndex)
                    KEY_ARROW_RIGHT, KEY_ARROW_DOWN -> choice = (choice + 1).coerceIn(0, resolutions.lastIndex)
                    KEY_ENTER -> deliver()
                }
            }

        }
    }

    var textWidth = 0.0

    fun draw(drawer: Drawer) {

        drawer.fontMap = defaultFontMap

        val widest = resolutions.map { it }.maxBy { drawer.writer { textWidth("${it.width} x ${it.height}") }}
        textWidth = drawer.writer { textWidth("${widest.width} x ${widest.height}") }

        resolutions.forEachIndexed { i, it ->
            val selected = i == choice

            drawer.writer {

                box = Rectangle(0.0, i * 30.0, textWidth * 1.0, 30.0)

                if (selected) {
                    drawer.fill = if (selected) ColorRGBa.GREEN else ColorRGBa.BLACK
                    drawer.stroke = null
                    drawer.rectangle(box)
                }

                drawer.fill = if (selected) ColorRGBa.BLACK else ColorRGBa.GREEN
                gaplessNewLine()
                text("${it.width} x ${it.height}")
            }
        }

    }

}
