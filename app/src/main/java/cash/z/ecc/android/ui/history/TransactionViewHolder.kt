package cash.z.ecc.android.ui.history

import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import cash.z.ecc.android.R
import cash.z.ecc.android.ext.goneIf
import cash.z.ecc.android.ext.toAppColor
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.isShielded
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.ui.MainActivity
import cash.z.ecc.android.ui.util.toUtf8Memo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TransactionViewHolder<T : ConfirmedTransaction>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val indicator = itemView.findViewById<View>(R.id.indicator)
    private val amountText = itemView.findViewById<TextView>(R.id.text_transaction_amount)
    private val topText = itemView.findViewById<TextView>(R.id.text_transaction_top)
    private val bottomText = itemView.findViewById<TextView>(R.id.text_transaction_bottom)
    private val shieldIcon = itemView.findViewById<View>(R.id.image_shield)
    private val formatter = SimpleDateFormat("M/d h:mma", Locale.getDefault())

    fun bindTo(transaction: T?) {
        val mainActivity = itemView.context as MainActivity
        mainActivity.lifecycleScope.launch {
            // update view
            var lineOne: String = ""
            var lineTwo: String = ""
            var amountZec: String = ""
            var amountDisplay: String = ""
            var amountColor: Int = R.color.text_light_dimmed
            var lineOneColor: Int = R.color.text_light
            var lineTwoColor: Int = R.color.text_light_dimmed
            var indicatorBackground: Int = R.drawable.background_indicator_unknown

            transaction?.apply {
                itemView.setOnClickListener {
                    onTransactionClicked(this)
                }
                itemView.setOnLongClickListener {
                    onTransactionLongPressed(this)
                    true
                }
                amountZec = value.convertZatoshiToZecString()
                // TODO: these might be good extension functions
                val timestamp = formatter.format(blockTimeInSeconds * 1000L)
                val isMined = blockTimeInSeconds != 0L
                when {
                    !toAddress.isNullOrEmpty() -> {
                        lineOne = "You paid ${toAddress?.toAbbreviatedAddress()}"
                        lineTwo = if (isMined) "Sent $timestamp" else "Pending confirmation"
                        // TODO: this logic works but is sloppy. Find a more robust solution to displaying information about expiration (such as expires in 1 block, etc). Then if it is way beyond expired, remove it entirely. Perhaps give the user a button for that (swipe to dismiss?)
                        if(!isMined && (expiryHeight != null) && (expiryHeight!! < mainActivity.latestHeight ?: -1)) lineTwo = "Expired"
                        amountDisplay = "- $amountZec"
                        if (isMined) {
                            amountColor = R.color.zcashRed
                            indicatorBackground = R.drawable.background_indicator_outbound
                        } else {
                            lineOneColor = R.color.text_light_dimmed
                            lineTwoColor = R.color.text_light
                        }
                    }
                    toAddress.isNullOrEmpty() && value > 0L && minedHeight > 0 -> {
                        lineOne = "${mainActivity.getSender(transaction)} paid you"
                        lineTwo = "Received $timestamp"
                        amountDisplay = "+ $amountZec"
                        amountColor = R.color.zcashGreen
                        indicatorBackground = R.drawable.background_indicator_inbound
                    }
                    else -> {
                        lineOne = "Unknown"
                        lineTwo = "Unknown"
                        amountDisplay = "$amountZec"
                        amountColor = R.color.text_light
                    }
                }
                // sanitize amount
                if (value < ZcashSdk.MINERS_FEE_ZATOSHI) amountDisplay = "< 0.001"
                else if (amountZec.length > 10) { // 10 allows 3 digits to the left and 6 to the right of the decimal
                    amountDisplay = "tap to view"
                }
            }

            topText.text = lineOne
            bottomText.text = lineTwo
            amountText.text = amountDisplay
            amountText.setTextColor(amountColor.toAppColor())
            topText.setTextColor(lineOneColor.toAppColor())
            bottomText.setTextColor(lineTwoColor.toAppColor())
            val context = itemView.context
            indicator.background = context.resources.getDrawable(indicatorBackground)

            // TODO: change this so we see the shield if it is a z-addr in the address line but verify the intended design/behavior, first
            shieldIcon.goneIf((transaction?.raw != null || transaction?.expiryHeight != null) && !transaction?.toAddress.isShielded())
        }
    }

    private fun onTransactionClicked(transaction: ConfirmedTransaction) {
        (itemView.context as MainActivity).apply {
            historyViewModel.selectedTransaction = transaction
            safeNavigate(R.id.action_nav_history_to_nav_transaction)
        }
    }

    private fun onTransactionLongPressed(transaction: ConfirmedTransaction) {
        val mainActivity = itemView.context as MainActivity
        (transaction.toAddress ?: mainActivity.extractAddress(transaction.memo.toUtf8Memo()))?.let {
            mainActivity.copyText(it, "Transaction Address")
        }
    }
    
}


