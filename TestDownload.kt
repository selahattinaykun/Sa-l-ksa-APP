import java.net.HttpURLConnection
import java.net.URL

fun main() {
    val url = URL("https://raw.githubusercontent.com/selahattinaykun/Sa-l-ksa-APP/main/releases/app-debug.apk")
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "HEAD"
    println("Response code: ${connection.responseCode}")
}
