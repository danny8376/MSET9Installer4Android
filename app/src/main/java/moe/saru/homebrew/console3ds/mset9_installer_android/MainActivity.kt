package moe.saru.homebrew.console3ds.mset9_installer_android

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.TextView
import androidx.core.view.MenuCompat
import androidx.core.view.MenuProvider
import androidx.core.view.children
import androidx.documentfile.provider.DocumentFile
import moe.saru.homebrew.console3ds.mset9_installer_android.databinding.ActivityMainBinding
import moe.saru.homebrew.console3ds.mset9_installer_android.enums.Model
import moe.saru.homebrew.console3ds.mset9_installer_android.enums.Stage
import moe.saru.homebrew.console3ds.mset9_installer_android.enums.Version
import java.io.BufferedReader
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    var stage: Stage = Stage.PICK

    var model : Model = Model.NOT_SELECTED_YET
    var version : Version = Version.NOT_SELECTED_YET

    var sdRoot: DocumentFile? = null
    var n3dsFolder: DocumentFile? = null
    var id0Folder: DocumentFile? = null

    var advanceMode = false

    var debugOptionEnabled = BuildConfig.ENABLE_DEBUG_OPTION
    var debugVerboseMode = false
    private var debugEnableCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)

        for (child in toolbar.children) {
            if (child is TextView) {
                child.setOnLongClickListener {
                    if (!debugOptionEnabled) {
                        Log.d("DebugExtra", "Title TextView long clicked, debug enabling attempt")
                        debugEnableCount = 1
                        @SuppressLint("SetTextI18n")
                        child.text = ">>>>>>>> 5 <<<<<<<<"
                        Handler(Looper.getMainLooper()).postDelayed({
                            debugEnableCount = 0
                            child.text = toolbar.title
                        }, 3000)
                    }
                    true
                }
                child.setOnClickListener {
                    if (debugEnableCount != 0) {
                        Log.d("TEST", "Title TextView clicked after long pressed, count: $debugEnableCount")
                        if (++debugEnableCount > 5) {
                            Log.d("DebugExtra", "Extra debug option enabled")
                            debugOptionEnabled = true
                            debugEnableCount = 0
                            child.text = toolbar.title
                        } else {
                            val remaining = 6 - debugEnableCount
                            @SuppressLint("SetTextI18n")
                            child.text = ">>>>>>>> $remaining <<<<<<<<"
                        }
                    }
                }
            }
        }

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Inflate the menu; this adds items to the action bar if it is present.
                menuInflater.inflate(R.menu.menu_main, menu)
                MenuCompat.setGroupDividerEnabled(menu, true)
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)

                menu.setGroupVisible(R.id.action_debug_extra, debugOptionEnabled)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                // Handle action bar item clicks here. The action bar will
                // automatically handle clicks on the Home/Up button, so long
                // as you specify a parent activity in AndroidManifest.xml.
                return when (item.itemId) {
                    R.id.action_credits -> {
                        showCredits()
                        true
                    }
                    R.id.action_advance -> {
                        item.isChecked = !item.isChecked
                        advanceMode = item.isChecked
                        notifyFragmentAboutOptionChanged()
                        true
                    }
                    R.id.action_verbose -> {
                        item.isChecked = !item.isChecked
                        debugVerboseMode = item.isChecked
                        true
                    }
                    R.id.action_log -> {
                        showLog()
                        true
                    }
                    else -> true
                }
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private fun notifyFragmentAboutOptionChanged() {
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.primaryNavigationFragment?.let {
            val mset9installer = it as MSET9Installer
            mset9installer.onOptionChanged()
        }
    }

    private fun showCredits() {
        val view = TextView(this)
        val padding = (30 * resources.displayMetrics.density).toInt()
        view.setPadding(padding, padding / 2, padding, 0)

        val verName = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).versionName
        val verStr = "${getString(R.string.app_name)} v${verName}"
        val credits = getString(R.string.credits, verStr)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            view.text = Html.fromHtml(credits, Html.FROM_HTML_MODE_COMPACT)
            view.linksClickable = true
            view.movementMethod = LinkMovementMethod.getInstance()
        } else {
            @Suppress("DEPRECATION")
            view.text = Html.fromHtml(credits)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.action_credits))
            .setView(view)
            .setPositiveButton(getString(R.string.alert_neutral)) { _, _ -> }
            .show()
    }

    private fun showLog() {
        try {
            val process = Runtime.getRuntime().exec("logcat -d")
            val log = process.inputStream.bufferedReader().use(BufferedReader::readText)

            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.log_dialog, null)
            builder.setView(dialogView)
            builder.setPositiveButton(getString(R.string.alert_neutral)) { _, _ -> }
            builder.setNeutralButton(getString(R.string.log_share)) { _, _ ->
                val intent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, log)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(intent, null))
            }
            dialogView.findViewById<TextView>(R.id.log).text = log

            val dialog = builder.create()
            dialog.show()
        } catch (_: IOException) {
        }
    }
}