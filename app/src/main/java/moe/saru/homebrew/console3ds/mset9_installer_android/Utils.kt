package moe.saru.homebrew.console3ds.mset9_installer_android

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.documentfile.provider.DocumentFile
import moe.saru.homebrew.console3ds.mset9_installer_android.enums.Model
import moe.saru.homebrew.console3ds.mset9_installer_android.enums.Version

class Utils {
    companion object {
        val haxID1s = listOf(
            HaxID1(Model.OLD, Version.VER118_1117, "\uFFFF\uFAFF\u9911\u4807\u4685\u6569\uA108\u2201\u4B05\u4798\u4668\u4659\uAAC0\u1C17\u4643\u4C03\u47A0\u47B8\u9000\u080A\uA071\u0805\uCE99\u0804\u0073\u0064\u006D\u0063\u9000\u080A\u0062\u0039"),
            HaxID1(Model.NEW, Version.VER118_1117, "\uFFFF\uFAFF\u9911\u4807\u4685\u6569\uA108\u2201\u4B05\u4798\u4668\u4659\uAAC0\u1C17\u4643\u4C03\u47A0\u47B8\u9000\u080A\uA071\u0805\uCE5D\u0804\u0073\u0064\u006D\u0063\u9000\u080A\u0062\u0039"),
            HaxID1(Model.OLD, Version.VER114_117, "\uFFFF\uFAFF\u9911\u4807\u4685\u6569\uA108\u2201\u4B05\u4798\u4668\u4659\uAAC0\u1C17\u4643\u4C03\u47A0\u47B8\u9000\u080A\u9E49\u0805\uCC99\u0804\u0073\u0064\u006D\u0063\u9000\u080A\u0062\u0039"),
            HaxID1(Model.NEW, Version.VER114_117, "\uFFFF\uFAFF\u9911\u4807\u4685\u6569\uA108\u2201\u4B05\u4798\u4668\u4659\uAAC0\u1C17\u4643\u4C03\u47A0\u47B8\u9000\u080A\u9E45\u0805\uCC81\u0804\u0073\u0064\u006D\u0063\u9000\u080A\u0062\u0039"),
        )

        fun getHax(model: Model, version: Version): HaxID1? {
            return haxID1s.find { it.model == model && it.version == version }
        }

        val homeMenuExtdataList = listOf("0000008F", "00000098", "00000082", "000000A1", "000000A9", "000000B1") // us,eu,jp,ch,kr,tw
        val miiMakerExtdataList = listOf("00000217", "00000227", "00000207", "00000267", "00000277", "00000287") // us,eu,jp,ch,kr,tw

        const val B9 = "b9"

        const val OLD_ID1_SUFFIX = "_user-id1"
        const val TRIGGER_FILE = "002F003A.txt" // ":/"

        val id0Regex = Regex("(?![0-9a-fA-F]{4}(01|00)[0-9a-fA-F]{18}00[0-9a-fA-F]{6})[0-9a-fA-F]{32}")
        val id1Regex = Regex("[0-9a-fA-F]{32}(?:${OLD_ID1_SUFFIX})?")

        fun findJustOneFolder(parent: DocumentFile?, success: ((DocumentFile) -> Unit)? = null, fail: ((Int) -> Unit)? = null, rule: (DocumentFile) -> Boolean): Boolean {
            if (parent != null) {
                var count = 0
                var candidate: DocumentFile? = null
                for (subFolder in parent.listFiles()) {
                    if (rule(subFolder)) {
                        candidate = subFolder
                        count += 1
                    }
                }
                return if (count == 1) {
                    success?.invoke(candidate!!)
                    true
                } else {
                    fail?.invoke(count)
                    false
                }
            }
            fail?.invoke(-1)
            return false
        }

        fun findFileIgnoreCase(parent: DocumentFile?, name: String): DocumentFile? {
            val target = name.uppercase()
            return parent?.listFiles()?.find { it?.name?.uppercase() == target }
        }

        fun getApplicationInfo(activity: ComponentActivity, packageName: String = activity.packageName, flags: Int = 0): ApplicationInfo {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
            } else {
                activity.packageManager.getApplicationInfo(packageName, flags)
            }
        }
    }

    data class HaxID1(var model: Model, var version: Version, var id1: String)
}