package lib

import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.defaultFontMap
import org.openrndr.extra.fx.Post
import org.openrndr.extra.fx.blur.LaserBlur
import org.openrndr.extra.midi.MidiTransceiver
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.math.map
import org.openrndr.poissonfill.PoissonFill
import org.openrndr.shape.LineSegment
import kotlin.math.cos
import kotlin.math.sin


fun Program.lightLeaks(
    midiTransceiver: MidiTransceiver?
) {

    class Stage {

        @IntParameter("numero luci", 1, 20)
        var n = 5

        @DoubleParameter("radius", 0.1, 20.0)
        var radius = 5.0

        @DoubleParameter("larghezza", 0.0, 400.0)
        var xOffset = 10.0

        @DoubleParameter("buio", 0.0, 300.0)
        var strokeWeight = 2.0

        @DoubleParameter("movimento Y", 0.0, 1.0)
        var movY = 0.5

        @DoubleParameter("velocità movimento Y", 0.0, 300.0)
        var speedY = 2.0

        @DoubleParameter("variazione movimento Y", 0.0, 50.0, precision = 3)
        var offsetY = 2.0

        @DoubleParameter("movimento X", 0.0, 1.0)
        var movX = 0.5

        @DoubleParameter("velocità movimento X", 0.0, 300.0)
        var speedX = 2.0

        @DoubleParameter("variazione movimento X", 0.0, 50.0, precision = 3)
        var offsetX = 2.0

        @BooleanParameter("dithering")
        var dither = true

        fun draw() {
            drawer.clear(ColorRGBa.TRANSPARENT)
            var origins = listOf(drawer.bounds.center)
            if (n > 1) {
                val bounds = drawer.bounds.offsetEdges(-xOffset)
                origins = LineSegment(
                    bounds.x,
                    height / 2.0,
                    bounds.x + bounds.width,
                    height / 2.0
                ).contour.equidistantPositions(n).mapIndexed { i, it ->
                    it.copy(
                        x = it.x + (cos(seconds * speedX + i * offsetX) * (width / 2.0)) * movX,
                        y = sin(seconds * speedY + i * offsetY) * (height / 2.0) * movY + (height / 2.0)
                    )
                }
            }

            drawer.stroke = null
            for (p in origins) {
                drawer.stroke = null
                drawer.fill = ColorRGBa.WHITE
                drawer.circle(p, radius)
            }

            drawer.strokeWeight = strokeWeight
            drawer.stroke = ColorRGBa.BLACK
            drawer.fill = null
            drawer.rectangle(drawer.bounds)
        }
    }


    val stage = Stage()

    val pf = PoissonFill()
    val lb = LaserBlur()
    val pix = Pixelate()

    midiTransceiver?.controlChanged?.listen {
        val (i, value) = it.control to it.value
        when (i) {
            0 -> stage.n = map(0.0, 127.0, 0.0, 20.0, value.toDouble()).toInt()
            1 -> stage.radius = map(0.0, 127.0, 0.1, 100.0, value.toDouble())
            2 -> stage.xOffset = map(0.0, 127.0, 0.0, 400.0, value.toDouble())
            3 -> stage.movY = map(0.0, 127.0, 0.0, 2.0, value.toDouble())
            4 -> stage.speedY = map(0.0, 127.0, 0.0, 500.0, value.toDouble())
            5 -> stage.movX = map(0.0, 127.0, 0.0, 2.0, value.toDouble())
            6 -> stage.speedX = map(0.0, 127.0, 0.0, 500.0, value.toDouble())
            23 -> stage.offsetX = map(0.0, 127.0, 0.0, 80.0, value.toDouble())
            16 -> pix.resolution = map(0.0, 127.0, 0.01, 1.0, value.toDouble())
            17 -> lb.radius = map(0.0, 127.0, -2.0, 2.0, value.toDouble())
            18 -> lb.amp0 = map(0.0, 127.0, 0.0, 1.0, value.toDouble())
            19 -> lb.amp1 = map(0.0, 127.0, 0.0, 1.0, value.toDouble())
            20 -> lb.exp = map(0.0, 127.0, -1.0, 1.0, value.toDouble())
            21 -> lb.phase = map(0.0, 127.0, -1.0, 1.0, value.toDouble())
            22 -> lb.radius = map(0.0, 127.0, -2.0, 2.0, value.toDouble())
        }

    }


    extend(Post()) {
        post { input, output ->
            val i1 = intermediate[1]
            pf.apply(input, i1)
            if (stage.dither || pix.resolution == 0.0) {
                val i2 = intermediate[2]
                lb.apply(i1, i2)
                pix.apply(i2, output)
            } else {
                lb.apply(i1, output)
            }

        }
    }

    extend {
        stage.draw()

        if (midiTransceiver == null) {
            drawer.fontMap = defaultFontMap
            drawer.fill = ColorRGBa.RED
            drawer.text("could not open midi device", 30.0, 50.0)
        }
    }
}