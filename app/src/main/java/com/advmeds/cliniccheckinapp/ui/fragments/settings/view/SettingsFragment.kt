package com.advmeds.cliniccheckinapp.ui.fragments.settings.view

import android.app.ActionBar
import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isGone
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.ListFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.advmeds.cliniccheckinapp.BuildConfig
import com.advmeds.cliniccheckinapp.R
import com.advmeds.cliniccheckinapp.databinding.SettingsFragmentBinding
import com.advmeds.cliniccheckinapp.dialog.EditCheckInItemDialog
import com.advmeds.cliniccheckinapp.models.remote.mScheduler.request.CreateAppointmentRequest
import com.advmeds.cliniccheckinapp.models.remote.mScheduler.sharedPreferences.AutomaticAppointmentMode
import com.advmeds.cliniccheckinapp.models.remote.mScheduler.sharedPreferences.AutomaticAppointmentSettingModel
import com.advmeds.cliniccheckinapp.models.remote.mScheduler.sharedPreferences.QueueingMachineSettingModel
import com.advmeds.cliniccheckinapp.models.remote.mScheduler.sharedPreferences.QueuingBoardSettingModel
import com.advmeds.cliniccheckinapp.repositories.AnalyticsRepositoryImpl
import com.advmeds.cliniccheckinapp.repositories.DownloadControllerRepository
import com.advmeds.cliniccheckinapp.repositories.RoomRepositories
import com.advmeds.cliniccheckinapp.repositories.SharedPreferencesRepo
import com.advmeds.cliniccheckinapp.ui.MainActivity
import com.advmeds.cliniccheckinapp.ui.fragments.settings.adapter.LanguageAdapter
import com.advmeds.cliniccheckinapp.ui.fragments.settings.adapter.SettingsAdapter
import com.advmeds.cliniccheckinapp.ui.fragments.settings.eventLogger.SettingsEventLogger
import com.advmeds.cliniccheckinapp.ui.fragments.settings.model.LanguageModel
import com.advmeds.cliniccheckinapp.ui.fragments.settings.model.combineArrays
import com.advmeds.cliniccheckinapp.ui.fragments.settings.viewModel.SettingsViewModel
import com.advmeds.cliniccheckinapp.ui.fragments.settings.viewModel.SettingsViewModelFactory
import com.advmeds.cliniccheckinapp.ui.fragments.settings.viewModel.UpdateSoftwareDownloadingStatus
import com.advmeds.cliniccheckinapp.ui.fragments.settings.viewModel.UpdateSoftwareRequestStatus
import com.advmeds.cliniccheckinapp.utils.Converter
import com.advmeds.cliniccheckinapp.utils.DownloadController
import com.advmeds.cliniccheckinapp.utils.showOnly
import com.google.android.material.checkbox.MaterialCheckBox
import kotlinx.android.synthetic.main.automatic_appointment_setting_dialog.*
import kotlinx.android.synthetic.main.format_checked_list.*
import kotlinx.android.synthetic.main.language_setting_dialog.*
import kotlinx.android.synthetic.main.queueing_board_setting_dialog.*
import kotlinx.android.synthetic.main.queueing_machine_setting_dialog.*
import kotlinx.android.synthetic.main.software_update_dialog.*
import kotlinx.android.synthetic.main.text_input_dialog.*
import kotlinx.android.synthetic.main.ui_setting_dialog.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.HttpUrl


class SettingsFragment : ListFragment() {

    private lateinit var viewModel: SettingsViewModel

    private var _binding: SettingsFragmentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var toolbar: Toolbar


    private lateinit var dialog: Dialog

    var stateFlowJob: Job? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val downloadController = DownloadController(requireContext().applicationContext)
        val downloadControllerRepository = DownloadControllerRepository(downloadController)

        val viewModelFactory = SettingsViewModelFactory(
            requireContext().applicationContext as Application,
            downloadControllerRepository,
            SettingsEventLogger(
                AnalyticsRepositoryImpl.getInstance(
                    RoomRepositories.eventsRepository,
                    SharedPreferencesRepo.getInstance(requireContext())
                )
            )
        )

        viewModel = ViewModelProvider(this, viewModelFactory)[SettingsViewModel::class.java]

        _binding = SettingsFragmentBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        toolbar = binding.toolbar
        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        toolbar.title = resources.getString(R.string.setting)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val adapter = SettingsAdapter(
            mContext = requireContext(),
            settingItems = resources.getStringArray(R.array.setting_items)
        )

        listAdapter = adapter
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)

        when (position) {
            0 -> onSetUiSettingsItemClicked()
            1 -> onSetServerDomainItemClicked()
            2 -> onSetOrgIDItemClicked()
            3 -> onSetDoctorsItemClicked()
            4 -> onSetRoomsItemClicked()
            5 -> onSetDeptIDItemClicked()
            6 -> onSetQueueingBoardSettingItemClicked()
            7 -> onSetQueueingMachineSettingItemClicked()
            8 -> onSetFormatCheckedListItemClicked()
            9 -> onSetAutomaticAppointmentSettingItemClicked()
            10 -> onSetLanguageSettingItemClicked()
            11 -> onSetSoftwareUpdateSettingItemClicked()
            12 -> onSetOpenSystemSettingsItemClicked()
            14 -> onSetExitItemClicked()
        }
    }


    private fun onSetUiSettingsItemClicked() {

        var slotsCount = 0
        dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.ui_setting_dialog)

        if (dialog.window == null)
            return

        dialog.window!!.setGravity(Gravity.CENTER)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))


        // set values
        dialog.ui_settings_dialog_input_field.editText?.setText(viewModel.machineTitle.ifBlank {
            getString(
                R.string.app_name
            )
        })

        val showInsertNHICardAnimation = viewModel.showInsertNHICardAnimation
        dialog.ui_settings_insert_nhi_card_animation.isChecked = showInsertNHICardAnimation

        val checkInItemsList = viewModel.checkInItemList
        slotsCount = checkInItemsList.count { it.isShow }
        val checkInItems = EditCheckInItemDialog.toObject(checkInItemsList)

        setUpForUISettings(dialog = dialog, checkInItems = checkInItems)

        val turnOnOrOffCheckBox: () -> Unit = {

            val listOfCheckBox = listOf(
                dialog.ui_settings_manual_input,
                dialog.ui_settings_virtual_nhi_card,
                dialog.ui_settings_customized_one,
                dialog.ui_settings_customized_two,
                dialog.ui_settings_customized_three,
                dialog.ui_settings_customized_four,
            )

            val listOfCheckBoxTextView = listOf(
                dialog.ui_settings_manual_input_text_view,
                dialog.ui_settings_virtual_nhi_card_text_view,
                dialog.ui_settings_customized_one_text_view,
                dialog.ui_settings_customized_two_text_view,
                dialog.ui_settings_customized_three_text_view,
                dialog.ui_settings_customized_four_text_view,
            )

            require(listOfCheckBox.size == listOfCheckBoxTextView.size) {
                "List must have the same size"
            }

            if (slotsCount == 4) {
                disableUnselectedCheckboxesUISettings(
                    listOfCheckBox = listOfCheckBox,
                    listOfCheckBoxTextView = listOfCheckBoxTextView
                )
            }
            if (slotsCount == 3) {
                enableAllCheckBoxesUISettings(
                    listOfCheckBox = listOfCheckBox,
                    listOfCheckBoxTextView = listOfCheckBoxTextView
                )
            }
        }


        // set check listeners
        dialog.ui_settings_manual_input.setOnCheckedChangeListener { _, isChecked ->
            checkInItems.manualInput.isShow = isChecked
            if (isChecked) slotsCount++ else slotsCount--
            turnOnOrOffCheckBox()
        }

        dialog.ui_settings_virtual_nhi_card.setOnCheckedChangeListener { _, isChecked ->
            checkInItems.virtualCard.isShow = isChecked
            if (isChecked) slotsCount++ else slotsCount--
            turnOnOrOffCheckBox()
        }

        dialog.ui_settings_customized_one.setOnCheckedChangeListener { _, isChecked ->
            checkInItems.customOne.isShow = isChecked
            dialog.ui_settings_customized_one_container.isGone = !isChecked
            if (isChecked) slotsCount++ else slotsCount--
            turnOnOrOffCheckBox()
        }

        dialog.ui_settings_customized_two.setOnCheckedChangeListener { _, isChecked ->
            checkInItems.customTwo.isShow = isChecked
            dialog.ui_settings_customized_two_container.isGone = !isChecked
            if (isChecked) slotsCount++ else slotsCount--
            turnOnOrOffCheckBox()
        }

        dialog.ui_settings_customized_three.setOnCheckedChangeListener { _, isChecked ->
            checkInItems.customThree.isShow = isChecked
            dialog.ui_settings_customized_three_container.isGone = !isChecked
            if (isChecked) slotsCount++ else slotsCount--
            turnOnOrOffCheckBox()
        }

        dialog.ui_settings_customized_four.setOnCheckedChangeListener { _, isChecked ->
            checkInItems.customFour.isShow = isChecked
            dialog.ui_settings_customized_four_container.isGone = !isChecked
            if (isChecked) slotsCount++ else slotsCount--
            turnOnOrOffCheckBox()
        }

        // buttons click listeners

        dialog.ui_settings_save_btn.setOnClickListener {

            val checkInItemsForSave =
                prepareCustomCheckInItemsForSaving(dialog = dialog, checkInItems = checkInItems)

            val newMachineTitle =
                dialog.ui_settings_dialog_input_field.editText?.text.toString().trim()

            val newShowInsertNHICardAnimation =
                dialog.ui_settings_insert_nhi_card_animation.isChecked

            viewModel.userChangeUiSetting(
                settingItemTitle = "UI Setting",
                originalMachineTitle = viewModel.machineTitle,
                originalValue = EditCheckInItemDialog.toObject(viewModel.checkInItemList),
                originalValueIsShowInsertNHICardAnimation = showInsertNHICardAnimation,
                changeMachineTitle = newMachineTitle,
                changeValue = checkInItemsForSave,
                changeIsShowInsertNHICardAnimation = newShowInsertNHICardAnimation
            )


            viewModel.machineTitle = newMachineTitle

            viewModel.checkInItemList = EditCheckInItemDialog.toList(checkInItemsForSave)

            viewModel.showInsertNHICardAnimation = newShowInsertNHICardAnimation

            dialog.dismiss()
        }

        dialog.ui_settings_cancel_btn.setOnClickListener {
            dialog.dismiss()
        }

        turnOnOrOffCheckBox()
        dialog.show()
    }

    private fun enableAllCheckBoxesUISettings(
        listOfCheckBox: List<MaterialCheckBox>,
        listOfCheckBoxTextView: List<TextView>
    ) {
        listOfCheckBox.zip(listOfCheckBoxTextView).forEach { (checkBox, textView) ->
            checkBox.isEnabled = true
            textView.setTextColor(Color.BLACK)
        }
    }

    private fun disableUnselectedCheckboxesUISettings(
        listOfCheckBox: List<MaterialCheckBox>,
        listOfCheckBoxTextView: List<TextView>
    ) {
        listOfCheckBox.zip(listOfCheckBoxTextView).forEach { (checkBox, textView) ->
            if (!checkBox.isChecked) {
                checkBox.isEnabled = false
                textView.setTextColor(Color.GRAY)
            }
        }
    }


    private fun setUpForUISettings(
        dialog: Dialog,
        checkInItems: EditCheckInItemDialog.EditCheckInItems
    ) {
        dialog.ui_settings_manual_input.isChecked = checkInItems.manualInput.isShow

        dialog.ui_settings_virtual_nhi_card.isChecked = checkInItems.virtualCard.isShow

        dialog.ui_settings_customized_one.isChecked = checkInItems.customOne.isShow
        dialog.ui_settings_customized_one_container.isGone = !checkInItems.customOne.isShow
        dialog.ui_settings_customized_one_block_name.editText?.setText(checkInItems.customOne.title)
        dialog.ui_settings_customized_one_action.editText?.setText(checkInItems.customOne.action)
        dialog.ui_settings_customized_one_doctor_id.editText?.setText(checkInItems.customOne.doctorId)
        dialog.ui_settings_customized_one_room_id.editText?.setText(checkInItems.customOne.divisionId)

        dialog.ui_settings_customized_two.isChecked = checkInItems.customTwo.isShow
        dialog.ui_settings_customized_two_container.isGone = !checkInItems.customTwo.isShow
        dialog.ui_settings_customized_two_block_name.editText?.setText(checkInItems.customTwo.title)
        dialog.ui_settings_customized_two_action.editText?.setText(checkInItems.customTwo.action)
        dialog.ui_settings_customized_two_doctor_id.editText?.setText(checkInItems.customTwo.doctorId)
        dialog.ui_settings_customized_two_room_id.editText?.setText(checkInItems.customTwo.divisionId)

        dialog.ui_settings_customized_three.isChecked = checkInItems.customThree.isShow
        dialog.ui_settings_customized_three_container.isGone = !checkInItems.customThree.isShow
        dialog.ui_settings_customized_three_block_name.editText?.setText(checkInItems.customThree.title)
        dialog.ui_settings_customized_three_action.editText?.setText(checkInItems.customThree.action)
        dialog.ui_settings_customized_three_doctor_id.editText?.setText(checkInItems.customThree.doctorId)
        dialog.ui_settings_customized_three_room_id.editText?.setText(checkInItems.customThree.divisionId)

        dialog.ui_settings_customized_four.isChecked = checkInItems.customFour.isShow
        dialog.ui_settings_customized_four_container.isGone = !checkInItems.customFour.isShow
        dialog.ui_settings_customized_four_block_name.editText?.setText(checkInItems.customFour.title)
        dialog.ui_settings_customized_four_action.editText?.setText(checkInItems.customFour.action)
        dialog.ui_settings_customized_four_doctor_id.editText?.setText(checkInItems.customFour.doctorId)
        dialog.ui_settings_customized_four_room_id.editText?.setText(checkInItems.customFour.divisionId)
    }

    private fun prepareCustomCheckInItemsForSaving(
        dialog: Dialog,
        checkInItems: EditCheckInItemDialog.EditCheckInItems
    ): EditCheckInItemDialog.EditCheckInItems {
        if (dialog.ui_settings_customized_one.isChecked)
            with(checkInItems.customOne) {
                title =
                    dialog.ui_settings_customized_one_block_name.editText?.text.toString().trim()
                action =
                    dialog.ui_settings_customized_one_action.editText?.text.toString().trim()
                doctorId =
                    dialog.ui_settings_customized_one_doctor_id.editText?.text.toString().trim()
                divisionId =
                    dialog.ui_settings_customized_one_room_id.editText?.text.toString().trim()
            }
        else
            with(checkInItems.customOne) {
                title = ""
                action = ""
                doctorId = ""
                divisionId = ""
            }

        if (dialog.ui_settings_customized_two.isChecked)
            with(checkInItems.customTwo) {
                title =
                    dialog.ui_settings_customized_two_block_name.editText?.text.toString().trim()
                action =
                    dialog.ui_settings_customized_two_action.editText?.text.toString().trim()
                doctorId =
                    dialog.ui_settings_customized_two_doctor_id.editText?.text.toString().trim()
                divisionId =
                    dialog.ui_settings_customized_two_room_id.editText?.text.toString().trim()
            }
        else
            with(checkInItems.customTwo) {
                title = ""
                action = ""
                doctorId = ""
                divisionId = ""
            }

        if (dialog.ui_settings_customized_three.isChecked)
            with(checkInItems.customThree) {
                title =
                    dialog.ui_settings_customized_three_block_name.editText?.text.toString().trim()
                action =
                    dialog.ui_settings_customized_three_action.editText?.text.toString().trim()
                doctorId =
                    dialog.ui_settings_customized_three_doctor_id.editText?.text.toString().trim()
                divisionId =
                    dialog.ui_settings_customized_three_room_id.editText?.text.toString().trim()
            }
        else
            with(checkInItems.customThree) {
                title = ""
                action = ""
                doctorId = ""
                divisionId = ""
            }

        if (dialog.ui_settings_customized_four.isChecked)
            with(checkInItems.customFour) {
                title =
                    dialog.ui_settings_customized_four_block_name.editText?.text.toString().trim()
                action =
                    dialog.ui_settings_customized_four_action.editText?.text.toString().trim()
                doctorId =
                    dialog.ui_settings_customized_four_doctor_id.editText?.text.toString().trim()
                divisionId =
                    dialog.ui_settings_customized_four_room_id.editText?.text.toString().trim()
            }
        else
            with(checkInItems.customFour) {
                title = ""
                action = ""
                doctorId = ""
                divisionId = ""
            }

        return checkInItems
    }

    private fun onSetServerDomainItemClicked() {

        val hint = requireContext().getString(R.string.customize_url_hint)
        val inputTextLabel = requireContext().getString(R.string.dialog_url_label)

        showTextInputDialog(
            titleResId = R.string.clinic_panel_url,
            inputText = viewModel.mSchedulerServerDomain.first,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
            inputTextLabel = inputTextLabel,
            hint = hint,
            selectedRadio = viewModel.mSchedulerServerDomain.second,
            showRadioButton = true,
            onDomainConfirmClick = { domain, selected ->
                try {
                    HttpUrl.get(domain)

                    viewModel.userChangeDomainSetting(
                        settingItemTitle = "Domain Setting",
                        originalUrl = viewModel.mSchedulerServerDomain.first,
                        originalSelect = viewModel.mSchedulerServerDomain.second,
                        newUrl = domain,
                        newSelect = selected
                    )

                    viewModel.mSchedulerServerDomain = Pair(domain, selected)
                } catch (e: Exception) {
                    AlertDialog.Builder(requireContext())
                        .setMessage(e.message)
                        .setPositiveButton(R.string.confirm, null)
                        .showOnly()
                }
            }
        )
    }

    private fun onSetOrgIDItemClicked() {

        val inputTextLabel = requireContext().getString(R.string.dialog_id_label)

        showTextInputDialog(
            titleResId = R.string.org_id,
            inputTextLabel = inputTextLabel,
            inputText = viewModel.orgId,
            onConfirmClick = { id ->
                if (id.isNotBlank()) {
                    viewModel.userChangeSettingItem(
                        settingItemTitle = "Clinic ID",
                        originalValue = viewModel.orgId,
                        newValue = id
                    )
                    viewModel.orgId = id
                }
            })
    }

    private fun onSetDoctorsItemClicked() {

        val inputTextLabel = requireContext().getString(R.string.dialog_id_label)

        showTextInputDialog(
            titleResId = R.string.doctors,
            inputTextLabel = inputTextLabel,
            showDescription = true,
            inputText = viewModel.doctors.joinToString(","),
            onConfirmClick = { doctors ->
                viewModel.userChangeSettingItem(
                    settingItemTitle = "Doctor ID",
                    originalValue = viewModel.doctors.joinToString(","),
                    newValue = doctors
                )
                viewModel.doctors = doctors.split(",").filter { it.isNotBlank() }.toSet()
            }
        )
    }

    private fun onSetRoomsItemClicked() {

        val inputTextLabel = requireContext().getString(R.string.dialog_id_label)

        showTextInputDialog(
            titleResId = R.string.rooms,
            inputTextLabel = inputTextLabel,
            inputText = viewModel.rooms.joinToString(","),
            onConfirmClick = { rooms ->
                viewModel.userChangeSettingItem(
                    settingItemTitle = "Division ID",
                    originalValue = viewModel.rooms.joinToString(","),
                    newValue = rooms
                )
                viewModel.rooms = rooms.split(",").filter { it.isNotBlank() }.toSet()
            })
    }

    private fun onSetFormatCheckedListItemClicked() {
        val choiceItems = CreateAppointmentRequest.NationalIdFormat.values()
        val checkedItems = choiceItems.map { viewModel.formatCheckedList.contains(it) }

        dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.format_checked_list)

        if (dialog.window == null)
            return

        dialog.window!!.setGravity(Gravity.CENTER)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.fcl_title.setText(R.string.format_checked)

        setListOfCheckBox(
            container = dialog.fcl_check_box_container,
            names = choiceItems,
            checks = checkedItems
        )

        dialog.fcl_save_btn.setOnClickListener {
            if (viewModel._tempFormatCheckedList == viewModel.formatCheckedList) {
                dialog.dismiss()
                return@setOnClickListener
            }

            viewModel.userChangeCheckedListSetting(
                settingItemTitle = "Format Check in Setting",
                originalValue = viewModel.formatCheckedList,
                newValue = viewModel._tempFormatCheckedList
            )

            viewModel.formatCheckedList = viewModel._tempFormatCheckedList

            dialog.dismiss()
        }

        dialog.fcl_cancel_btn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            viewModel.clearTempFormatCheckedList()
        }

        dialog.show()
    }

    private fun onSetDeptIDItemClicked() {

        val inputTextLabel = requireContext().getString(R.string.dialog_id_label)

        showTextInputDialog(
            titleResId = R.string.dept_id,
            inputTextLabel = inputTextLabel,
            inputText = viewModel.deptId.joinToString(","),
            showDescription = true,
            onConfirmClick = { id ->
                viewModel.userChangeSettingItem(
                    settingItemTitle = "Room ID",
                    originalValue = viewModel.deptId.joinToString(","),
                    newValue = id
                )
                if (id.isNotBlank()) {
                    viewModel.deptId = id.split(",").filter { it.isNotBlank() }.toSet()
                }
            }
        )
    }

    private fun showTextInputDialog(
        titleResId: Int,
        inputTextLabel: String,
        inputText: String = "",
        hint: String = "",
        inputType: Int = InputType.TYPE_CLASS_TEXT,
        selectedRadio: Int = 0,
        showRadioButton: Boolean = false,
        showDescription: Boolean = false,
        onConfirmClick: (String) -> Unit = {},
        onDomainConfirmClick: ((String, Int) -> Unit)? = null
    ) {

        dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.text_input_dialog)

        if (dialog.window == null)
            return

        dialog.window!!.setGravity(Gravity.CENTER)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.dialog_title.setText(titleResId)
        dialog.dialog_input_field.editText?.setText(inputText)
        dialog.dialog_input_field.placeholderText = hint
        dialog.dialog_input_field.hint = inputTextLabel
        dialog.dialog_input_field.editText?.inputType = inputType

        if (showDescription) {
            dialog.dialog_description.visibility = View.VISIBLE
        }

        if (showRadioButton) {
            val urlContainer = dialog.dialog_input_container

            dialog.dialog_radio_group.visibility = View.VISIBLE
            urlContainer.isGone = selectedRadio != 2

            when (selectedRadio) {
                0 -> dialog.dialog_radio_group.check(R.id.domain_service_official_site)
                1 -> dialog.dialog_radio_group.check(R.id.domain_service_testing_site)
                2 -> dialog.dialog_radio_group.check(R.id.domain_service_customize)
            }

            dialog.dialog_radio_group.setOnCheckedChangeListener(
                RadioGroup.OnCheckedChangeListener { _, checkedId ->
                    when (checkedId) {
                        R.id.domain_service_official_site -> urlContainer.visibility = View.GONE
                        R.id.domain_service_testing_site -> urlContainer.visibility = View.GONE
                        R.id.domain_service_customize -> urlContainer.visibility = View.VISIBLE
                    }
                }
            )
        }

        dialog.dialog_cancel_btn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.dialog_save_btn.setOnClickListener {

            var inputData = dialog.dialog_input_field.editText?.text.toString().trim()

            if (showRadioButton) {

                var selectedRadio = 0

                when (dialog.dialog_radio_group.checkedRadioButtonId) {
                    R.id.domain_service_official_site -> {
                        inputData = "https://www.mscheduler.com"
                        selectedRadio = 0
                    }
                    R.id.domain_service_testing_site -> {
                        inputData = "https://test.mscheduler.com"
                        selectedRadio = 1
                    }
                    R.id.domain_service_customize -> {
                        inputData = dialog.dialog_input_field.editText?.text.toString().trim()
                        selectedRadio = 2
                    }
                }

                onDomainConfirmClick?.invoke(inputData, selectedRadio)
            }

            onConfirmClick(inputData)

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun onSetQueueingBoardSettingItemClicked() {

        val label = resources.getString(R.string.qbs_screen_edit_text_label)

        dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.queueing_board_setting_dialog)

        if (dialog.window == null)
            return

        dialog.window!!.setGravity(Gravity.CENTER)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val queuingBoardSettingModel = viewModel.queueingBoardSettings

        dialog.queueing_board_setting_switcher.isChecked = queuingBoardSettingModel.isEnabled
        dialog.queueing_board_setting_container.isGone = !queuingBoardSettingModel.isEnabled

        dialog.et_qbs_irl_input.hint = label
        dialog.et_qbs_irl_input.editText?.setText(queuingBoardSettingModel.url)

        dialog.queueing_board_setting_switcher.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                dialog.queueing_board_setting_container.visibility = View.VISIBLE
            else
                dialog.queueing_board_setting_container.visibility = View.GONE
        }

        val saveButton = dialog.btn_qbs_dialog_save
        val cancelButton = dialog.btn_qbs_dialog_cancel

        saveButton.setOnClickListener {
            val qbsIsEnable = dialog.queueing_board_setting_switcher.isChecked
            val qbsDomain = dialog.et_qbs_irl_input.editText?.text.toString().trim()

            try {
                if (qbsIsEnable) {
                    HttpUrl.get(qbsDomain)
                }

                val queueingMachineSettingModelForSave = QueuingBoardSettingModel(
                    isEnabled = qbsIsEnable,
                    url = if (qbsIsEnable) qbsDomain else ""
                )

                viewModel.userChangeQueueingBoardSetting(
                    settingItemTitle = "Queueing Board Setting",
                    originalValue = viewModel.queueingBoardSettings,
                    newValue = queueingMachineSettingModelForSave
                )

                viewModel.queueingBoardSettings = queueingMachineSettingModelForSave
            } catch (e: Exception) {
                viewModel.queueingBoardSettings = QueuingBoardSettingModel(
                    isEnabled = qbsIsEnable,
                    url = queuingBoardSettingModel.url
                )

                AlertDialog.Builder(requireContext())
                    .setMessage(e.message)
                    .setPositiveButton(R.string.confirm, null)
                    .showOnly()
            }

            dialog.dismiss()

        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun onSetQueueingMachineSettingItemClicked() {

        dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.queueing_machine_setting_dialog)

        if (dialog.window == null)
            return

        dialog.window!!.setGravity(Gravity.CENTER)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val queueingMachineSettingModel = viewModel.queueingMachineSettings

        dialog.queueing_machine_setting_switcher.isChecked = queueingMachineSettingModel.isEnabled
        dialog.queueing_machine_setting_container.isGone = !queueingMachineSettingModel.isEnabled

        dialog.qms_cb_organization.isChecked = queueingMachineSettingModel.organization
        dialog.qms_cb_doctor.isChecked = queueingMachineSettingModel.doctor
        dialog.qms_cb_dept.isChecked = queueingMachineSettingModel.dept
        dialog.qms_cb_time.isChecked = queueingMachineSettingModel.time

        dialog.queueing_machine_setting_one_ticket_switcher.isChecked =
            queueingMachineSettingModel.isOneTicket

        dialog.queueing_machine_setting_switcher.setOnCheckedChangeListener { _, isChecked ->
            dialog.queueing_machine_setting_container.isGone = !isChecked
        }

        when (queueingMachineSettingModel.textSize) {
            QueueingMachineSettingModel.MillimeterSize.FIFTY_SEVEN_MILLIMETERS ->
                dialog.qms_dialog_radio_group.check(R.id.qms_print_size_57)
            QueueingMachineSettingModel.MillimeterSize.SEVENTY_SIX_MILLIMETERS ->
                dialog.qms_dialog_radio_group.check(R.id.qms_print_size_76)

        }

        val saveButton = dialog.btn_qms_dialog_save
        val cancelButton = dialog.btn_qms_dialog_cancel

        saveButton.setOnClickListener {

            val queueingMachineSettingModelForSave = prepareQueueingMachineSettingForSaving(dialog)

            if (queueingMachineSettingModelForSave.isSame(viewModel.queueingMachineSettings))
                return@setOnClickListener

            viewModel.userChangeQueueingMachineSetting(
                settingItemTitle = "Queueing Machine Setting",
                originalValue = viewModel.queueingMachineSettings,
                newValue = queueingMachineSettingModelForSave
            )

            viewModel.queueingMachineSettings = queueingMachineSettingModelForSave
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

    }

    private fun prepareQueueingMachineSettingForSaving(dialog: Dialog): QueueingMachineSettingModel {

        val isEnable: Boolean = dialog.queueing_machine_setting_switcher.isChecked
        val organization: Boolean = dialog.qms_cb_organization.isChecked
        val doctor: Boolean = dialog.qms_cb_doctor.isChecked
        val dept: Boolean = dialog.qms_cb_dept.isChecked
        val time: Boolean = dialog.qms_cb_time.isChecked
        val isOneTicket: Boolean = dialog.queueing_machine_setting_one_ticket_switcher.isChecked
        val textSize: QueueingMachineSettingModel.MillimeterSize =
            when (dialog.qms_dialog_radio_group.checkedRadioButtonId) {
                R.id.qms_print_size_57 -> QueueingMachineSettingModel.MillimeterSize.FIFTY_SEVEN_MILLIMETERS
                R.id.qms_print_size_76 -> QueueingMachineSettingModel.MillimeterSize.SEVENTY_SIX_MILLIMETERS
                else -> QueueingMachineSettingModel.MillimeterSize.FIFTY_SEVEN_MILLIMETERS
            }

        dialog.dismiss()

        return QueueingMachineSettingModel(
            isEnabled = isEnable,
            organization = if (!isEnable) false else organization,
            doctor = if (!isEnable) false else doctor,
            dept = if (!isEnable) false else dept,
            time = if (!isEnable) false else time,
            isOneTicket = if (!isEnable) false else isOneTicket,
            textSize = if (!isEnable) QueueingMachineSettingModel.MillimeterSize.FIFTY_SEVEN_MILLIMETERS else textSize
        )
    }


    private fun onSetAutomaticAppointmentSettingItemClicked() {
        dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.automatic_appointment_setting_dialog)

        if (dialog.window == null)
            return

        dialog.window!!.setGravity(Gravity.CENTER)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val automaticAppointmentSettingModel = viewModel.automaticAppointmentSetting

        dialog.automatic_appointment_setting_switcher.isChecked =
            automaticAppointmentSettingModel.isEnabled

        dialog.automatic_appointment_radio_group.isGone =
            !automaticAppointmentSettingModel.isEnabled

        dialog.automatic_appointment_setting_container.isGone =
            !(automaticAppointmentSettingModel.isEnabled &&
                    (automaticAppointmentSettingModel.mode == AutomaticAppointmentMode.SINGLE_MODE))

        when (automaticAppointmentSettingModel.mode) {
            AutomaticAppointmentMode.SINGLE_MODE ->
                dialog.automatic_appointment_radio_group.check(R.id.automatic_appointment_single_mode)
            AutomaticAppointmentMode.MULTIPLE_MODE ->
                dialog.automatic_appointment_radio_group.check(R.id.automatic_appointment_multiple_mode)
        }

        dialog.automatic_appointment_doctor_input_field.editText?.setText(
            automaticAppointmentSettingModel.doctorId
        )
        dialog.automatic_appointment_room_input_field.editText?.setText(
            automaticAppointmentSettingModel.roomId
        )


        dialog.automatic_appointment_setting_switcher.setOnCheckedChangeListener { _, isChecked ->
            dialog.automatic_appointment_radio_group.isGone = !isChecked

            val isContainerVisible = isChecked && dialog.automatic_appointment_single_mode.isChecked

            dialog.automatic_appointment_setting_container.isGone = !isContainerVisible
        }

        dialog.automatic_appointment_radio_group.setOnCheckedChangeListener(
            RadioGroup.OnCheckedChangeListener { _, checkedId ->
                if (!dialog.automatic_appointment_setting_switcher.isChecked) {
                    return@OnCheckedChangeListener
                }

                when (checkedId) {
                    R.id.automatic_appointment_single_mode -> dialog.automatic_appointment_setting_container.visibility =
                        View.VISIBLE
                    R.id.automatic_appointment_multiple_mode -> dialog.automatic_appointment_setting_container.visibility =
                        View.GONE
                }
            }
        )


        dialog.automatic_appointment_doctor_input_field.editText?.doOnTextChanged { inputText, _, _, _ ->
            if (inputText.toString().isNotEmpty())
                dialog.automatic_appointment_doctor_input_field.error = null

        }
        dialog.automatic_appointment_doctor_input_field.editText?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus)
                dialog.automatic_appointment_doctor_input_field.error = null
        }

        dialog.automatic_appointment_room_input_field.editText?.doOnTextChanged { inputText, _, _, _ ->
            if (inputText.toString().isNotEmpty())
                dialog.automatic_appointment_room_input_field.error = null

        }
        dialog.automatic_appointment_room_input_field.editText?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus)
                dialog.automatic_appointment_room_input_field.error = null
        }


        val saveButton = dialog.automatic_appointment_dialog_save_btn
        val cancelButton = dialog.automatic_appointment_dialog_cancel_btn

        saveButton.setOnClickListener {
            val isEnable = dialog.automatic_appointment_setting_switcher.isChecked
            val mode =
                if (dialog.automatic_appointment_single_mode.isChecked) AutomaticAppointmentMode.SINGLE_MODE
                else AutomaticAppointmentMode.MULTIPLE_MODE

            if (isEnable) {
                val doctors =
                    dialog.automatic_appointment_doctor_input_field.editText?.text.toString()
                val rooms = dialog.automatic_appointment_room_input_field.editText?.text.toString()

                if ((doctors.isNotBlank() && rooms.isNotBlank()) || mode == AutomaticAppointmentMode.MULTIPLE_MODE) {
                    val newAutomaticAppointment = AutomaticAppointmentSettingModel(
                        isEnabled = true,
                        mode = mode,
                        doctorId = doctors,
                        roomId = rooms
                    )

                    viewModel.userChangeAutomaticAppointmentSetting(
                        settingItemTitle = "Automatic Appointment",
                        originalValue = viewModel.automaticAppointmentSetting,
                        newValue = newAutomaticAppointment
                    )

                    viewModel.automaticAppointmentSetting = newAutomaticAppointment

                    dialog.dismiss()
                } else {
                    if (doctors.isBlank()) {
                        dialog.automatic_appointment_doctor_input_field.error =
                            getString(R.string.automatic_appointment_setting_error_empty_field)
                    }

                    if (rooms.isBlank()) {
                        dialog.automatic_appointment_room_input_field.error =
                            getString(R.string.automatic_appointment_setting_error_empty_field)
                    }
                }
            } else {
                val newAutomaticAppointment = AutomaticAppointmentSettingModel(
                    isEnabled = false,
                    mode = AutomaticAppointmentMode.SINGLE_MODE,
                    doctorId = "",
                    roomId = ""
                )

                viewModel.userChangeAutomaticAppointmentSetting(
                    settingItemTitle = "Automatic Appointment",
                    originalValue = viewModel.automaticAppointmentSetting,
                    newValue = newAutomaticAppointment
                )

                viewModel.automaticAppointmentSetting = newAutomaticAppointment

                dialog.dismiss()
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun onSetLanguageSettingItemClicked() {

        var currentLanguage = viewModel.language
        var languageArrayWithIsSelected = getLanguageArray(currentLanguage)


        dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.language_setting_dialog)

        if (dialog.window == null)
            return

        dialog.window!!.setGravity(Gravity.CENTER)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))


        val adapter = LanguageAdapter(
            itemList = languageArrayWithIsSelected,
            LanguageAdapter.OnClickListener { language ->
                currentLanguage = Converter.language_name_to_lang_code(requireContext(), language)
                languageArrayWithIsSelected = getLanguageArray(currentLanguage)
            })

        dialog.language_recycler_view.layoutManager = LinearLayoutManager(requireContext())
        dialog.language_recycler_view.adapter = adapter

        val saveButton = dialog.btn_version_setting_dialog_save
        val cancelButton = dialog.btn_version_setting_dialog_cancel

        saveButton.setOnClickListener {
            if (currentLanguage != viewModel.language) {
                (requireActivity() as MainActivity).setLanguage(language = currentLanguage)

                viewModel.userChangeSettingItem(
                    settingItemTitle = "Language Setting",
                    originalValue = viewModel.language,
                    newValue = currentLanguage
                )

                viewModel.language = currentLanguage
                setupUI()
            }

            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun onSetSoftwareUpdateSettingItemClicked() {

        dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.software_update_dialog)

        if (dialog.window == null)
            return

        var updateDialogOpen = true

        dialog.window!!.setGravity(Gravity.CENTER)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val havePermissionForInstallUnknownApp = checkInstallUnknownApkPermission()
        val haveWriteExternalStoragePermission = checkIsWriteExternalStoragePermission()

        if (!havePermissionForInstallUnknownApp || !haveWriteExternalStoragePermission) {

            var totalPermissions = 0
            var currentPermission = 0

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                totalPermissions++

                if (haveWriteExternalStoragePermission)
                    currentPermission++
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                totalPermissions++

                if (havePermissionForInstallUnknownApp)
                    currentPermission++
            }

            dialog.update_software_dialog_ok_btn.isGone = false
            dialog.update_software_progress_bar.isGone = true

            val textForDialog =
                "${getString(R.string.update_software_dialog_permission)} ($currentPermission/$totalPermissions)"

            dialog.update_software_text.text = textForDialog

            dialog.update_software_dialog_ok_btn.setOnClickListener {
                dialog.dismiss()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!havePermissionForInstallUnknownApp) {
                        (requireContext() as MainActivity).getInstallUnknownApkPermission()
                    }
                }
                if (!havePermissionForInstallUnknownApp) {
                    return@setOnClickListener
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    if (!haveWriteExternalStoragePermission) {
                        (requireContext() as MainActivity).getWriteExternalStoragePermission()
                    }
                }
            }

            val permissions = mutableListOf<String>()

            if (!havePermissionForInstallUnknownApp) {
                permissions.add("Install Unknown App Permission")
            }

            if (!haveWriteExternalStoragePermission) {
                permissions.add("Write External Storage Permission")
            }

            viewModel.userSelectSoftwareUpdateSetting(
                "Update Software: permission",
                permissions = permissions
            )
        } else {

            Log.d("check---", "onSetSoftwareUpdateSettingItemClicked: ${stateFlowJob?.isActive}")
            if (stateFlowJob?.isActive != true) {
                viewModel.checkForUpdates()
                viewModel.userSelectSoftwareUpdateSetting(
                    settingItemTitle = "Update Software: check update",
                    currentVersion = BuildConfig.VERSION_NAME
                )
            }

            stateFlowJob = lifecycleScope.launch(Dispatchers.Main) {
                viewModel.uiState.collect { uiState ->

                    if (updateDialogOpen) {

                        val isShowLoadingComponent =
                            uiState.updateSoftwareRequestStatus == UpdateSoftwareRequestStatus.LOADING ||
                                    uiState.updateSoftwareDownloadingStatus == UpdateSoftwareDownloadingStatus.LOADING

                        val text = if (uiState.updateSoftwareDialogText != 0)
                            "${getString(uiState.updateSoftwareDialogText)} ${uiState.updateSoftwarePercentageDownload}"
                        else ""


                        dialog.update_software_progress_bar.isGone = !isShowLoadingComponent
                        dialog.update_software_text.text = text
                    }
                }
            }

            stateFlowJob?.invokeOnCompletion {
                viewModel.closeUpdateDialog()
            }
        }

        dialog.update_software_dialog_cancel_btn.setOnClickListener {
            stateFlowJob?.cancel()
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            updateDialogOpen = false
            Log.d("check---", "onSetSoftwareUpdateSettingItemClicked: $updateDialogOpen")
        }

        dialog.show()
    }

    private fun onSetOpenSystemSettingsItemClicked() {
        viewModel.userOpenDeviceSetting()
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }

    private fun onSetExitItemClicked() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.exit_app)
            .setPositiveButton(R.string.dialog_ok_button) { _, _ ->
                viewModel.userCloseApplication()
                requireActivity().finishAndRemoveTask()
            }
            .setNegativeButton(R.string.cancel, null)
            .showOnly()
    }

    private fun getLanguageArray(currentLanguage: String): Array<LanguageModel> {
        val longLanguage = Converter.language_lang_code_to_name(requireContext(), currentLanguage)

        val languageArray = resources.getStringArray(R.array.language_items)
        val isSelectedArray = languageArray.map { it.contains(longLanguage) }

        return combineArrays(languageArray, isSelectedArray)
    }

    private fun setListOfCheckBox(
        container: LinearLayout?,
        names: Array<CreateAppointmentRequest.NationalIdFormat>,
        checks: List<Boolean>
    ) {
        for (i in 0 until names.size) {
            container?.addView(createCheckBox(title = names[i], value = checks[i]))
        }
    }

    private fun createCheckBox(
        title: CreateAppointmentRequest.NationalIdFormat,
        value: Boolean
    ): LinearLayout {

        val layoutParameters = ActionBar.LayoutParams(
            ActionBar.LayoutParams.WRAP_CONTENT,
            ActionBar.LayoutParams.WRAP_CONTENT
        )

        val linearLayout = LinearLayout(requireContext())
        linearLayout.layoutParams = layoutParameters
        linearLayout.gravity = Gravity.CENTER_VERTICAL
        linearLayout.orientation = LinearLayout.HORIZONTAL

        val checkBox = MaterialCheckBox(requireContext())

        checkBox.layoutParams = layoutParameters
        checkBox.gravity = Gravity.CENTER
        checkBox.scaleX = 1.0f
        checkBox.scaleY = 1.0f
        checkBox.isChecked = value

        checkBox.setOnCheckedChangeListener { _, isChecked ->

            val list = viewModel._tempFormatCheckedList.toMutableList()

            if (isChecked) {
                list.add(title)
            } else {
                list.remove(title)
            }

            viewModel._tempFormatCheckedList = list
        }

        val outValue = TypedValue()
        resources.getValue(R.dimen.font_size_h4_float, outValue, true)
        val textSize = outValue.float

        val textView = TextView(requireContext())

        textView.layoutParams = layoutParameters
        textView.setTextColor(Color.BLACK)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
        textView.setText(title.description)

        linearLayout.addView(checkBox)
        linearLayout.addView(textView)

        return linearLayout
    }

    private fun checkInstallUnknownApkPermission() =
        (requireContext() as MainActivity).checkInstallUnknownApkPermission()

    private fun checkIsWriteExternalStoragePermission() =
        (requireContext() as MainActivity).checkIsWriteExternalStoragePermission()


    private val manageUnknownAppSourcesLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d("check---", "onRequestPermissionsResult: its work")
            } else {
                Log.d("check---", "onRequestPermissionsResult: its work but its not")
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()

        try {
            dialog.dismiss()
        } catch (_: Exception) {
        }

        stateFlowJob?.cancel()

        _binding = null
    }
}