import lib.FullscreenManager
import lib.MidiManager
import lib.ResolutionManager
import lib.lightLeaks
import org.openrndr.*
import org.openrndr.extra.midi.MidiTransceiver
import org.openrndr.extra.viewbox.viewBox

fun main() {

    val (config, midi) = selectionScreen()

    mainProgram(config, midi)

}


fun selectionScreen(): Pair<Configuration, MidiTransceiver?> {
    var config: Configuration? = null
    var midiDevice: MidiTransceiver? = null

    application {
        program {

            val midiManager = MidiManager(this)
            var resolutionManager: ResolutionManager? = null
            var fullscreenManager: FullscreenManager? = null

            midiManager.deviceSelected.listen {
                resolutionManager = ResolutionManager(this)

                resolutionManager?.resolutionSelected?.listen { resolution ->
                    fullscreenManager = FullscreenManager(this)

                    fullscreenManager?.fullscreenSelected?.listen { fullscreen ->
                        config = Configuration().apply {
                            this.width = resolution.width
                            this.height = resolution.height
                            this.fullscreen = if (fullscreen) Fullscreen.SET_DISPLAY_MODE else Fullscreen.DISABLED
                        }

                        midiDevice = midiManager.selectedDevice
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



fun mainProgram(config: Configuration, midi: MidiTransceiver?) {
    application {
        configure {
            this.width = config.width
            this.height = config.height
            this.fullscreen = config.fullscreen
        }

        program {

            val vb = viewBox(drawer.bounds) {
                lightLeaks(midi)
            }

            extend {
                vb.draw()
            }
        }

    }
}