package moe.saru.homebrew.console3ds.mset9_installer_android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import moe.saru.homebrew.console3ds.mset9_installer_android.databinding.VersionSelectorBinding
import moe.saru.homebrew.console3ds.mset9_installer_android.enums.Version

class VersionSelector : Fragment() {

    private var _binding: VersionSelectorBinding? = null
    private var _mainActivity: MainActivity? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val mainActivity get() = _mainActivity!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = VersionSelectorBinding.inflate(inflater, container, false)
        _mainActivity = activity as MainActivity?
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.button114117.setOnClickListener {
            mainActivity.version = Version.VER114_117
            gotoMSET9Installer()
        }
        binding.button1181117.setOnClickListener {
            mainActivity.version = Version.VER118_1117
            gotoMSET9Installer()
        }
    }

    private fun gotoMSET9Installer() {
        findNavController().navigate(R.id.action_VersionSelector_to_MSET9Installer)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}