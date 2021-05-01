package cash.z.ecc.android.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.navArgs
import cash.z.ecc.android.R
import cash.z.ecc.android.databinding.FragmentFeedbackBinding
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.feedback.Report.Funnel.UserFeedback
import cash.z.ecc.android.feedback.Report.Tap.FEEDBACK_CANCEL
import cash.z.ecc.android.feedback.Report.Tap.FEEDBACK_SUBMIT
import cash.z.ecc.android.ui.base.BaseFragment

/**
 * Fragment representing the home screen of the app. This is the screen most often seen by the user when launching the
 * application.
 */
class FeedbackFragment : BaseFragment<FragmentFeedbackBinding>() {
    override val screen = Report.Screen.FEEDBACK
    val args: FeedbackFragmentArgs by navArgs()

    override fun inflate(inflater: LayoutInflater): FragmentFeedbackBinding =
        FragmentFeedbackBinding.inflate(inflater)

    private lateinit var ratings: Array<View>

//    private val padder = ViewTreeObserver.OnGlobalLayoutListener {
//        Toast.makeText(mainActivity, "LAYOUT", Toast.LENGTH_SHORT).show()
//    }

    //
    // LifeCycle
    //

    override fun onResume() {
        super.onResume()
//        mainActivity!!.window.decorView.viewTreeObserver.addOnGlobalLayoutListener(padder)
//        mainActivity!!.findViewById<View>(android.R.id.content).viewTreeObserver.addOnGlobalLayoutListener(padder)
    }

    override fun onPause() {
        super.onPause()
//        mainActivity!!.window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(padder)
//        mainActivity!!.findViewById<View>(android.R.id.content).viewTreeObserver.removeOnGlobalLayoutListener(padder)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            backButtonHitArea.setOnClickListener(::onFeedbackCancel)
            buttonSubmit.setOnClickListener(::onFeedbackSubmit)

            ratings = arrayOf(feedbackExp1, feedbackExp2, feedbackExp3, feedbackExp4, feedbackExp5)
            ratings.forEach {
                it.setOnClickListener(::onRatingClicked)
            }

            if (args.rating >= 0) {
                onRatingClicked(ratings[args.rating])
            }
        }
    }

    //
    // Private API
    //

    private fun onFeedbackSubmit(view: View) {
        Toast.makeText(mainActivity, R.string.feedback_thanks, Toast.LENGTH_LONG).show()
        tapped(FEEDBACK_SUBMIT)

        val q1 = binding.inputQuestion1.editText?.text.toString()
        val q2 = binding.inputQuestion2.editText?.text.toString()
        val q3 = binding.inputQuestion3.editText?.text.toString()
        val rating = ratings.indexOfFirst { it.isActivated } + 1
        val solicited = args.isSolicited

        mainActivity?.reportFunnel(UserFeedback.Submitted(rating, q1, q2, q3, solicited))

        mainActivity?.navController?.navigateUp()
    }
    private fun onFeedbackCancel(view: View) {
        tapped(FEEDBACK_CANCEL)
        mainActivity?.reportFunnel(UserFeedback.Cancelled)
        mainActivity?.navController?.navigateUp()
    }

    private fun onRatingClicked(view: View) {
        ratings.forEach { it.isActivated = false }
        view.isActivated = !view.isActivated
    }
}
