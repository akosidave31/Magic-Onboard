package com.akosidave.tlstunnel

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.widget.*
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.*

class MainActivity : Activity() {
    private var tls: SSLSocket? = null
    private lateinit var status: TextView
    private lateinit var info: TextView
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        fun field(hint: String, value: String, number: Boolean = false) = EditText(this).apply {
            this.hint = hint
            setText(value)
            inputType = if (number) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
            root.addView(this)
        }
        val server = field("Server", "127.0.0.1")
        val port = field("Port", "4443", number = true)
        val sni = field("SNI host", "performancemanager4.successfactors.com")
        val trustAll = CheckBox(this).apply {
            text = "Accept self-signed cert (testing only)"
            isChecked = true
            root.addView(this)
        }
        button = Button(this).apply { text = "Connect"; root.addView(this) }
        status = TextView(this).apply { text = "Status: Disconnected"; textSize = 18f; root.addView(this) }
        info = TextView(this).apply { typeface = android.graphics.Typeface.MONOSPACE; root.addView(this) }
        setContentView(ScrollView(this).apply { addView(root) })

        button.setOnClickListener {
            if (tls != null) { disconnect(); return@setOnClickListener }
            val host = server.text.toString().trim()
            val p = port.text.toString().toIntOrNull() ?: 443
            val sniHost = sni.text.toString().trim()
            val insecure = trustAll.isChecked
            status.text = "Status: Connecting…"
            button.isEnabled = false
            Thread { connect(host, p, sniHost, insecure) }.start()
        }
    }

    private fun connect(host: String, port: Int, sniHost: String, insecure: Boolean) {
        try {
            val t0 = System.nanoTime()
            val raw = Socket()
            raw.connect(InetSocketAddress(host, port), 10_000)
            val t1 = System.nanoTime()
            val ctx = SSLContext.getInstance("TLS")
            ctx.init(null, if (insecure) arrayOf<TrustManager>(trustAllManager) else null, null)
            val s = ctx.socketFactory.createSocket(raw, host, port, true) as SSLSocket
            if (sniHost.isNotEmpty()) {
                val params = s.sslParameters
                params.serverNames = listOf(SNIHostName(sniHost))
                s.sslParameters = params
            }
            s.startHandshake()
            val t2 = System.nanoTime()
            tls = s
            val text = "TLS version : ${s.session.protocol}\n" +
                "Cipher      : ${s.session.cipherSuite}\n" +
                "Method      : TLS + SNI\n" +
                "TCP connect : ${"%.1f".format((t1 - t0) / 1e6)} ms\n" +
                "Handshake   : ${"%.1f".format((t2 - t1) / 1e6)} ms\n" +
                "Latency     : ${"%.1f".format((t2 - t0) / 1e6)} ms"
            runOnUiThread {
                status.text = "Status: Connected"
                info.text = text
                button.text = "Disconnect"
                button.isEnabled = true
            }
        } catch (e: Exception) {
            runOnUiThread {
                status.text = "Status: Failed"
                info.text = e.toString()
                button.isEnabled = true
            }
        }
    }

    private fun disconnect() {
        val s = tls
        tls = null
        Thread { try { s?.close() } catch (_: Exception) {} }.start()
        status.text = "Status: Disconnected"
        info.text = ""
        button.text = "Connect"
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }

    companion object {
        private val trustAllManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }
}
