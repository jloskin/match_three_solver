import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.TextField
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import java.awt.Rectangle
import java.net.URL
import java.util.*


class Window : Initializable {
    private var startPoint: Pair<Double, Double> = Pair(0.0, 0.0)
    private var startPointSize: Pair<Double, Double> = Pair(0.0, 0.0)
    private var startWindowSize: Pair<Double, Double> = Pair(0.0, 0.0)

    @FXML
    private lateinit var screenPane: Pane
    @FXML
    private lateinit var columnCount: TextField
    @FXML
    private lateinit var rowCount: TextField

    private lateinit var canvas: ResizableCanvas

    override fun initialize(location: URL, resources: ResourceBundle?) {
        canvas = ResizableCanvas().apply {
            widthProperty().bind(screenPane.widthProperty())
            heightProperty().bind(screenPane.heightProperty())
        }
        screenPane.children.add(canvas)
        columnCount.textProperty().addListener { _, _, newValue -> canvas.column.value = newValue.toIntOrNull() }
        rowCount.textProperty().addListener { _, _, newValue -> canvas.row.value = newValue.toIntOrNull() }
    }

    private var start: Boolean = false
    private var thread: Thread? = null

    private fun initCapture(templates: Set<Int>) {
        start = true
        thread = Thread(thread(templates)).also(Thread::start)
    }

    private fun thread(templates: Set<Int>): () -> Unit = {
        var time = 0L
        while (start) {
            if (System.currentTimeMillis() - time > 500) {
                time = System.currentTimeMillis()

                val input = "input.png"
                Solver.makeShot(
                    input,
                    screenPane.localToScreen(screenPane.boundsInLocal).let {
                        Rectangle(
                            it.minX.toInt(),
                            it.minY.toInt(),
                            it.width.toInt(),
                            it.height.toInt()
                        )
                    }
                )
                val step = Solver.solver(input, templates)
                println(step)
                if (step.combination.height != -1 && step.combination.width != -1)
                    Platform.runLater {
                        canvas.drawBlock(step)
                    }
            }
        }
    }

    @FXML
    fun initWindowMove(event: MouseEvent) {
        startPoint = Pair(event.sceneX, event.sceneY)
    }

    @FXML
    fun windowDragged(event: MouseEvent) {
        Main.PRIMARY_STAGE.x = event.screenX - startPoint.first
        Main.PRIMARY_STAGE.y = event.screenY - startPoint.second
    }

    @FXML
    fun xyFrameSizer(mouseEvent: MouseEvent) {
        Main.PRIMARY_STAGE.width = startWindowSize.first + mouseEvent.screenX - startPointSize.first
        Main.PRIMARY_STAGE.height = startWindowSize.second + mouseEvent.screenY - startPointSize.second
    }

    @FXML
    fun initXySizer(mouseEvent: MouseEvent) {
        startPointSize = Pair(mouseEvent.screenX, mouseEvent.screenY)
        startWindowSize = Pair(Main.PRIMARY_STAGE.width, Main.PRIMARY_STAGE.height)
    }

    @FXML
    fun initParser(mouseEvent: MouseEvent) {
        Platform.runLater {
            screenPane.styleProperty().value = "-fx-background-color: null;"
            canvas.clear()
            Thread({
                Thread.sleep(2000)
                val templates = canvas.patterns().mapIndexed { a, b ->
                    Solver.makeShot("$a.png", b)
                    a
                }.toSet()

                initCapture(templates)
            }).start()
        }
    }

    @FXML
    fun clearAll(mouseEvent: MouseEvent) {
        start = false
        thread?.interrupt()

        screenPane.styleProperty().value = "-fx-background-color: rgba(255, 255, 255, 0.6);"
        canvas.points.clear()
        canvas.redraw()
    }
}