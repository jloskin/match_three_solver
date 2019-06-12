import javafx.fxml.FXML
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import java.awt.Rectangle


class Window {
    private var startPoint: Pair<Double, Double> = Pair(0.0, 0.0)
    private var startPointSize: Pair<Double, Double> = Pair(0.0, 0.0)
    private var startWindowSize: Pair<Double, Double> = Pair(0.0, 0.0)

    @FXML
    private lateinit var screenPane: Pane

    init {
        initCapture()
    }

    private fun initCapture() {
        Thread({
            var time = System.currentTimeMillis()
            while (true) {
                if (System.currentTimeMillis() - time > 3000) {
                    time = System.currentTimeMillis()
                    val bounds = screenPane.boundsInLocal
                    val screenBounds = screenPane.localToScreen(bounds)
                    val x = screenBounds.minX
                    val y = screenBounds.minY
                    val width = screenBounds.width
                    val height = screenBounds.height
                    val input = "./src/main/resources/Shot.png"
                    Solver.makeShot(input, Rectangle(x.toInt(), y.toInt(), width.toInt(), height.toInt()))
                    Solver.solver(input)
                }
            }
        }).start()
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
}