import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.stage.StageStyle


class Main : Application() {
    override fun start(
        primaryStage: Stage
    ) {
        PRIMARY_STAGE = primaryStage
        primaryStage.apply {
            isAlwaysOnTop = true
            scene = Scene(FXMLLoader.load(this@Main.javaClass.getResource("root.fxml"))).apply {
                initStyle(StageStyle.TRANSPARENT)
                fill = null
            }
        }.show()
    }

    companion object {
        lateinit var PRIMARY_STAGE: Stage

        @JvmStatic
        fun main(args: Array<String>) {
            launch(Main::class.java)
        }
    }
}
