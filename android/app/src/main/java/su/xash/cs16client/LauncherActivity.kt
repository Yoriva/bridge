package su.xash.cs16client

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

abstract class LauncherActivity : Activity() {

    protected abstract val gamedir: String

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val engine = findEngine()
        if (engine == null) {
            Toast.makeText(this, "Xash3D FWGS engine is not installed!", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(ENGINE_URL))
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )
            finish()
            return
        }

        val passedArgv = intent?.getStringExtra("argv")?.takeIf { it.isNotBlank() }
        val effectiveArgv = passedArgv
            ?: prefs.getString(KEY_ARGV, DEFAULT_ARGV)
            ?: DEFAULT_ARGV

        if (passedArgv != null) {
            prefs.edit().putString(KEY_ARGV, passedArgv.trim()).apply()
        }

        launch(engine, buildArgv(effectiveArgv))
        finish()
    }

    private fun launch(enginePackage: String, argv: String) {
        val launchIntent = Intent()
            .setComponent(ComponentName(enginePackage, "su.xash.engine.XashActivity"))
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra("gamedir", gamedir)
            .putExtra("gamelibdir", applicationInfo.nativeLibraryDir)
            .putExtra("argv", argv)
            .putExtra("package", packageName)

        startActivity(launchIntent)
    }

    private fun buildArgv(userArgv: String): String {
        val trimmed = userArgv.trim()
        val hasDll = trimmed.split(Regex("\\s+")).any { it == "-dll" || it.startsWith("-dll=") }
        return when {
            trimmed.isEmpty() -> DLL_ARG
            hasDll -> trimmed
            else -> "$trimmed $DLL_ARG"
        }
    }

    private fun findEngine(): String? = when {
        isInstalled(ENGINE_TEST) -> ENGINE_TEST
        isInstalled(ENGINE_STABLE) -> ENGINE_STABLE
        else -> null
    }

    private fun isInstalled(pkg: String): Boolean = try {
        packageManager.getPackageInfo(pkg, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    companion object {
        private const val PREFS = "cs16_bridge"
        private const val KEY_ARGV = "argv"
        private const val DEFAULT_ARGV = "-dev 2 -log"
        private const val DLL_ARG = "-dll @metadroid"
        private const val ENGINE_TEST = "su.xash.engine.test"
        private const val ENGINE_STABLE = "su.xash.engine"
        private const val ENGINE_URL =
            "https://github.com/FWGS/xash3d-fwgs/releases/tag/continuous"
    }
}
