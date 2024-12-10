import lib.*
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.defaultFontMap
import org.openrndr.draw.loadFont
import org.openrndr.extra.midi.MidiTransceiver
import org.openrndr.extra.midi.openMidiDevice
import org.openrndr.extra.viewbox.viewBox
import javax.sound.midi.MidiSystem

fun main() {

    val (config, midi) = selectionScreen()

    mainProgram(config, midi)

}


fun selectionScreen(): Pair<Configuration, MidiInfo?> {
    var config: Configuration? = null
    var midiDevice: MidiInfo? = null

    application {
        program {

            val midiManager = MidiManager(this)
            var resolutionManager: ResolutionManager? = null
            var fullscreenManager: FullscreenManager? = null

            midiManager.deviceSelected.listen {
                resolutionManager = ResolutionManager(this)
                midiDevice = midiManager.selectedDevice

                println(midiDevice)

                resolutionManager?.resolutionSelected?.listen { resolution ->
                    fullscreenManager = FullscreenManager(this)

                    fullscreenManager?.fullscreenSelected?.listen { fullscreen ->
                        config = Configuration().apply {
                            this.width = resolution.width
                            this.height = resolution.height
                            this.fullscreen = if (fullscreen) Fullscreen.SET_DISPLAY_MODE else Fullscreen.DISABLED
                        }

                        application.exit()
                    }
                }
            }

            extend {
                midiManager.draw(drawer)
                drawer.translate(midiManager.textWidth, 0.0)
                resolutionManager?.draw(drawer)
                resolutionManager?.let {
                    drawer.translate(it.textWidth, 0.0)
                    fullscreenManager?.draw(drawer)
                }
            }
        }
    }

    return config!! to midiDevice
}



fun mainProgram(config: Configuration, midiInfo: MidiInfo?) {
    application {
        configure {
            this.width = config.width
            this.height = config.height
            this.fullscreen = config.fullscreen
        }

        program {

            val midiDevice = if (midiInfo?.receiver == null) {
                MidiTransceiver.fromDeviceVendor(this, midiInfo!!.name, midiInfo.vendor)
            } else {
                MidiTransceiver(this, midiInfo.receiver, midiInfo.transmitter)
            }


            val vb = viewBox(drawer.bounds) {
                lightLeaks(midiDevice)
            }

            extend {
                vb.draw()
            }
        }

    }
}