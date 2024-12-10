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
import org.openrndr.extra.parameters.listParameters
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
        var lightRadius = 5.0

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


        val pix = Pixelate()
        val pf = PoissonFill()
        val lb = LaserBlur()


        val ranges = (listParameters() + lb.listParameters()).associate { it.label to it.doubleRange }

        fun Int.midiMapped(label: String): Double {
            val l = ranges.getValue(label)!!.start
            val r = ranges.getValue(label)!!.endInclusive

            return this.toDouble().map(0.0, 127.0, l, r, true)
        }

        fun assignValue(label: String, value: Int) {
            when(label) {
                "n" -> n = value.midiMapped(label).toInt()
                "lightRadius" ->  lightRadius = value.midiMapped(label)
                "xOffset" ->  xOffset = value.midiMapped(label)
                "movY" ->  movY = value.midiMapped(label)
                "movX" ->  movX = value.midiMapped(label)
                "speedY" ->  speedY = value.midiMapped(label)
                "speedX" ->  speedX = value.midiMapped(label)
                "offsetX" ->  offsetX = value.midiMapped(label)
                "offsetY" ->  offsetY = value.midiMapped(label)
                "resolution" ->  pix.resolution = value.midiMapped(label)
                "radius" -> lb.radius = value.midiMapped(label)
                "amp0" -> lb.amp0 = value.midiMapped(label)
                "amp1" -> lb.amp1 = value.midiMapped(label)
                "exp" -> lb.exp = value.midiMapped(label)
                "phase" -> lb.phase = value.midiMapped(label)
            }
        }

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
                drawer.circle(p, lightRadius)
            }

            drawer.strokeWeight = strokeWeight
            drawer.stroke = ColorRGBa.BLACK
            drawer.fill = null
            drawer.rectangle(drawer.bounds)
        }
    }


    val stage = Stage()

    var learning = false

    keyboard.character.listen {
        if (it.character == 'l') {
            learning = !learning
        }
    }

    val parameters = stage.listParameters() + stage.lb.listParameters()
    val unassigned = parameters.map { it.label }.toMutableList()

    val map = mutableMapOf<Int, String>()

    midiTransceiver?.controlChanged!!.listen {
        println(it.control)

        val (i, value) = it.control to it.value

        val label = map.getOrPut(i) {
            val unassignedValue = unassigned.firstOrNull()
            if (unassignedValue == null) { "" }
            else {
                unassigned.remove(unassignedValue)
                unassignedValue
            }
        }

        stage.assignValue(label, value)
    }


    extend(Post()) {
        post { input, output ->
            val i1 = intermediate[1]
            stage.pf.apply(input, i1)
            if (stage.dither || stage.pix.resolution == 0.0) {
                val i2 = intermediate[2]
                stage.lb.apply(i1, i2)
                stage.pix.apply(i2, output)
            } else {
                stage.lb.apply(i1, output)
            }

        }
    }

    extend {
        stage.draw()
    }
}