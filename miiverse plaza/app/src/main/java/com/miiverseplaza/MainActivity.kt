package com.miiverseplaza

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.miiverseplaza.data.EncounterStore
import com.miiverseplaza.data.ProfileStore
import com.miiverseplaza.model.Encounter
import com.miiverseplaza.model.PlazaProfile
import com.miiverseplaza.net.PlazaProtocol
import com.miiverseplaza.net.StreetPassBridge
import java.util.UUID
import java.text.DateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    private lateinit var profileStore: ProfileStore
    private lateinit var encounterStore: EncounterStore
    private lateinit var bridge: StreetPassBridge

    private lateinit var nicknameInput: EditText
    private lateinit var gameInput: EditText
    private lateinit var statusInput: EditText
    private lateinit var platformLabel: TextView
    private lateinit var statusLabel: TextView
    private lateinit var endpointLabel: TextView
    private lateinit var syncPreviewLabel: TextView
    private lateinit var missionLabel: TextView
    private lateinit var scoreLabel: TextView
    private lateinit var hubSummaryLabel: TextView
    private lateinit var targetHostInput: EditText
    private lateinit var dsiTarget: CheckBox
    private lateinit var switchTarget: CheckBox
    private lateinit var n3dsTarget: CheckBox
    private lateinit var encountersList: ListView

    private val encounterItems = mutableListOf<String>()
    private lateinit var encounterAdapter: ArrayAdapter<String>
    private val encounters = mutableListOf<Encounter>()
    private var plazaPoints = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        profileStore = ProfileStore(this)
        encounterStore = EncounterStore(this)
        bridge = StreetPassBridge(
            context = this,
            scope = lifecycleScope,
            onEncounter = ::handleEncounter
        )

        bindViews()
        loadExistingEncounters()
        loadProfileIntoUi(profileStore.loadProfile())
        setupReactivePreview()

        findViewById<Button>(R.id.saveProfileButton).setOnClickListener {
            val profile = collectProfile()
            profileStore.saveProfile(profile)
            refreshSyncPreview()
            Snackbar.make(it, R.string.profile_saved, Snackbar.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.startDiscoveryButton).setOnClickListener { view ->
            val profile = collectProfile()
            profileStore.saveProfile(profile)
            bridge.start(profile)
            statusLabel.text = getString(R.string.discovery_running)
            Snackbar.make(view, R.string.discovery_started, Snackbar.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.stopDiscoveryButton).setOnClickListener { view ->
            bridge.stop()
            statusLabel.text = getString(R.string.discovery_stopped)
            Snackbar.make(view, R.string.discovery_paused, Snackbar.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.sendSyncButton).setOnClickListener { view ->
            val profile = collectProfile()
            profileStore.saveProfile(profile)
            val payload = PlazaProtocol.encodeSystemSync(profile, selectedTargets(), encounters)
            val targetHost = targetHostInput.text.toString().ifBlank { "255.255.255.255" }

            bridge.sendSystemSync(targetHost, payload) { success ->
                val messageRes = if (success) {
                    R.string.sync_sent
                } else {
                    R.string.sync_failed
                }
                Snackbar.make(view, getString(messageRes, targetHost, PlazaProtocol.PORT), Snackbar.LENGTH_LONG).show()
            }
        }

        findViewById<Button>(R.id.mockEncounterButton).setOnClickListener { view ->
            val mockEncounter = Encounter(
                profile = PlazaProfile(
                    deviceId = UUID.randomUUID().toString(),
                    nickname = "Visitor ${encounters.size + 1}",
                    favoriteGame = if (encounters.size % 2 == 0) "Mario vs. Donkey Kong" else "Animal Crossing",
                    statusMessage = "Gespot via telefoonhub",
                    platform = "Phone Pass"
                ),
                timestamp = System.currentTimeMillis(),
                hostAddress = "local-mock"
            )
            handleEncounter(mockEncounter)
            Snackbar.make(view, R.string.mock_encounter_added, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        bridge.stop()
        super.onDestroy()
    }

    private fun bindViews() {
        nicknameInput = findViewById(R.id.nicknameInput)
        gameInput = findViewById(R.id.favoriteGameInput)
        statusInput = findViewById(R.id.statusMessageInput)
        platformLabel = findViewById(R.id.platformValue)
        statusLabel = findViewById(R.id.discoveryStatusValue)
        endpointLabel = findViewById(R.id.endpointValue)
        syncPreviewLabel = findViewById(R.id.syncPreviewValue)
        missionLabel = findViewById(R.id.missionValue)
        scoreLabel = findViewById(R.id.scoreValue)
        hubSummaryLabel = findViewById(R.id.hubSummaryValue)
        targetHostInput = findViewById(R.id.targetHostInput)
        dsiTarget = findViewById(R.id.targetDsi)
        switchTarget = findViewById(R.id.targetSwitch)
        n3dsTarget = findViewById(R.id.target3ds)
        encountersList = findViewById(R.id.encountersList)

        encounterAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, encounterItems)
        encountersList.adapter = encounterAdapter
        endpointLabel.text = getString(R.string.endpoint_template, PlazaProtocol.PORT)
    }

    private fun loadProfileIntoUi(profile: PlazaProfile) {
        nicknameInput.setText(profile.nickname)
        gameInput.setText(profile.favoriteGame)
        statusInput.setText(profile.statusMessage)
        platformLabel.text = profile.platform
        statusLabel.text = getString(R.string.discovery_stopped)
        missionLabel.text = getString(R.string.default_mission)
        scoreLabel.text = getString(R.string.score_template, plazaPoints)
        dsiTarget.isChecked = true
        switchTarget.isChecked = true
        n3dsTarget.isChecked = true
        hubSummaryLabel.text = getString(R.string.hub_summary_template, encounters.size, selectedTargets().joinToString())
        targetHostInput.setText("255.255.255.255")
        syncPreviewLabel.text = PlazaProtocol.encodeSystemSync(profile, selectedTargets(), encounters)
    }

    private fun collectProfile(): PlazaProfile {
        val existing = profileStore.loadProfile()
        return existing.copy(
            nickname = nicknameInput.text.toString().ifBlank { "Plaza Visitor" },
            favoriteGame = gameInput.text.toString().ifBlank { "Mario Kart DS" },
            statusMessage = statusInput.text.toString().ifBlank { "Op zoek naar een encounter!" }
        )
    }

    private fun selectedTargets(): List<String> {
        val targets = mutableListOf<String>()
        if (dsiTarget.isChecked) targets += "DSi"
        if (switchTarget.isChecked) targets += "Switch"
        if (n3dsTarget.isChecked) targets += "3DS"
        return if (targets.isEmpty()) listOf("DSi") else targets
    }

    private fun handleEncounter(encounter: Encounter) {
        runOnUiThread {
            val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(encounter.timestamp))
            val line = getString(
                R.string.encounter_template,
                encounter.profile.nickname,
                encounter.profile.platform,
                encounter.profile.favoriteGame,
                encounter.hostAddress,
                time
            )

            encounters.removeAll { it.profile.deviceId == encounter.profile.deviceId }
            encounters.add(0, encounter)
            encounterStore.saveEncounters(encounters)
            rebuildEncounterList()

            plazaPoints = encounters.size * 10
            scoreLabel.text = getString(R.string.score_template, plazaPoints)
            missionLabel.text = getString(R.string.mission_progress_template, encounter.profile.nickname)
            hubSummaryLabel.text = getString(R.string.hub_summary_template, encounters.size, selectedTargets().joinToString())
            encounterAdapter.notifyDataSetChanged()
            refreshSyncPreview()
        }
    }

    private fun setupReactivePreview() {
        refreshSyncPreview()

        val watcher = SimpleTextWatcher { refreshSyncPreview() }
        nicknameInput.addTextChangedListener(watcher)
        gameInput.addTextChangedListener(watcher)
        statusInput.addTextChangedListener(watcher)

        val targetListener = android.widget.CompoundButton.OnCheckedChangeListener { _, _ ->
            hubSummaryLabel.text = getString(R.string.hub_summary_template, encounters.size, selectedTargets().joinToString())
            refreshSyncPreview()
        }
        dsiTarget.setOnCheckedChangeListener(targetListener)
        switchTarget.setOnCheckedChangeListener(targetListener)
        n3dsTarget.setOnCheckedChangeListener(targetListener)
    }

    private fun refreshSyncPreview() {
        syncPreviewLabel.text = PlazaProtocol.encodeSystemSync(collectProfile(), selectedTargets(), encounters)
    }

    private fun loadExistingEncounters() {
        encounters.clear()
        encounters.addAll(encounterStore.loadEncounters().sortedByDescending { it.timestamp })
        rebuildEncounterList()
        plazaPoints = encounters.size * 10
    }

    private fun rebuildEncounterList() {
        encounterItems.clear()
        encounterItems.addAll(
            encounters.map { encounter ->
                val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(encounter.timestamp))
                "${encounter.profile.deviceId.take(8)} | " + getString(
                    R.string.encounter_template,
                    encounter.profile.nickname,
                    encounter.profile.platform,
                    encounter.profile.favoriteGame,
                    encounter.hostAddress,
                    time
                )
            }
        )
        if (::encounterAdapter.isInitialized) {
            encounterAdapter.notifyDataSetChanged()
        }
    }
}
