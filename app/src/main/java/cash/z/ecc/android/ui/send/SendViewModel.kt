package cash.z.ecc.android.ui.send

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.R
import cash.z.ecc.android.ext.Const
import cash.z.ecc.android.ext.WalletZecFormmatter
import cash.z.ecc.android.feedback.Feedback
import cash.z.ecc.android.feedback.Feedback.Keyed
import cash.z.ecc.android.feedback.Feedback.TimeMetric
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.feedback.Report.Funnel.Send
import cash.z.ecc.android.feedback.Report.Funnel.Send.SendSelected
import cash.z.ecc.android.feedback.Report.Funnel.Send.SpendingKeyFound
import cash.z.ecc.android.feedback.Report.Issue
import cash.z.ecc.android.feedback.Report.MetricType
import cash.z.ecc.android.feedback.Report.MetricType.*
import cash.z.ecc.android.lockbox.LockBox
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.db.entity.*
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.AddressType
import cash.z.ecc.android.ui.util.INCLUDE_MEMO_PREFIX_STANDARD
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SendViewModel @Inject constructor() : ViewModel() {

    // note used in testing
    val metrics = mutableMapOf<String, TimeMetric>()

    @Inject
    lateinit var lockBox: LockBox

    @Inject
    lateinit var synchronizer: Synchronizer

    @Inject
    lateinit var feedback: Feedback

    var fromAddress: String = ""
    var toAddress: String = ""
    var memo: String = ""
    var zatoshiAmount: Long = -1L
    var includeFromAddress: Boolean = false
        set(value) {
            require(!value || (value && !fromAddress.isNullOrEmpty())) {
                "Error: fromAddress was empty while attempting to include it in the memo. Verify" +
                        " that initFromAddress() has previously been called on this viewmodel."
            }
            field = value
        }
    val isShielded get() = toAddress.startsWith("z")
    
    fun send(): Flow<PendingTransaction> {
        funnel(SendSelected)
        val memoToSend = createMemoToSend()
        val keys = DerivationTool.deriveSpendingKeys(
            lockBox.getBytes(Const.Backup.SEED)!!,
            synchronizer.network
        )
        funnel(SpendingKeyFound)
        reportUserInputIssues(memoToSend)
        return synchronizer.sendToAddress(
            keys[0],
            zatoshiAmount,
            toAddress,
            memoToSend.chunked(ZcashSdk.MAX_MEMO_SIZE).firstOrNull() ?: ""
        ).onEach {
            twig("Received pending txUpdate: ${it?.toString()}")
            updateMetrics(it)
            reportFailures(it)
        }
    }

    fun cancel(pendingId: Long) {
        viewModelScope.launch {
            synchronizer.cancelSpend(pendingId)
        }
    }

    fun createMemoToSend() = if (includeFromAddress) "$memo\n$INCLUDE_MEMO_PREFIX_STANDARD\n$fromAddress" else memo

    suspend fun validateAddress(address: String): AddressType =
        synchronizer.validateAddress(address)

    fun validate(context: Context, availableZatoshi: Long?, maxZatoshi: Long?) = flow<String?> {

        when {
            synchronizer.validateAddress(toAddress).isNotValid -> {
                emit(context.getString(R.string.send_validation_error_address_invalid))
            }
            zatoshiAmount < 1 -> {
                emit(context.getString(R.string.send_validation_error_amount_minimum))
            }
            availableZatoshi == null -> {
                emit(context.getString(R.string.send_validation_error_unknown_funds))
            }
            availableZatoshi == 0L -> {
                emit(context.getString(R.string.send_validation_error_no_available_funds))
            }
            availableZatoshi > 0 && availableZatoshi < ZcashSdk.MINERS_FEE_ZATOSHI -> {
                emit(context.getString(R.string.send_validation_error_dust))
            }
            maxZatoshi != null && zatoshiAmount > maxZatoshi -> {
                emit(context.getString(R.string.send_validation_error_too_much, WalletZecFormmatter.toZecStringFull(maxZatoshi)))
            }
            createMemoToSend().length > ZcashSdk.MAX_MEMO_SIZE -> {
                emit(context.getString(R.string.send_validation_error_memo_length, ZcashSdk.MAX_MEMO_SIZE))
            }
            else -> emit(null)
        }
    }

    fun afterInitFromAddress(block: () -> Unit) {
        viewModelScope.launch {
            fromAddress = synchronizer.getAddress()
            block()
        }
    }

    fun reset() {
        fromAddress = ""
        toAddress = ""
        memo = ""
        zatoshiAmount = -1L
        includeFromAddress = false
    }


    //
    // Analytics
    //

    private fun reportFailures(tx: PendingTransaction) {
        when {
            tx.isCancelled() -> funnel(Send.Cancelled)
            tx.isFailedEncoding() -> {
                // report that the funnel leaked and also capture a non-fatal app error
                funnel(Send.ErrorEncoding(tx.errorCode, tx.errorMessage))
                feedback.report(Report.Error.NonFatal.TxEncodeError(tx.errorCode, tx.errorMessage))
            }
            tx.isFailedSubmit() -> {
                // report that the funnel leaked and also capture a non-fatal app error
                funnel(Send.ErrorSubmitting(tx.errorCode, tx.errorMessage))
                feedback.report(Report.Error.NonFatal.TxSubmitError(tx.errorCode, tx.errorMessage))
            }
        }
    }

    private fun reportUserInputIssues(memoToSend: String) {
        if (toAddress == fromAddress) feedback.report(Issue.SelfSend)
        when {
            zatoshiAmount < ZcashSdk.MINERS_FEE_ZATOSHI -> feedback.report(Issue.TinyAmount)
            zatoshiAmount < 100 -> feedback.report(Issue.MicroAmount)
            zatoshiAmount == 1L -> feedback.report(Issue.MinimumAmount)
        }
        memoToSend.length.also {
            when {
                it > ZcashSdk.MAX_MEMO_SIZE -> feedback.report(Issue.TruncatedMemo(it))
                it > (ZcashSdk.MAX_MEMO_SIZE * 0.96) -> feedback.report(Issue.LargeMemo(it))
            }
        }
    }

    private fun updateMetrics(tx: PendingTransaction) {
        try {
            when {
                tx.isMined() -> TRANSACTION_SUBMITTED to TRANSACTION_MINED by tx.id
                tx.isSubmitSuccess() -> TRANSACTION_CREATED to TRANSACTION_SUBMITTED by tx.id
                tx.isCreated() -> TRANSACTION_INITIALIZED to TRANSACTION_CREATED by tx.id
                tx.isCreating() -> +TRANSACTION_INITIALIZED by tx.id
                else -> null
            }?.let { metricId ->
                report(metricId)
            }
        } catch (t: Throwable) {
            feedback.report(t)
        }
    }

    private fun report(metricId: String?) {
        metrics[metricId]?.let { metric ->
            metric.takeUnless { (it.elapsedTime ?: 0) <= 0L }?.let {
                viewModelScope.launch {
                    withContext(IO) {
                        feedback.report(metric)

                        // does this metric complete another metric?
                        metricId!!.toRelatedMetricId().let { relatedId ->
                            metrics[relatedId]?.let { relatedMetric ->
                                // then remove the related metric, itself. And the relation.
                                metrics.remove(relatedMetric.toMetricIdFor(metricId!!.toTxId()))
                                metrics.remove(relatedId)
                            }
                        }

                        // remove all top-level metrics
                        if (metric.key == Report.MetricType.TRANSACTION_MINED.key) metrics.remove(metricId)
                    }
                }
            }
        }
    }

    fun funnel(step: Send?) {
        step ?: return
        feedback.report(step)
    }

    private operator fun MetricType.unaryPlus(): TimeMetric = TimeMetric(key, description).markTime()
    private infix fun TimeMetric.by(txId: Long) = this.toMetricIdFor(txId).also { metrics[it] = this }
    private infix fun Pair<MetricType, MetricType>.by(txId: Long): String? {
        val startMetric = first.toMetricIdFor(txId).let { metricId ->
            metrics[metricId].also { if (it == null) println("Warning no start metric for id: $metricId") }
        }
        return startMetric?.endTime?.let { startMetricEndTime ->
                TimeMetric(second.key, second.description, mutableListOf(startMetricEndTime))
                    .markTime().let { endMetric ->
                        endMetric.toMetricIdFor(txId).also { metricId ->
                            metrics[metricId] = endMetric
                            metrics[metricId.toRelatedMetricId()] = startMetric
                        }
                    }
            }

    }

    private fun Keyed<String>.toMetricIdFor(id: Long): String = "$id.$key"
    private fun String.toRelatedMetricId(): String = "$this.related"
    private fun String.toTxId(): Long = split('.').first().toLong()
}








