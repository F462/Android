package protect.card_locker

import com.google.zxing.BarcodeFormat
import java.util.Arrays
import java.util.Collections

class CatimaBarcode private constructor(private val mBarcodeFormat: BarcodeFormat) {
    val isSupported: Boolean
        get() = barcodeFormats.contains(mBarcodeFormat)

    val isSquare: Boolean
        get() = mBarcodeFormat == BarcodeFormat.AZTEC || mBarcodeFormat == BarcodeFormat.MAXICODE || mBarcodeFormat == BarcodeFormat.QR_CODE

    fun hasInternalPadding(): Boolean {
        return mBarcodeFormat == BarcodeFormat.PDF_417
                || mBarcodeFormat == BarcodeFormat.QR_CODE
    }

    fun format(): BarcodeFormat {
        return mBarcodeFormat
    }

    fun name(): String {
        return mBarcodeFormat.name
    }

    fun prettyName(): String {
        val index = barcodeFormats.indexOf(mBarcodeFormat)

        if (index == -1 || index >= barcodePrettyNames.size) {
            return mBarcodeFormat.name
        }

        return barcodePrettyNames[index]
    }

    companion object {
        val barcodeFormats: List<BarcodeFormat> = Collections.unmodifiableList(
            Arrays.asList(
                BarcodeFormat.AZTEC,
                BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODABAR,
                BarcodeFormat.DATA_MATRIX,
                BarcodeFormat.EAN_8,
                BarcodeFormat.EAN_13,
                BarcodeFormat.ITF,
                BarcodeFormat.PDF_417,
                BarcodeFormat.QR_CODE,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E
            )
        )

        @JvmField
        val barcodePrettyNames: List<String> = Collections.unmodifiableList(
            mutableListOf(
                "Aztec",
                "Code 39",
                "Code 93",
                "Code 128",
                "Codabar",
                "Data Matrix",
                "EAN 8",
                "EAN 13",
                "ITF",
                "PDF 417",
                "QR Code",
                "UPC A",
                "UPC E"
            )
        )

        @JvmStatic
        fun fromBarcode(barcodeFormat: BarcodeFormat): CatimaBarcode {
            return CatimaBarcode(barcodeFormat)
        }

        @JvmStatic
        fun fromName(name: String): CatimaBarcode {
            return CatimaBarcode(BarcodeFormat.valueOf(name))
        }

        @JvmStatic
        fun fromPrettyName(prettyName: String): CatimaBarcode {
            try {
                return CatimaBarcode(
                    barcodeFormats[barcodePrettyNames.indexOf(
                        prettyName
                    )]
                )
            } catch (e: IndexOutOfBoundsException) {
                throw IllegalArgumentException("No barcode type with pretty name $prettyName known!")
            }
        }
    }
}
