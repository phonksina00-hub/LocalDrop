
package com.example.sampleapp

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import java.net.ServerSocket
import java.net.Socket
import java.net.Inet4Address
import java.net.NetworkInterface
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private var server: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()
    private lateinit var info: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 32, 24, 32)
            setBackgroundColor(Color.rgb(15, 23, 42))
        }

        val title = TextView(this).apply {
            text = "LOCAL DROP"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        info = TextView(this).apply {
            text = "در حال راه‌اندازی..."
            textSize = 16f
            setTextColor(Color.rgb(134, 239, 172))
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }

        val button = Button(this).apply {
            text = "روشن کردن سرور"
            setOnClickListener { startServer() }
        }

        layout.addView(title)
        layout.addView(info)
        layout.addView(button)
        setContentView(layout)

        startServer()
    }

    private fun getIP(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val addresses =
                    interfaces.nextElement().inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress &&
                        address is Inet4Address
                    ) return address.hostAddress ?: "نامشخص"
                }
            }
        } catch (_: Exception) {}
        return "IP پیدا نشد"
    }

    private fun startServer() {
        if (server != null) return

        executor.execute {
            try {
                server = ServerSocket(8765)
                runOnUiThread {
                    info.text =
                        "سرور روشن است\nhttp://${getIP()}:8765"
                }

                while (true) {
                    val client = server!!.accept()
                    executor.execute { handle(client) }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    info.text = "خطا: ${e.message}"
                }
            }
        }
    }

    private fun handle(client: Socket) {
        try {
            val input = BufferedInputStream(client.getInputStream())
            val output = BufferedOutputStream(client.getOutputStream())

            val request = StringBuilder()
            var previous = -1
            var current: Int

            while (true) {
                current = input.read()
                if (current == -1) break
                request.append(current.toChar())
                if (previous == 13 && current == 10) break
                previous = current
            }

            val body = """
                <!doctype html>
                <html lang="fa">
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <body style="font:20px sans-serif;text-align:center;padding:30px">
                <h2>Local Drop</h2>
                <p>اتصال به سرور برقرار شد!</p>
                <p>این نسخه آزمایشی است؛ انتقال فایل هنوز اضافه نشده.</p>
                </body></html>
            """.trimIndent().toByteArray(Charsets.UTF_8)

            val header = (
                "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Connection: close\r\n\r\n"
            ).toByteArray(Charsets.UTF_8)

            output.write(header)
            output.write(body)
            output.flush()
        } catch (_: Exception) {
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        try { server?.close() } catch (_: Exception) {}
        executor.shutdownNow()
        super.onDestroy()
    }
}

