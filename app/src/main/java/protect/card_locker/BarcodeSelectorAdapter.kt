package protect.card_locker

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import protect.card_locker.async.TaskHandler
import protect.card_locker.databinding.BarcodeLayoutBinding

class BarcodeSelectorAdapter(
    context: Context,
    barcodes: ArrayList<CatimaBarcodeWithValue?>,
    private val mListener: BarcodeSelectorListener
) :
    ArrayAdapter<CatimaBarcodeWithValue?>(context, 0, barcodes) {
    private val mTasks = TaskHandler()

    private class ViewHolder {
        var image: ImageView? = null
        var text: TextView? = null
    }

    interface BarcodeSelectorListener {
        fun onRowClicked(inputPosition: Int, view: View)
    }

    fun setBarcodes(barcodes: ArrayList<CatimaBarcodeWithValue>) {
        clear()
        addAll(barcodes)
        notifyDataSetChanged()
        mTasks.flushTaskList(TaskHandler.TYPE.BARCODE, true, false, false)
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view
        val catimaBarcodeWithValue = getItem(position)
        val catimaBarcode = catimaBarcodeWithValue!!.catimaBarcode()
        val value = catimaBarcodeWithValue.value()

        val viewHolder: ViewHolder
        if (convertView == null) {
            viewHolder = ViewHolder()
            val inflater = LayoutInflater.from(context)
            val barcodeLayoutBinding = BarcodeLayoutBinding.inflate(inflater, parent, false)
            convertView = barcodeLayoutBinding.root
            viewHolder.image = barcodeLayoutBinding.barcodeImage
            viewHolder.text = barcodeLayoutBinding.barcodeName
            convertView.setTag(viewHolder)
        } else {
            viewHolder = convertView.tag as ViewHolder
        }

        createBarcodeOption(viewHolder.image!!, catimaBarcode.format().name, value, viewHolder.text)

        val finalConvertView: View = convertView
        convertView.setOnClickListener(View.OnClickListener { view: View? ->
            mListener.onRowClicked(
                position,
                finalConvertView
            )
        })

        return convertView
    }

    fun isValid(view: View): Boolean {
        val viewHolder = view.tag as ViewHolder
        return viewHolder.image!!.tag != null && viewHolder.image!!.tag as Boolean
    }

    private fun createBarcodeOption(
        image: ImageView,
        formatType: String,
        cardId: String,
        text: TextView?
    ) {
        val format = CatimaBarcode.fromName(formatType)

        image.setImageBitmap(null)
        image.clipToOutline = true

        if (image.height == 0) {
            // The size of the ImageView is not yet available as it has not
            // yet been drawn. Wait for it to be drawn so the size is available.
            image.viewTreeObserver.addOnGlobalLayoutListener(
                object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        Log.d(
                            TAG,
                            "Global layout finished, type: + " + formatType + ", width: " + image.width
                        )
                        image.viewTreeObserver.removeOnGlobalLayoutListener(this)

                        Log.d(
                            TAG,
                            "Generating barcode for type $formatType"
                        )

                        val barcodeWriter = BarcodeImageWriterTask(
                            context,
                            image,
                            cardId,
                            format,
                            text,
                            true,
                            null,
                            true,
                            false
                        )
                        mTasks.executeTask(TaskHandler.TYPE.BARCODE, barcodeWriter)
                    }
                })
        } else {
            Log.d(
                TAG,
                "Generating barcode for type $formatType"
            )
            val barcodeWriter = BarcodeImageWriterTask(
                context,
                image,
                cardId,
                format,
                text,
                true,
                null,
                true,
                false
            )
            mTasks.executeTask(TaskHandler.TYPE.BARCODE, barcodeWriter)
        }
    }

    companion object {
        private const val TAG = "Catima"
    }
}
