package lib

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.defaultFontMap
import org.openrndr.events.Event
import org.openrndr.extra.midi.MidiTransceiver
import org.openrndr.extra.midi.listMidiDevices
import org.openrndr.extra.textwriter.writer
import org.openrndr.shape.Rectangle
import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiSystem

data class MidiInfo(val name: String, val vendor: String, val transmitter: MidiDevice? = null, val receiver: MidiDevice? = null)

class MidiManager(val program: Program) {

    val devices = listMidiDevices()

    var choice = 0
    var delivered = false

    val recName = "CTRL"
    val transName = "SLIDER"

    val deviceSelected = Event<Unit>()

    var selectedDevice: MidiInfo? = null

    fun deliver() {
        try {
            devices[choice].open(program)
            selectedDevice = MidiInfo(devices[choice].name, devices[choice].vendor)
            return
        } catch (e: Throwable) {
            if (e is java.lang.IllegalArgumentException) {
                val info = MidiSystem.getMidiDeviceInfo()
                println(info)

                for (device in info) {
                    for (other in info.toList().minus(device)) {
                        if (device.name == other.name) {
                            val rec = MidiSystem.getMidiDevice(info.first { it.name.contains(recName) }).apply { open() }
                            val trans = MidiSystem.getMidiDevice(info.first { it.name.contains(transName) }).apply { open() }

                            selectedDevice = MidiInfo("", "", trans, rec)
                            return
                        }
                    }
                }
            } else {
                e.printStackTrace()
            }
        } finally {
            if (selectedDevice != null) {
                deviceSelected.trigger(Unit)
            } else {
                println("failed to create working device")
            }

            delivered = true
        }

    }

    init {
        program.keyboard.keyUp.listen {
            if (!delivered) {
                when(it.key) {
                    KEY_ARROW_LEFT, KEY_ARROW_UP -> choice = (choice - 1).coerceIn(0, devices.lastIndex)
                    KEY_ARROW_RIGHT, KEY_ARROW_DOWN -> choice = (choice + 1).coerceIn(0, devices.lastIndex)
                    KEY_ENTER -> deliver()
                }
            }

        }

    }

    var textWidth = 0.0

    fun draw(drawer: Drawer) {

        drawer.fontMap = defaultFontMap

        val widest = devices.maxBy { drawer.writer { textWidth(it.name) } }
        textWidth = drawer.writer { textWidth(widest.name) }

        devices.forEachIndexed { i, it ->
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
                text(it.name)

            }
        }

    }

}
