package protect.card_locker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import protect.card_locker.BarcodeSelectorAdapter.BarcodeSelectorListener
import protect.card_locker.compose.CatimaAboutSection
import protect.card_locker.compose.CatimaTopAppBar
import protect.card_locker.compose.theme.CatimaTheme
import protect.card_locker.databinding.BarcodeSelectorActivityBinding

/**
 * This activity is callable and will allow a user to enter
 * barcode data and generate all barcodes possible for
 * the data. The user may then select any barcode, where its
 * data and type will be returned to the caller.
 */
class BarcodeSelectorActivity : CatimaAppCompatActivity(), BarcodeSelectorListener {
    private var binding: BarcodeSelectorActivityBinding? = null
    private val typingDelayHandler = Handler(Looper.getMainLooper())
    private var mAdapter: BarcodeSelectorAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ScreenContent()
        }

//        binding = BarcodeSelectorActivityBinding.inflate(
//            layoutInflater
//        )
//        setTitle(R.string.selectBarcodeTitle)
//        setContentView(binding!!.root)
//        Utils.applyWindowInsets(binding!!.root)
//        val toolbar: Toolbar = binding!!.toolbar
//        setSupportActionBar(toolbar)
//        enableToolbarBackButton()
//
//        val cardId = binding!!.cardId
//        val mBarcodeList = binding!!.barcodes
//        mAdapter = BarcodeSelectorAdapter(this, ArrayList(), this)
//        mBarcodeList.adapter = mAdapter
//
//        cardId.addTextChangedListener(object : SimpleTextWatcher() {
//            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
//                // Delay the input processing so we avoid overload
//                typingDelayHandler.removeCallbacksAndMessages(null)
//
//                typingDelayHandler.postDelayed({
//                    Log.d(TAG, "Entered text: $s")
//                    runOnUiThread {
//                        generateBarcodes(s.toString())
//                    }
//                }, INPUT_DELAY.toLong())
//            }
//        })
//
//        val b = intent.extras
//        val initialCardId = b?.getString(LoyaltyCard.BUNDLE_LOYALTY_CARD_CARD_ID)
//
//        if (initialCardId != null) {
//            cardId.setText(initialCardId)
//        } else {
//            generateBarcodes("")
//        }
    }

    @Composable
    fun ScreenContent() {
        var cardId by remember { mutableStateOf("") }

        CatimaTheme {
            Scaffold(
                topBar = { CatimaTopAppBar(stringResource(R.string.selectBarcodeTitle), onBackPressedDispatcher) }
            ) { innerPadding ->
                Column(
                    modifier = Modifier.padding(innerPadding).verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.manually_enter_barcode_instructions),
                        modifier = Modifier.padding(all = 8.dp)
                    )

                    TextField(
                        value = cardId,
                        onValueChange = { cardId = it },
                        label = { Text(stringResource(R.string.cardId)) }
                    )

//                    LazyColumn {
//                        items(barcodes) { barcode ->
//
//                        }
//                    }
                }
            }
        }
    }

    @Preview
    @Composable
    fun BarcodeSelectorActivityPreview() {
        ScreenContent()
    }

    private fun generateBarcodes(value: String) {
        // Update barcodes
        val barcodes = ArrayList<CatimaBarcodeWithValue>()
        for (barcodeFormat in CatimaBarcode.barcodeFormats) {
            val catimaBarcode = CatimaBarcode.fromBarcode(barcodeFormat)
            barcodes.add(CatimaBarcodeWithValue(catimaBarcode, value))
        }
        mAdapter!!.setBarcodes(barcodes)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            setResult(RESULT_CANCELED)
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onRowClicked(inputPosition: Int, view: View) {
        val barcodeWithValue = mAdapter!!.getItem(inputPosition)
        val catimaBarcode = barcodeWithValue!!.catimaBarcode()

        if (!mAdapter!!.isValid(view)) {
            Toast.makeText(this, getString(R.string.wrongValueForBarcodeType), Toast.LENGTH_LONG)
                .show()
            return
        }

        val barcodeFormat = catimaBarcode.format().name
        val value = barcodeWithValue.value()

        Log.d(
            TAG,
            "Selected barcode type $barcodeFormat"
        )

        val result = Intent()
        result.putExtra(BARCODE_FORMAT, barcodeFormat)
        result.putExtra(BARCODE_CONTENTS, value)
        this@BarcodeSelectorActivity.setResult(RESULT_OK, result)
        finish()
    }

    companion object {
        private const val TAG = "Catima"

        // Result this activity will return
        const val BARCODE_CONTENTS: String = "contents"
        const val BARCODE_FORMAT: String = "format"

        const val INPUT_DELAY: Int = 250
    }
}


