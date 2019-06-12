import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.awt.Rectangle
import java.awt.Robot
import java.io.File
import javax.imageio.ImageIO

object Solver {
    fun solver(input: String, templates: Set<Int>): Step {
        val output = "output.png"
        val runes: Map<String, Int> = templates.map { "$it.png" to it }.toMap()

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

        return solve(array)
    }

    private fun solve(
        array: Array<Array<Int>>
    ): Step = time("solve") {
        val flatten = (0 until array.size).map { x ->
            (0 until array[x].size).map { y ->
                simpleSolver(array, Coordinate(x, y))
            }.flatten()
        }.flatten()
        findBest(flatten)
    }

    private fun findBest(flatten: List<Step>): Step {
        val comb = flatten.filter { it.combination.height >= 3 || it.combination.width >= 3 }
        val maximum = comb.maxBy { it.combination.height + it.combination.width }
        return if (maximum != null && maximum.let { it.combination.width + it.combination.height - 1 } >= 5) {
            maximum
        } else {
            comb.find { it.combination.height == 5 || it.combination.width == 5 }
                ?: comb.find { it.combination.height == 4 || it.combination.width == 4 }
                ?: comb.find { it.combination.height == 3 || it.combination.width == 3 }
                ?: Step(Coordinate(-1, -1), Coordinate(-1, -1))
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

    data class Step(
        val from: Coordinate,
        val to: Coordinate,
        val combination: Combination = Combination(-1, -1)
    )

    private fun simpleSolver(
        array: Array<Array<Int>>,
        position: Coordinate
    ): Set<Step> {
        val item = array[position.x][position.y]
        if (item < 0) return setOf()

        val bottom: Step = checkCombination(array, position, position.copy(y = position.y + 1))
        val top: Step = checkCombination(array, position, position.copy(y = position.y - 1))
        val right: Step = checkCombination(array, position, position.copy(x = position.x + 1))
        val left: Step = checkCombination(array, position, position.copy(x = position.x - 1))

        return setOf(
            bottom,
            top,
            right,
            left
        )
    }

    private fun checkCombination(
        array: Array<Array<Int>>,
        position: Coordinate,
        newPosition: Coordinate
    ): Step {
        if (newPosition.x < 0 || newPosition.x >= array.size) return Step(position, newPosition)
        if (newPosition.y < 0 || newPosition.y >= array[newPosition.x].size) return Step(position, newPosition)

        return Step(
            position,
            newPosition,
            checkCombination(
                copyAndRotate(array, position, newPosition),
                newPosition
            )
        )
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

    fun makeShot(input: String, capture: Rectangle) {
        ImageIO.write(Robot().createScreenCapture(capture), "png", File(input))
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