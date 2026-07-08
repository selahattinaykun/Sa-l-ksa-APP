import java.util.zip.ZipFile
import java.io.File
fun main() {
    try {
        ZipFile(File("test.apk")).use { println("Valid ZIP") }
    } catch (e: Exception) {
        println("Invalid ZIP: ${e.message}")
    }
}
