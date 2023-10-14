package moe.saru.homebrew.console3ds.mset9_installer_android

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.collection.arrayMapOf
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import moe.saru.homebrew.console3ds.mset9_installer_android.databinding.Mset9InstallerBinding
import moe.saru.homebrew.console3ds.mset9_installer_android.enums.Model
import moe.saru.homebrew.console3ds.mset9_installer_android.enums.Stage
import moe.saru.homebrew.console3ds.mset9_installer_android.enums.Version

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class MSET9Installer : Fragment() {

    private var _binding: Mset9InstallerBinding? = null
    private var _mainActivity: MainActivity? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val mainActivity get() = _mainActivity!!

    private val stage get() = mainActivity.stage

    private val version get() = mainActivity.version
    private val model get() = mainActivity.model

    private val sdRoot get() = mainActivity.sdRoot
    private val n3dsFolder get() = mainActivity.n3dsFolder
    private val id0Folder get() = mainActivity.id0Folder

    private var id1Folder: DocumentFile? = null
    private var id1HaxFolder: DocumentFile? = null
    private var id1HaxExtdataFolder: DocumentFile? = null

    private fun canAccessSDRoot(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return true
        }
        activity?.let {
            val appInfo: ApplicationInfo = Utils.getApplicationInfo(it)
            if (appInfo.targetSdkVersion <= Build.VERSION_CODES.Q) {
                return true
            }
        }
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = Mset9InstallerBinding.inflate(inflater, container, false)
        _mainActivity = activity as MainActivity?
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!canAccessSDRoot()) {
            binding.buttonPickFolder.text = getString(R.string.install_pick_3ds)
        }
        bindButtonListeners()

        checkState()
    }

    private fun showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        view?.let {
            Snackbar
                .make(requireView(), message, duration)
                .show()
        }
    }

    private fun showAlert(title: String, message: String, neutral: String? = null, setup: ((AlertDialog.Builder) -> AlertDialog.Builder)? = null) {
        val builder = AlertDialog.Builder(mainActivity)
            .setTitle(title)
            .setMessage(message)
        if (neutral != null) {
            builder.setNeutralButton(neutral) { _, _ -> }
        }
        if (setup != null) {
            setup(builder)
        } else {
            builder.setNeutralButton(getString(R.string.alert_neutral)) { _, _ -> }
        }
        builder.show()
    }

    private fun showLoading(text: String): Dialog {
        val builder: AlertDialog.Builder = AlertDialog.Builder(mainActivity)
        builder.setCancelable(false)
        val dialogView = layoutInflater.inflate(R.layout.loading_dialog, null)
        builder.setView(dialogView)
        dialogView.findViewById<TextView>(R.id.loadingText).text = text

        val dialog = builder.create()
        dialog.show()

        return dialog
    }

    private val pickFolderIntentResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                val uri: Uri = intent.data as Uri
                Log.d("FolderPicking", "Picked Folder URI: ${uri}")
                mainActivity.sdRoot = null
                mainActivity.n3dsFolder = null
                mainActivity.id0Folder = null
                id1Folder = null
                context?.let {
                    val folder = DocumentFile.fromTreeUri(it, uri)
                    if (folder?.isDirectory == true) {
                        folder.name?.let { folderName ->
                            if (folderName.equals("Nintendo 3DS", true)) {
                                Log.d("FolderPicking", "Nintendo 3DS Folder Picked")
                                mainActivity.n3dsFolder = folder
                                pickID0FromN3DS()
                            } else if (checkIfID0(folder)) {
                                Log.d("FolderPicking", "ID0 Folder Picked")
                                mainActivity.id0Folder = folder
                            } else if (checkIfID1(folder)) {
                                Log.e("FolderPicking", "ID1 Folder Picked")
                                showSnackbar(getString(R.string.pick_picked_id1), Snackbar.LENGTH_LONG)
                            } else if (pickN3DSFromSDRoot(folder)) {
                                Log.d("FolderPicking", "SD Root Picked")
                            } else {
                                Log.e("FolderPicking", "Unknown Folder Picked")
                                showSnackbar(getString(R.string.pick_picked_unknown), Snackbar.LENGTH_LONG)
                            }
                            checkState()
                        }
                    } else {
                        Log.e("FolderPicking", "Not even folder!")
                        // not directory
                    }
                }
            }
        }
    }

    private fun pickN3DSFromSDRoot(folder: DocumentFile): Boolean {
        val n3dsFolder = Utils.findFileIgnoreCase(folder,"Nintendo 3DS")
        if (n3dsFolder != null) {
            mainActivity.sdRoot = folder
            mainActivity.n3dsFolder = n3dsFolder
            pickID0FromN3DS()
            return true
        }
        return false
    }

    private fun pickID0FromN3DS(): Boolean {
        return Utils.findJustOneFolder(n3dsFolder, {
            mainActivity.id0Folder = it
            Log.d("FolderPicking", "ID0 Folder Auto Picked - ${id0Folder?.name}")
        }, {
            Log.e("FolderPicking", "0 or more than 1 ID0 found")
            showSnackbar(getString(R.string.pick_id0_not_1))
        }) {
            it.isDirectory && checkIfID0(it)
        }
    }

    private fun checkIfID0(folder: DocumentFile): Boolean {
        folder.name?.let { folderName ->
            if (Utils.id0Regex.matchEntire(folderName) == null) {
                return false
            }
            return folder.listFiles().any { it.isDirectory && checkIfID1(it) }
        }
        return false
    }

    private fun checkIfID1(folder: DocumentFile): Boolean {
        return getHaxID1(folder) != null || Utils.id1Regex.matchEntire(folder.name ?: "") != null
    }

    private fun getHaxID1(folder: DocumentFile): Utils.HaxID1? {
        return Utils.haxID1s.find { it.id1 == folder.name }
    }

    private fun findMatchingHaxID1(folder: DocumentFile): Pair<DocumentFile, Utils.HaxID1>? {
        var ret: Pair<DocumentFile, Utils.HaxID1>? = null
        var haxID1: Utils.HaxID1? = null
        Utils.findJustOneFolder(folder, {
            ret = Pair(it, haxID1!!)
        }, {
            if (it >= 1) {
                Log.e("Prepare", "Multiple Hax ID1 ???")
                showSnackbar(getString(R.string.pick_multi_hax_id1))
                // WTF???
            }
        }) {
            val tmpHaxID1 = getHaxID1(it)
            if (tmpHaxID1 != null) {
                haxID1 = tmpHaxID1
                true
            } else {
                false
            }
        }
        return ret
    }

    private fun findBackupID1(): DocumentFile? {
        var ret: DocumentFile? = null
        Utils.findJustOneFolder(id0Folder, {
            ret = it
        }) {
            checkIfID1(it) && it.name?.endsWith(Utils.OLD_ID1_SUFFIX) == true
        }
        return ret
    }

    private fun findID1(): Boolean {
        return Utils.findJustOneFolder(id0Folder, {
            id1Folder = it
        }) {
            //it.name?.length == 32
            checkIfID1(it) && it.name?.endsWith(Utils.OLD_ID1_SUFFIX) != true
        }
    }

    private fun findHaxFolder(): DocumentFile? {
        if (id0Folder == null) return null
        val hax = Utils.getHax(model, version) ?: return null
        id1HaxFolder = id0Folder!!.findFile(hax.id1)
        return id1HaxFolder
    }

    private fun checkState() {
        if (id0Folder != null) {
            val matching = findMatchingHaxID1(id0Folder!!)
            if (matching != null) {
                id1HaxFolder = matching.first
                mainActivity.model = matching.second.model
                mainActivity.version = matching.second.version
                checkInjectState()
            } else if ((!findID1() && findBackupID1() != null) || (findID1() && findMatchingHaxID1(id0Folder!!) != null)) {
                renderStage(Stage.BROKEN)
            } else if (stage == Stage.SETUP_VARIANT && model != Model.NOT_SELECTED_YET && version != Version.NOT_SELECTED_YET) {
                doSetup()
            } else {
                renderStage(Stage.SETUP)
            }
        } else {
            renderStage(Stage.PICK)
        }
    }

    private fun checkInjectState() {
        if (id1HaxFolder == null) return
        id1HaxExtdataFolder = Utils.findFileIgnoreCase(id1HaxFolder!!,"extdata")
        if (id1HaxExtdataFolder == null) {
            Log.e("Inject", "hax id1 extdata folder is missing")
            showSnackbar(getString(R.string.inject_missing_hax_extdata))
            renderStage(Stage.BROKEN)
            return
        }
        if (Utils.findFileIgnoreCase(id1HaxExtdataFolder!!, Utils.TRIGGER_FILE) == null) {
            renderStage(Stage.INJECT)
        } else {
            renderStage(Stage.TRIGGER)
        }
    }

    private fun renderStage(newStage: Stage? = null) {
        if (newStage != null) {
            mainActivity.stage = newStage
            Log.d("InstallerStage", "switch to ${stage.name}")
        }
        val to: Pair<List<View>, List<View>> = when (stage) {
            Stage.PICK -> Pair(listOf(
                binding.buttonPickFolder,
            ), listOf(
                binding.buttonSetup,
                binding.buttonInjectTrigger,
                binding.buttonRemoveTrigger,
                binding.buttonRemove,
            ))
            Stage.SETUP, Stage.SETUP_VARIANT -> Pair(listOf(
                binding.buttonPickFolder,
                binding.buttonSetup,
            ), listOf(
                binding.buttonInjectTrigger,
                binding.buttonRemoveTrigger,
                binding.buttonRemove,
            ))
            Stage.INJECT -> Pair(listOf(
                binding.buttonInjectTrigger,
                binding.buttonRemove,
            ), listOf(
                binding.buttonPickFolder,
                binding.buttonSetup,
                binding.buttonRemoveTrigger,
            ))
            Stage.TRIGGER -> Pair(listOf(
                binding.buttonRemoveTrigger,
                binding.buttonRemove,
            ), listOf(
                binding.buttonPickFolder,
                binding.buttonSetup,
                binding.buttonInjectTrigger,
            ))
            Stage.BROKEN -> Pair(listOf(
                binding.buttonRemove,
            ), listOf(
                binding.buttonPickFolder,
                binding.buttonSetup,
                binding.buttonInjectTrigger,
                binding.buttonRemoveTrigger,
            ))
            Stage.DOING_WORK -> Pair(listOf(
            ), listOf(
                binding.buttonPickFolder,
                binding.buttonSetup,
                binding.buttonInjectTrigger,
                binding.buttonRemoveTrigger,
                binding.buttonRemove,
            ))
        }
        to.first.forEach { it.isEnabled = true }
        to.second.forEach { it.isEnabled = false }
    }

    private fun bindButtonListeners() {
        binding.buttonPickFolder.setOnClickListener {
            pickFolder()
        }
        binding.buttonSetup.setOnClickListener {
            if (model != Model.NOT_SELECTED_YET && version != Version.NOT_SELECTED_YET) {
                doSetup()
            } else if (!findID1()) {
                Log.e("Setup", "ID1 issue")
            } else if (checkAndCreateDummyDbs() == null) {
                Log.e("Setup", "do dbs folder")
            } else {
                renderStage(Stage.SETUP_VARIANT)
                findNavController().navigate(R.id.action_MSET9Installer_to_ModelSelector)
            }
        }
        binding.buttonInjectTrigger.setOnClickListener {
            Handler(Looper.getMainLooper()).post {
                id1HaxExtdataFolder?.createFile("application/octet-stream", Utils.TRIGGER_FILE)
                checkInjectState()
            }
        }
        binding.buttonRemoveTrigger.setOnClickListener {
            Handler(Looper.getMainLooper()).post {
                Utils.findFileIgnoreCase(id1HaxExtdataFolder, Utils.TRIGGER_FILE)?.delete()
                checkInjectState()
            }
        }
        binding.buttonRemove.setOnClickListener {
            doRemove()
        }
    }

    private fun pickFolder() {
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        pickFolderIntentResult.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
    }

    private fun doSetup() {
        renderStage(Stage.DOING_WORK)
        val loading = showLoading(getString(R.string.setup_loading))
        Handler(Looper.getMainLooper()).post {
            doActualSetup()
            loading.hide()
            checkState()
        }
    }

    private fun doActualSetup() {
        Log.d("Setup", "Setup - ${model.name} ${version.name}")

        val hax = Utils.getHax(model, version)
        if (hax == null) {
            Log.e("Setup", "No applicable hax")
            return
        }
        if (!findID1()) {
            Log.e("Setup", "ID1 Issue")
            return
        }

        doSetupSDRoot()

        getID1Folders() ?: return

        id1Folder!!.renameTo("${id1Folder!!.name!!}${Utils.OLD_ID1_SUFFIX}")
        id1HaxFolder = id0Folder!!.createDirectory(hax.id1)
        if (id1HaxFolder == null) {
            Log.e("Setup", "failed to create hax id1")
            return
        }

        val folders = getID1Folders() ?: return
        val newFolders = arrayMapOf(Pair("", id1HaxFolder!!))

        for (t in folders) {
            if (t.third.isDirectory) {
                val f = newFolders[t.first]?.createDirectory(t.third.name!!)
                if (f == null) {
                    Log.e("Setup", "failed to create hax id1 folder - ${t.second}")
                    return
                }
                newFolders[t.second] = f
            }
            if (t.third.isFile) {
                val f = newFolders[t.first]?.createFile("application/octet-stream", t.third.name!!)
                if (f == null) {
                    Log.e("Setup", "failed to create hax id1 file - ${t.second}")
                    return
                }
                val i = mainActivity.contentResolver.openInputStream(t.third.uri)
                val o = mainActivity.contentResolver.openOutputStream(f.uri)
                if (i == null || o == null) {
                    Log.e("Setup", "failed to open stream - ${t.second}")
                    return
                }
                i.copyTo(o)
                o.close()
                i.close()
            }
        }
    }

    private fun getID1Folders(): List<Triple<String, String, DocumentFile>>? {
        if (id1Folder == null) return null

        val dbs: DocumentFile = checkAndCreateDummyDbs() ?: return null
        val list = arrayListOf(Triple("", "dbs", dbs))
        Utils.findFileIgnoreCase(dbs, "title.db")?.let { list.add(Triple("dbs","dbs/title.db", it)) } ?: return null
        Utils.findFileIgnoreCase(dbs, "import.db")?.let { list.add(Triple("dbs", "dbs/import.db", it)) } ?: return null

        val extdata = id1Folder!!.findFile("extdata")
        if (extdata == null || extdata.name == null) {
            Log.e("Setup", "No extdata folder!")
            return null
        }
        list.add(Triple("", "extdata", extdata))
        val extdata0 = extdata.findFile("00000000")
        if (extdata0 == null || extdata0.name == null) {
            Log.e("Setup", "No extdata 00000000 folder!")
            return null
        }
        list.add(Triple("extdata", "extdata/00000000", extdata0))
        var homeMenuExtdata: DocumentFile? = null
        var miiMakerExtdata: DocumentFile? = null
        for (sub in extdata0.listFiles()) {
            Log.d("Setup", "ext folder ${sub.name}")
            if (homeMenuExtdata != null && miiMakerExtdata != null) break
            if (homeMenuExtdata == null && Utils.homeMenuExtdataList.contains(sub.name?.uppercase())) {
                homeMenuExtdata = sub
                continue
            }
            if (miiMakerExtdata == null && Utils.miiMakerExtdataList.contains(sub.name?.uppercase())) {
                miiMakerExtdata = sub
                continue
            }
        }
        if (homeMenuExtdata == null || homeMenuExtdata.name == null) {
            Log.e("Setup", "No home menu extdata folder!")
            showAlert(getString(R.string.setup_alert_extdata_title), getString(R.string.setup_alert_extdata_home_menu))
            return null
        }
        if (miiMakerExtdata == null || miiMakerExtdata.name == null) {
            Log.e("Setup", "No mii maker extdata folder!")
            showAlert(getString(R.string.setup_alert_extdata_title), getString(R.string.setup_alert_extdata_mii_maker))
            return null
        }
        @Suppress("NAME_SHADOWING")
        for (extdata in listOf(homeMenuExtdata, miiMakerExtdata)) {
            val extdata0 = extdata.findFile("00000000")
            if (extdata0 == null || extdata0.name == null) {
                Log.e("Setup", "No extdata 00000000 folder for ${extdata.name}!")
                return null
            }
            list.add(Triple("extdata/00000000", "extdata/00000000/${extdata.name}", extdata))
            list.add(Triple("extdata/00000000/${extdata.name}", "extdata/00000000/${extdata.name}/00000000", extdata0))
            list.addAll(extdata0.listFiles().map { Triple("extdata/00000000/${extdata.name}/00000000", "extdata/00000000/${extdata.name}/00000000/${it.name}", it) })
        }
        return list
    }

    private fun checkAndCreateDummyDbs(): DocumentFile? {
        val dbs = Utils.findFileIgnoreCase(id1Folder!!,"dbs")
        if (dbs == null) {
            Log.i("Setup", "dbs doesn't exist")
            askIfCreateDummyDbs()
            return null
        }
        if (!dbs.isDirectory) {
            Log.e("Setup", "dbs isn't folder!")
            return null
        }
        val title = Utils.findFileIgnoreCase(dbs, "title.db")
        val import = Utils.findFileIgnoreCase(dbs, "import.db")
        if (title == null || import == null) {
            Log.i("Setup", "db file doesn't exist")
            askIfCreateDummyDbs()
            return null
        }
        if (!title.isFile || !import.isFile) {
            Log.e("Setup", "db files aren't files!")
            return null
        }
        if (title.length() == 0L || import.length() == 0L) {
            Log.e("Setup", "db files are dummy!")
            showAlert(getString(R.string.setup_alert_dummy_db_title), getString(R.string.setup_alert_dummy_db_reset))
            return null
        }
        return dbs
    }

    private fun askIfCreateDummyDbs() {
        val title = getString(R.string.setup_alert_dummy_db_title)
        showAlert(title, "no dbs, create it?") {
            it
                .setNegativeButton("Cancel") { _, _ -> }
                .setPositiveButton("Yes") { _, _ ->
                    if (createDummyDbs()) {
                        Log.i("Setup", "Dummy DB Created")
                        showAlert(title, "${getString(R.string.setup_alert_dummy_db_created)}\n\n${getString(R.string.setup_alert_dummy_db_reset)}")
                    } else {
                        Log.e("Setup", "Fail to create Dummy DB")
                        showAlert(title, getString(R.string.setup_alert_dummy_db_failed))
                    }
                }
        }
    }

    private fun createDummyDbs(): Boolean {
        if (id1Folder == null) return false
        val dbs = Utils.findFileIgnoreCase(id1Folder!!,"dbs") ?: id1Folder!!.createDirectory("dbs")
        if (dbs == null) {
            Log.e("Setup", "can't create dbs folder!")
            return false
        }
        val title = Utils.findFileIgnoreCase(dbs, "title.db") ?: dbs.createFile("application/octet-stream", "title.db")
        if (title == null) {
            Log.e("Setup", "can't create title.db!")
            return false
        }
        val import = Utils.findFileIgnoreCase(dbs, "import.db") ?: dbs.createFile("application/octet-stream", "import.db")
        if (import == null) {
            Log.e("Setup", "can't create title.db!")
            return false
        }
        return true
    }

    private fun doSetupSDRoot() {
        Handler(Looper.getMainLooper()).post {
            if (sdRoot != null) {
            }
        }
    }

    private fun doRemove() {
        renderStage(Stage.DOING_WORK)
        val loading = showLoading(getString(R.string.remove_loading))
        Handler(Looper.getMainLooper()).post {
            Log.d("Setup", "Remove - ${model.name} ${version.name}")

            findHaxFolder()?.delete()

            val backupID1 = findBackupID1()
            if (backupID1 != null) {
                val oriName = backupID1.name!!.removeSuffix(Utils.OLD_ID1_SUFFIX)
                Log.d("Remove", "Rename ${backupID1.name} to ${oriName}")
                backupID1.renameTo(oriName)
            }

            loading.hide()

            mainActivity.model = Model.NOT_SELECTED_YET
            mainActivity.version = Version.NOT_SELECTED_YET
            checkState()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}