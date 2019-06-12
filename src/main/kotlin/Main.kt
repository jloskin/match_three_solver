import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Border
import javafx.scene.layout.BorderStroke
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
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
    override fun start(
        primaryStage: Stage
    ) {
        primaryStage.apply {
            isAlwaysOnTop = true
            scene = Scene(
                VBox().apply {
                    border = Border(BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, null, null))
                    style = "-fx-background-color: rgba(0,0,0,0);"
                }
            ).apply {
                initStyle(StageStyle.TRANSPARENT)
                fill = null
                addEventFilter(KeyEvent.KEY_RELEASED, {
                    println("click")
                })
            }
            width = 200.0
            height = 100.0
        }.show()
    }

    companion object {
        @JvmStatic
        fun main(
            args: Array<String>
        ) {
/*
            val path = "input/"

            val input = "${path}aa.png"
            val output = "output.png"
            val runes: Map<String, Int> = mapOf(
                "${path}simple/air.png" to 1,
                "${path}broken/air.png" to 1,
                "${path}simple/water.png" to 2,
                "${path}broken/water.png" to 2,
                "${path}simple/void.png" to 3,
                "${path}broken/void.png" to 3,
                "${path}simple/earth.png" to 4,
                "${path}broken/earth.png" to 4,
                "${path}broken/earth_2.png" to 4,
                "${path}simple/fire.png" to 5,
                "${path}broken/fire.png" to 5
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

            solve(array)*/
            launch(Main::class.java)
//            System.exit(0)
        }

        private fun solve(
            array: Array<Array<Int>>
        ) = time("solve") {
            (0 until array.size).forEach { x ->
                (0 until array[x].size).forEach { y ->
                    simpleSolver(array, Coordinate(x, y))
                }
            }
        }

        data class Coordinate(
            val x: Int,
            val y: Int
        )

        data class Combination(
            val width: Int,
            val height: Int
        )

        private fun simpleSolver(
            array: Array<Array<Int>>,
            position: Coordinate
        ) {
            val item = array[position.x][position.y]
            if (item < 0) return

            val bottom = checkCombination(array, position, position.copy(y = position.y + 1))
            val top = checkCombination(array, position, position.copy(y = position.y - 1))
            val right = checkCombination(array, position, position.copy(x = position.x + 1))
            val left = checkCombination(array, position, position.copy(x = position.x - 1))
        }

        private fun checkCombination(
            array: Array<Array<Int>>,
            position: Coordinate,
            newPosition: Coordinate
        ): Boolean {
            if (newPosition.x < 0 || newPosition.x >= array.size) return false
            if (newPosition.y < 0 || newPosition.y >= array[newPosition.x].size) return false

            val combination = checkCombination(
                copyAndRotate(array, position, newPosition),
                newPosition
            )

            if ((combination.width >= 4 || combination.height >= 4)) {
                println(position)
                println(combination)
            }

            return true
        }

        private fun copyAndRotate(
            array: Array<Array<Int>>,
            position: Coordinate,
            newPosition: Coordinate
        ): Array<Array<Int>> =
            array.map(Array<Int>::clone).toTypedArray().apply {
                val tmp = array[position.x][position.y]
                this[position.x][position.y] = this[newPosition.x][newPosition.y]
                this[newPosition.x][newPosition.y] = tmp
            }

        private fun checkCombination(
            deepCopy: Array<Array<Int>>,
            position: Coordinate
        ): Combination {
            val centerItem = deepCopy[position.x][position.y]

            val left = (position.x - 1 downTo 0)
                .find { deepCopy[it][position.y] != centerItem }
                ?.let { it + 1 }
                ?: 0
            val right = (position.x + 1 until deepCopy.size)
                .find { deepCopy[it][position.y] != centerItem }
                ?.let { it - 1 }
                ?: (deepCopy.size - 1)
            val top = (position.y - 1 downTo 0)
                .find { deepCopy[position.x][it] != centerItem }
                ?.let { it + 1 }
                ?: 0
            val bottom = (position.y + 1 until deepCopy[position.x].size)
                .find { deepCopy[position.x][it] != centerItem }
                ?.let { it - 1 }
                ?: (deepCopy[position.x].size - 1)

            return Combination(right - left + 1, bottom - top + 1)
        }

        private fun transformToArray(
            coordinates: Set<Pair<String, Point>>,
            templates: Map<String, Int>
        ): Array<Array<Int>> =
            time("transformToArray") {
                val width = coordinates.groupBy { it.second.x }.size
                val height = coordinates.groupBy { it.second.y }.size
                val array = Array(width, { Array(height, { -1 }) })
                coordinates.forEach {
                    array[it.second.x.toInt()][it.second.y.toInt()] = templates[it.first] ?: error("")
                }
                array
            }

        private fun <T> time(
            name: String,
            func: () -> T
        ): T {
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

        private fun removeDuplicate(
            items: Set<Pair<String, Point>>,
            tolerance: Int = 10
        ): Set<Pair<String, Point>> =
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


        private fun parseImage(
            input: String,
            templates: Set<String>,
            output: String
        ): Set<Pair<String, Point>> =
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
