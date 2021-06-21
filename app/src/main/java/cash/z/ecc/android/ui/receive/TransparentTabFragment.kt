package cash.z.ecc.android.ui.receive

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import cash.z.android.qrecycler.QRecycler
import cash.z.ecc.android.BuildConfig
import cash.z.ecc.android.databinding.FragmentTabReceiveTransparentBinding
import cash.z.ecc.android.di.viewmodel.viewModel
import cash.z.ecc.android.ext.distribute
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.ui.base.BaseFragment
import cash.z.ecc.android.ui.util.AddressPartNumberSpan
import kotlinx.coroutines.launch

class TransparentTabFragment :
    BaseFragment<FragmentTabReceiveTransparentBinding>() {
    override val screen = Report.Screen.RECEIVE

    private val viewModel: ReceiveViewModel by viewModel()

    lateinit var qrecycler: QRecycler

    lateinit var addressParts: Array<TextView>

    override fun inflate(inflater: LayoutInflater): FragmentTabReceiveTransparentBinding =
        FragmentTabReceiveTransparentBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addressParts = arrayOf(
            binding.textAddressPart1,
            binding.textAddressPart2
        )
        binding.iconQrLogo.setOnLongClickListener {
            mainActivity?.takeIf { BuildConfig.FLAVOR.lowercase().contains("testnet") }?.let {
                it.copyTransparentAddress(null)
                it.onLaunchUrl("https://faucet.zecpages.com/")
                true
            } ?: false
        }
    }

    override fun onAttach(context: Context) {
        qrecycler = QRecycler() // inject! :)
        super.onAttach(context)
    }

    override fun onResume() {
        super.onResume()
        resumedScope.launch {
            onAddressLoaded(viewModel.getTranparentAddress())
        }
    }

    private fun onAddressLoaded(address: String) {
        twig("address loaded:  $address length: ${address.length}")
        qrecycler.load(address)
            .withQuietZoneSize(3)
            .withCorrectionLevel(QRecycler.CorrectionLevel.MEDIUM)
            .into(binding.receiveQrCode)

        address.distribute(2) { i, part ->
            setAddressPart(i, part)
        }
    }

    private fun setAddressPart(index: Int, addressPart: String) {
        twig("setting address for part $index) $addressPart")
        val thinSpace = "\u2005" // 0.25 em space
        val textSpan = SpannableString("${index + 1}$thinSpace$addressPart")

        textSpan.setSpan(AddressPartNumberSpan(), 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        addressParts[index].text = textSpan
    }
}
