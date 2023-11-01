package moe.saru.homebrew.console3ds.mset9_installer_android

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
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
import androidx.documentfile.provider.DocumentFile
import moe.saru.homebrew.console3ds.mset9_installer_android.databinding.ActivityMainBinding
import moe.saru.homebrew.console3ds.mset9_installer_android.enums.Model
import moe.saru.homebrew.console3ds.mset9_installer_android.enums.Stage
import moe.saru.homebrew.console3ds.mset9_installer_android.enums.Version
import java.io.BufferedReader
import java.io.IOException
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    var stage: Stage = Stage.PICK

    var model : Model = Model.NOT_SELECTED_YET
    var version : Version = Version.NOT_SELECTED_YET

    var sdRoot: DocumentFile? = null
    var n3dsFolder: DocumentFile? = null
    var id0Folder: DocumentFile? = null

    var debugOptionEnabled = BuildConfig.ENABLE_DEBUG_OPTION
    var debugVerboseMode = false
    private var debugEnableLastOpen: TimeMark? = null
    private var debugEnableCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

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

                if (!debugOptionEnabled) {
                    if (debugEnableLastOpen?.elapsedNow()?.inWholeMilliseconds?.let { it < 1500 } != true) {
                        debugEnableCount = 0
                    } else if (++debugEnableCount >= 5) {
                        debugOptionEnabled = true
                        Log.d("DebugExtra", "Extra debug option enabled")
                    }
                    debugEnableLastOpen = TimeSource.Monotonic.markNow()
                }

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

    fun showCredits() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.action_credits))
            .setMessage(getString(R.string.credits))
            .setNeutralButton(getString(R.string.alert_neutral)) { _, _ -> }
            .show()
    }

    fun showLog() {
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