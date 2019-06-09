import javafx.application.Application
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.input.KeyEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.Screen.getPrimary
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.io.File
import javax.imageio.ImageIO


class Main : Application() {
    override fun start(primaryStage: Stage) {
        val screen = getPrimary()
        val bounds = screen.visualBounds

        primaryStage.x = bounds.minX
        primaryStage.y = bounds.minY
        primaryStage.width = bounds.width
        primaryStage.height = bounds.height
        primaryStage.isMaximized = true
        primaryStage.initStyle(StageStyle.TRANSPARENT)
        primaryStage.isAlwaysOnTop = true
        primaryStage.scene = Scene(
            StackPane().apply {
                children.add(
                    Pane().apply {
                        setMaxSize(50.0, 50.0)
                        background = Background(BackgroundFill(Color.web("#00ff00"), CornerRadii.EMPTY, Insets.EMPTY))
                    }
                )
                layoutX = 0.0
                layoutY = 0.0
            }
        ).apply {
            fill = Color.TRANSPARENT
            addEventFilter(KeyEvent.KEY_RELEASED, {
                println("click")
            })
        }
        primaryStage.show()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
//            makeShot()
            val path = "input/"

            val input = "${path}aa.png"
            val output = "output.png"
            val runes: Map<String, Int> = mapOf(
                "${path}air.png" to 1,
                "${path}water.png" to 2,
                "${path}void.png" to 3,
                "${path}earth.png" to 4,
                "${path}fire.png" to 5
            )

            val array = transformToArray(
                normalizeCoordinates(
                    removeDuplicate(
                        parseImage(
                            input,
                            runes.entries.map(Map.Entry<String, Int>::key).toSet(),
                            output
                        )
                    )
                ),
                runes
            )
            array.map { it.map(Int::toString).map { it.padStart(3) }.joinToString(",") }.forEach(::println)
            //launch(Main::class.java)
            System.exit(0)
        }

        private fun transformToArray(
            coordinates: Set<Pair<String, Point>>,
            templates: Map<String, Int>
        ): Array<Array<Int>> =
            time("transformToArray") {
                val width = coordinates.groupBy { it.second.y }.size
                val height = coordinates.groupBy { it.second.x }.size
                val array = Array(width, { Array(height, { -1 }) })
                coordinates.forEach {
                    array[it.second.y.toInt()][it.second.x.toInt()] = templates[it.first] ?: error("")
                }
                array
            }

        private fun <T> time(name: String, func: () -> T): T {
            val start = System.currentTimeMillis()
            val result = func()
            val end = System.currentTimeMillis()
            println("Method: $name. Duration: ${end - start}.")
            return result
        }

        private fun normalizeCoordinates(
            uniquePoints: Set<Pair<String, Point>>,
            tolerance: Int = 10
        ): Set<Pair<String, Point>> =
            time("normalizeCoordinates") {
                val sortByX = uniquePoints.sortedBy { it.second.x }
                var xx = 0
                (0 until sortByX.size - 1)
                    .forEach {
                        val a = sortByX[it].second
                        val b = sortByX[it + 1].second

                        if (Math.abs(a.x - b.x) < tolerance) {
                            b.x = a.x
                            a.x = xx.toDouble()
                        } else {
                            a.x = xx.toDouble()
                            xx++
                        }

                        if (it == sortByX.size - 2) b.x = xx.toDouble()
                    }

                val sortByY = uniquePoints.sortedBy { it.second.y }
                var yy = 0
                (0 until sortByY.size - 1)
                    .forEach {
                        val a = sortByY[it].second
                        val b = sortByY[it + 1].second

                        if (Math.abs(a.y - b.y) < tolerance) {
                            b.y = a.y
                            a.y = yy.toDouble()
                        } else {
                            a.y = yy.toDouble()
                            yy++
                        }

                        if (it == sortByY.size - 2) b.y = yy.toDouble()
                    }
                uniquePoints
            }

        private fun removeDuplicate(items: Set<Pair<String, Point>>, tolerance: Int = 10): Set<Pair<String, Point>> =
            time("removeDuplicate") {
                items.forEach { first ->
                    val firX = first.second.x.toInt()
                    val firY = first.second.y.toInt()

                    items.forEach { second ->
                        val secX = second.second.x.toInt()
                        val secY = second.second.y.toInt()

                        val raznX = Math.abs(firX - secX) < tolerance
                        val raznY = Math.abs(firY - secY) < tolerance

                        if (raznX && raznY && first != second) {
                            first.second.x = second.second.x
                            first.second.y = second.second.y
                        }
                    }
                }

                items.distinctBy(Pair<String, Point>::second).toSet()
            }


        private fun parseImage(input: String, templates: Set<String>, output: String): Set<Pair<String, Point>> =
            time("parseImage") {
                System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
                run(
                    input,
                    templates,
                    output,
                    Imgproc.TM_CCOEFF_NORMED
                )
            }

        private fun makeShot() {
            val r = Robot()

            // It saves screenshot to desired path
            val path = "./src/main/resources/Shot.jpg"

            // Used to get ScreenSize and capture image
            val capture = Rectangle(Toolkit.getDefaultToolkit().screenSize)
            val image = r.createScreenCapture(capture)
            ImageIO.write(image, "jpg", File(path))
            println("Screenshot saved")
        }

        private fun run(
            inFile: String,
            templateFile: Set<String>,
            outFile: String,
            match_method: Int,
            array: Set<Pair<String, Point>> = setOf()
        ): Set<Pair<String, Point>> {
            if (templateFile.isEmpty()) return array

            val arr = mutableSetOf<Point>()
            val first = templateFile.first()
            val img = Imgcodecs.imread(inFile)
            val template = Imgcodecs.imread(first)

            // / Create the result matrix
            val resultCols = img.cols() - template.cols() + 1
            val resultRows = img.rows() - template.rows() + 1
            val result = Mat(resultRows, resultCols, CvType.CV_32FC1)

            // / Do the Matching and Normalize
            Imgproc.matchTemplate(img, template, result, match_method)
            Core.normalize(result, result, 0.0, 1.0, Core.NORM_MINMAX, -1, Mat())

            while (true) {
                val mmr = Core.minMaxLoc(result)
                val matchLoc = mmr.maxLoc
                if (mmr.maxVal >= 0.9) {
                    val point = Point(matchLoc.x + template.cols(), matchLoc.y + template.rows())
                    arr += Point(matchLoc.x, matchLoc.y)
                    val color = Scalar(0.0, 255.0, 0.0)

                    Imgproc.rectangle(
                        img,
                        matchLoc,
                        point,
                        color
                    )
                    Imgproc.rectangle(
                        result,
                        matchLoc,
                        point,
                        color,
                        -1
                    )
                } else {
                    break
                }
            }

            Imgcodecs.imwrite(outFile, img)

            return run(
                outFile,
                templateFile - first,
                outFile,
                match_method,
                array + arr.map { Pair(first, it) }
            )
        }
    }
}
