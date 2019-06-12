import javafx.fxml.FXML
import javafx.scene.input.MouseEvent

class Window {
    var startPoint: Pair<Double, Double> = Pair(0.0, 0.0)
    var startPointSize: Pair<Double, Double> = Pair(0.0, 0.0)
    var startWindowSize: Pair<Double, Double> = Pair(0.0, 0.0)

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
        val newWidth = mouseEvent.screenX - startPointSize.first
        val newHeight = mouseEvent.screenY - startPointSize.second

        Main.PRIMARY_STAGE.width = startWindowSize.first + newWidth
        Main.PRIMARY_STAGE.height = startWindowSize.second + newHeight
    }

    @FXML
    fun initXySizer(mouseEvent: MouseEvent) {
        startPointSize = Pair(mouseEvent.screenX, mouseEvent.screenY)
        startWindowSize = Pair(Main.PRIMARY_STAGE.width, Main.PRIMARY_STAGE.height)
    }
}