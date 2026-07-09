import java.util.zip.ZipFile
import java.io.File

fun main(args: Array<String>) {
    val file = File(args[0])
    try {
        ZipFile(file).use {
            println("Valid zip: \${file.name}")
        }
    } catch (e: Exception) {
        println("Invalid zip \${file.name}: \${e.message}")
    }
}
