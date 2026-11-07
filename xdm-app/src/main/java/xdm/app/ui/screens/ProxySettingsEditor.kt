package xdm.app.ui.screens

import xdm.core.network.http.DownloadProxyMode
import xdm.core.network.http.DownloadProxySettings
import java.awt.Component
import java.awt.GridLayout
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel

/** Shared editor used when adding or changing a download's network routing. */
object ProxySettingsEditor {
    fun choose(owner: Component?, current: DownloadProxySettings): DownloadProxySettings? {
        val modes = DownloadProxyMode.entries.toTypedArray()
        val mode = JComboBox(modes).apply { selectedItem = current.mode }
        val host = JTextField(current.host, 24)
        val port = JSpinner(SpinnerNumberModel(current.port.coerceIn(1, 65535), 1, 65535, 1))
        val username = JTextField(current.username, 24)
        val password = JPasswordField(current.password, 24)
        val fields = JPanel(GridLayout(0, 2, 10, 8)).apply {
            add(JLabel("Connection mode"))
            add(mode)
            add(JLabel("Proxy host"))
            add(host)
            add(JLabel("Proxy port"))
            add(port)
            add(JLabel("Username (optional)"))
            add(username)
            add(JLabel("Password (stored with local obfuscation)"))
            add(password)
        }
        fun refresh() {
            val explicit = mode.selectedItem == DownloadProxyMode.HTTP || mode.selectedItem == DownloadProxyMode.SOCKS
            host.isEnabled = explicit
            port.isEnabled = explicit
            username.isEnabled = explicit
            password.isEnabled = explicit
        }
        mode.addActionListener { refresh() }
        refresh()
        while (true) {
            if (JOptionPane.showConfirmDialog(owner, fields, "Download Proxy", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return null
            val candidate = runCatching {
                DownloadProxySettings(mode.selectedItem as DownloadProxyMode, host.text.trim(), port.value as Int,
                    username.text.trim(), String(password.password))
            }
            if (candidate.isSuccess) return candidate.getOrThrow()
            JOptionPane.showMessageDialog(owner, candidate.exceptionOrNull()?.message ?: "Invalid proxy settings",
                "Invalid Proxy", JOptionPane.ERROR_MESSAGE)
        }
    }
}
