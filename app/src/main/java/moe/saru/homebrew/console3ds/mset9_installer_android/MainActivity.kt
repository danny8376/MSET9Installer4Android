package moe.saru.homebrew.console3ds.mset9_installer_android

import android.app.AlertDialog
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import androidx.documentfile.provider.DocumentFile
import moe.saru.homebrew.console3ds.mset9_installer_android.databinding.ActivityMainBinding
import moe.saru.homebrew.console3ds.mset9_installer_android.enums.Model
import moe.saru.homebrew.console3ds.mset9_installer_android.enums.Stage
import moe.saru.homebrew.console3ds.mset9_installer_android.enums.Version

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    var stage: Stage = Stage.PICK

    var model : Model = Model.NOT_SELECTED_YET
    var version : Version = Version.NOT_SELECTED_YET

    var sdRoot: DocumentFile? = null
    var n3dsFolder: DocumentFile? = null
    var id0Folder: DocumentFile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_credits -> {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.action_credits))
                    .setMessage(getString(R.string.credits))
                    .setNeutralButton(getString(R.string.alert_neutral)) { _, _ -> }
                    .show()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}