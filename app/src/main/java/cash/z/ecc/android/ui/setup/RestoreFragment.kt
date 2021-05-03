package cash.z.ecc.android.ui.setup

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import cash.z.ecc.android.R
import cash.z.ecc.android.ZcashWalletApp
import cash.z.ecc.android.databinding.FragmentRestoreBinding
import cash.z.ecc.android.di.viewmodel.activityViewModel
import cash.z.ecc.android.ext.goneIf
import cash.z.ecc.android.ext.showConfirmation
import cash.z.ecc.android.ext.showInvalidSeedPhraseError
import cash.z.ecc.android.ext.showSharedLibraryCriticalError
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.feedback.Report.Funnel.Restore
import cash.z.ecc.android.feedback.Report.Tap.RESTORE_BACK
import cash.z.ecc.android.feedback.Report.Tap.RESTORE_CLEAR
import cash.z.ecc.android.feedback.Report.Tap.RESTORE_DONE
import cash.z.ecc.android.feedback.Report.Tap.RESTORE_SUCCESS
import cash.z.ecc.android.ui.base.BaseFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tylersuehr.chips.Chip
import com.tylersuehr.chips.ChipsAdapter
import com.tylersuehr.chips.SeedWordAdapter
import kotlinx.coroutines.launch

class RestoreFragment : BaseFragment<FragmentRestoreBinding>(), View.OnKeyListener {
    override val screen = Report.Screen.RESTORE

    private val walletSetup: WalletSetupViewModel by activityViewModel(false)

    private lateinit var seedWordRecycler: RecyclerView
    private var seedWordAdapter: SeedWordAdapter? = null

    override fun inflate(inflater: LayoutInflater): FragmentRestoreBinding =
        FragmentRestoreBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        seedWordRecycler = binding.chipsInput.findViewById<RecyclerView>(R.id.chips_recycler)
        seedWordAdapter = SeedWordAdapter(seedWordRecycler.adapter as ChipsAdapter).onDataSetChanged {
            onChipsModified()
        }.also { onChipsModified() }
        seedWordRecycler.adapter = seedWordAdapter

        binding.chipsInput.apply {
            setFilterableChipList(getChips())
            setDelimiter("[ ;,]", true)
        }

        binding.buttonDone.setOnClickListener {
            onDone().also { tapped(RESTORE_DONE) }
        }

        binding.buttonSuccess.setOnClickListener {
            onEnterWallet().also { tapped(RESTORE_SUCCESS) }
        }

        binding.buttonClear.setOnClickListener {
            onClearSeedWords().also { tapped(RESTORE_CLEAR) }
        }
    }

    private fun onClearSeedWords() {
        mainActivity?.showConfirmation(
            "Clear All Words",
            "Are you sure you would like to clear all the seed words and type them again?",
            "Clear",
            onPositive = {
                binding.chipsInput.clearSelectedChips()
            }
        )
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainActivity?.onFragmentBackPressed(this) {
            tapped(RESTORE_BACK)
            if (seedWordAdapter == null || seedWordAdapter?.itemCount == 1) {
                onExit()
            } else {
                MaterialAlertDialogBuilder(activity)
                    .setMessage("Are you sure? For security, the words that you have entered will be cleared!")
                    .setTitle("Abort?")
                    .setPositiveButton("Stay") { dialog, _ ->
                        mainActivity?.reportFunnel(Restore.Stay)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Exit") { dialog, _ ->
                        dialog.dismiss()
                        onExit()
                    }
                    .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Require one less tap to enter the seed words
        touchScreenForUser()
    }

    private fun onExit() {
        mainActivity?.reportFunnel(Restore.Exit)
        hideAutoCompleteWords()
        mainActivity?.hideKeyboard()
        mainActivity?.navController?.popBackStack()
    }

    private fun onEnterWallet() {
        mainActivity?.reportFunnel(Restore.Success)
        mainActivity?.safeNavigate(R.id.action_nav_restore_to_nav_home)
    }

    private fun onDone() {
        mainActivity?.reportFunnel(Restore.Done)
        mainActivity?.hideKeyboard()
        val activation = ZcashWalletApp.instance.defaultNetwork.saplingActivationHeight
        val seedPhrase = binding.chipsInput.selectedChips.joinToString(" ") {
            it.title
        }
        var birthday = binding.root.findViewById<TextView>(R.id.input_birthdate).text.toString()
            .let { birthdateString ->
                if (birthdateString.isNullOrEmpty()) activation else birthdateString.toInt()
            }.coerceAtLeast(activation)

        try {
            walletSetup.validatePhrase(seedPhrase)
            importWallet(seedPhrase, birthday)
        } catch (t: Throwable) {
            mainActivity?.showInvalidSeedPhraseError(t)
        }
    }

    private fun importWallet(seedPhrase: String, birthday: Int) {
        mainActivity?.reportFunnel(Restore.ImportStarted)
        mainActivity?.hideKeyboard()
        mainActivity?.apply {
            lifecycleScope.launch {
                try {
                    mainActivity?.startSync(walletSetup.importWallet(seedPhrase, birthday))
                    // bugfix: if the user proceeds before the synchronizer is created the app will crash!
                    binding.buttonSuccess.isEnabled = true
                    mainActivity?.reportFunnel(Restore.ImportCompleted)
                    playSound("sound_receive_small.mp3")
                    vibrateSuccess()
                } catch (e: UnsatisfiedLinkError) {
                    mainActivity?.showSharedLibraryCriticalError(e)
                }
            }
        }

        binding.groupDone.visibility = View.GONE
        binding.groupStart.visibility = View.GONE
        binding.groupSuccess.visibility = View.VISIBLE
        binding.buttonSuccess.isEnabled = false
    }

    private fun onChipsModified() {
        updateDoneViews()
        forceShowKeyboard()
    }

    private fun updateDoneViews(): Boolean {
        val count = seedWordAdapter?.itemCount ?: 0
        reportWords(count - 1) // subtract 1 for the editText
        val isDone = count > 24
        binding.groupDone.goneIf(!isDone)
        return !isDone
    }

    // forcefully show the keyboard as a hack to fix odd behavior where the keyboard
    // sometimes closes randomly and inexplicably in between seed word entries
    private fun forceShowKeyboard() {
        requireView().postDelayed(
            {
                val isDone = (seedWordAdapter?.itemCount ?: 0) > 24
                val focusedView = if (isDone) binding.inputBirthdate else seedWordAdapter!!.editText
                mainActivity!!.showKeyboard(focusedView)
                focusedView.requestFocus()
            },
            500L
        )
    }

    private fun reportWords(count: Int) {
        mainActivity?.run {
//            reportFunnel(Restore.SeedWordCount(count))
            if (count == 1) {
                reportFunnel(Restore.SeedWordsStarted)
            } else if (count == 24) {
                reportFunnel(Restore.SeedWordsCompleted)
            }
        }
    }

    private fun hideAutoCompleteWords() {
        seedWordAdapter?.editText?.setText("")
    }

    private fun getChips(): List<Chip> {
        return resources.getStringArray(R.array.word_list).map {
            SeedWordChip(it)
        }
    }

    private fun touchScreenForUser() {
        seedWordAdapter?.editText?.apply {
            postDelayed(
                {
                    seedWordAdapter?.editText?.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    dispatchTouchEvent(motionEvent(ACTION_DOWN))
                    dispatchTouchEvent(motionEvent(ACTION_UP))
                },
                100L
            )
        }
    }

    private fun motionEvent(action: Int) = SystemClock.uptimeMillis().let { now ->
        MotionEvent.obtain(now, now, action, 0f, 0f, 0)
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        return false
    }
}

class SeedWordChip(val word: String, var index: Int = -1) : Chip() {
    override fun getSubtitle(): String? = null // "subtitle for $word"
    override fun getAvatarDrawable(): Drawable? = null
    override fun getId() = index
    override fun getTitle() = word
    override fun getAvatarUri() = null
}
