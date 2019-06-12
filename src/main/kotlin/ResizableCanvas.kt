import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import java.awt.Rectangle

class ResizableCanvas(
    val column: IntegerProperty = SimpleIntegerProperty(0),
    val row: IntegerProperty = SimpleIntegerProperty(0),
    val points: MutableSet<Pair<Int, Int>> = mutableSetOf()
) : Canvas() {

    init {
        // Redraw canvas when size changes.
        widthProperty().addListener { _ -> redraw() }
        heightProperty().addListener { _ -> redraw() }
        column.addListener { _ -> redraw() }
        row.addListener { _ -> redraw() }
        setEventHandler(MouseEvent.MOUSE_RELEASED, {
            val columnWidth = width / column.value
            val rowHeight = height / row.value
            val pair = Pair((it.x / columnWidth).toInt(), (it.y / rowHeight).toInt())
            if (points.contains(pair)) points -= pair
            else points += pair
            redraw()
        })
    }

    fun redraw() {
        with(graphicsContext2D) {
            clearRect(0.0, 0.0, width, height)
            globalAlpha = 1.0
            stroke = Color.GREEN

            val columnWidth = width / column.value
            val rowHeight = height / row.value

            (0 until column.value)
                .map { it * columnWidth }
                .forEach { strokeLine(it, 0.0, it, height) }
            (0 until row.value)
                .map { it * rowHeight }
                .forEach { strokeLine(0.0, it, width, it) }

            globalAlpha = 0.1
            fill = Color.RED
            points.forEach {
                val d = it.first * columnWidth
                val d1 = it.second * rowHeight
                fillRect(10 + d, 10 + d1, columnWidth - 20, rowHeight - 20)
            }
        }
    }

    override fun isResizable(): Boolean = true

    override fun prefWidth(height: Double): Double = width

    override fun prefHeight(width: Double): Double = height

    fun patterns(): Set<Rectangle> {
        val columnWidth = width / column.value
        val rowHeight = height / row.value
        return points.map {
            localToScreen(
                (it.first * columnWidth),
                (it.second * rowHeight)
            ).let {
                Pair(it.x.toInt(), it.y.toInt())
            }.let { (x, y) ->
                Rectangle(
                    x + 10, y + 10,
                    columnWidth.toInt() - 20, rowHeight.toInt() - 20
                )
            }
        }.toSet()
    }

    fun clear() {
        with(graphicsContext2D) {
            clearRect(0.0, 0.0, width, height)
        }
    }

    fun drawBlock(step: Solver.Step) {
        with(graphicsContext2D) {
            clearRect(0.0, 0.0, width, height)

            val columnWidth = width / column.value
            val rowHeight = height / row.value
            globalAlpha = 1.0
            stroke = Color.GREEN
            lineWidth = 3.0
            strokeRect(step.from.x * columnWidth, step.from.y * rowHeight, columnWidth, rowHeight)
            stroke = Color.RED
            strokeRect(step.to.x * columnWidth, step.to.y * rowHeight, columnWidth, rowHeight)
        }
    }
}