package moe.saru.homebrew.console3ds.mset9_installer_android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import moe.saru.homebrew.console3ds.mset9_installer_android.databinding.ModelSelectorBinding
import moe.saru.homebrew.console3ds.mset9_installer_android.enums.Model

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class ModelSelector : Fragment() {

    private var _binding: ModelSelectorBinding? = null
    private var _mainActivity: MainActivity? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val mainActivity get() = _mainActivity!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = ModelSelectorBinding.inflate(inflater, container, false)
        _mainActivity = activity as MainActivity?
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val oldModelOnClickListener = View.OnClickListener {
            mainActivity.model = Model.OLD
            gotoVersionSelector()
        }
        val newModelOnClickListener = View.OnClickListener {
            mainActivity.model = Model.NEW
            gotoVersionSelector()
        }
        binding.buttonO3DS.setOnClickListener(oldModelOnClickListener)
        binding.buttonO3DSXL.setOnClickListener(oldModelOnClickListener)
        binding.buttonO2DS.setOnClickListener(oldModelOnClickListener)
        binding.buttonN3DS.setOnClickListener(newModelOnClickListener)
        binding.buttonN3DSXL.setOnClickListener(newModelOnClickListener)
        binding.buttonN2DSXL.setOnClickListener(newModelOnClickListener)
    }

    private fun gotoVersionSelector() {
        findNavController().navigate(R.id.action_ModelSelector_to_VersionSelector)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}