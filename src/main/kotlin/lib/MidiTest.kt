package lib

import org.openrndr.application
import org.openrndr.extra.midi.listMidiDevices

fun main() {
    application {


        program {

            val midi = listMidiDevices().first { it.name.contains("LPD") }.open(this)

            midi.controlChanged.listen {
                println("changed")
            }

            extend {


            }
        }
    }
}