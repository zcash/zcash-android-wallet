package cash.z.ecc.android.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import cash.z.ecc.android.R
import cash.z.ecc.android.databinding.FragmentDetailBinding
import cash.z.ecc.android.di.viewmodel.viewModel
import cash.z.ecc.android.ext.goneIf
import cash.z.ecc.android.ext.onClickNavUp
import cash.z.ecc.android.ext.toColoredSpan
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.feedback.Report.Tap.DETAIL_BACK
import cash.z.ecc.android.ui.base.BaseFragment
import cash.z.ecc.android.sdk.block.CompactBlockProcessor.WalletBalance
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.ext.collectWith
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.sdk.ext.twig
import kotlinx.coroutines.launch


class WalletDetailFragment : BaseFragment<FragmentDetailBinding>() {
    override val screen = Report.Screen.DETAIL
    private val viewModel: WalletDetailViewModel by viewModel()

    private lateinit var adapter: TransactionAdapter<ConfirmedTransaction>

    override fun inflate(inflater: LayoutInflater): FragmentDetailBinding =
        FragmentDetailBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButtonHitArea.onClickNavUp { tapped(DETAIL_BACK) }
        lifecycleScope.launch {
            binding.textAddress.text = viewModel.getAddress().toAbbreviatedAddress()
        }
    }

    override fun onResume() {
        super.onResume()
        initTransactionUI()
        viewModel.balance.collectWith(resumedScope) {
            onBalanceUpdated(it)
        }
    }

    private fun onBalanceUpdated(balance: WalletBalance) {
        binding.textBalanceAvailable.text = balance.availableZatoshi.convertZatoshiToZecString()
        val change = (balance.totalZatoshi - balance.availableZatoshi)
        binding.textBalanceDescription.apply {
            goneIf(change <= 0L)
            val changeString = change.convertZatoshiToZecString()
            text = "(expecting +$changeString ZEC)".toColoredSpan(R.color.text_light, "+${changeString}")
        }
    }

    private fun initTransactionUI() {
        adapter = TransactionAdapter()
        binding.recyclerTransactions.apply {
            layoutManager =
                LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            addItemDecoration(TransactionsFooter(binding.recyclerTransactions.context))
            adapter = this@WalletDetailFragment.adapter
            scrollToTop()
        }
        viewModel.transactions.collectWith(resumedScope) { onTransactionsUpdated(it) }
    }

    private fun onTransactionsUpdated(transactions: PagedList<ConfirmedTransaction>) {
        twig("got a new paged list of transactions")
        transactions.size.let { newCount ->
            binding.groupEmptyViews.goneIf(newCount > 0)
            val preSize = adapter.itemCount
            adapter.submitList(transactions)
            // don't rescroll while the user is looking at the list, unless it's initialization
            // using 4 here because there might be headers or other things that make 0 a bad pick
            // 4 is about how many can fit before scrolling becomes necessary on many screens
            if (preSize < 4 && newCount > preSize) {
                scrollToTop()
            }
        }
    }

    private fun scrollToTop() {
        twig("scrolling to the top")
        binding.recyclerTransactions.apply {
            postDelayed({
                smoothScrollToPosition(0)
            }, 5L)
        }
    }

    // TODO: maybe implement this for better fade behavior. Or do an actual scroll behavior instead, yeah do that. Or an item decoration.
    fun onLastItemShown(item: ConfirmedTransaction, position: Int) {
        binding.footerFade.alpha = position.toFloat() / (binding.recyclerTransactions.adapter?.itemCount ?: 1)
    }
}