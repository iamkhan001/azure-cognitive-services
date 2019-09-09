package com.nstudio.navigation.azurecognitivefaceapi

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import com.microsoft.projectoxford.face.contract.*
import com.microsoft.projectoxford.face.FaceServiceRestClient
import com.microsoft.projectoxford.face.FaceServiceClient
import android.graphics.*
import java.lang.StringBuilder
import android.os.Build




class MainActivity : AppCompatActivity() {

    companion object {
        @JvmStatic
        private val PICK_IMAGE = 1
    }

    private val apiEndpoint = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0"
    private val subscriptionKey = "358cd3ce3fc44fff9e454de01a3f4de3"

    private  var faceServiceClient =  FaceServiceRestClient(apiEndpoint, subscriptionKey)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkConfiguration()

        btnSelectPicture.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE)
        }
    }

    private fun checkConfiguration() {
        val conf = "Manufacture: ${Build.MANUFACTURER} Model: ${Build.MODEL} Version: ${Build.VERSION.SDK_INT} Release: ${Build.VERSION.RELEASE}"
        tvConfig.text = conf
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.data != null) {
            val uri = data.data

            try {
                val bitmap = getBitmapFromUri(uri)
                imgPhoto.setImageBitmap(bitmap)
                detectAndFrame(bitmap)

                btnSelectPicture.isEnabled = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }

    private fun detectAndFrame(bitmap: Bitmap) {
        tvResult.text = ""

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())

        progressBar.visibility = View.VISIBLE

        val faceAttributes = arrayOf(FaceServiceClient.FaceAttributeType.Age, FaceServiceClient.FaceAttributeType.Gender,FaceServiceClient.FaceAttributeType.Smile)

        Thread(Runnable {
            try {

                val result = faceServiceClient.detect(
                    inputStream,
                    true,
                    true,
                    faceAttributes
                )

                showResult(bitmap,result)

            } catch (e: Exception) {
                val result = String.format("Detection failed: %s", e.message)
                showError(result)
            }
        }).start()
    }

    private fun showResult(bitmap: Bitmap,result: Array<Face>) {
        runOnUiThread {
            btnSelectPicture.isEnabled = true
            progressBar.visibility = View.GONE
            imgPhoto.setImageBitmap(drawFaceRectanglesOnBitmap(bitmap, result))
            bitmap.recycle()
        }
    }

    private fun showError(result:String){
        runOnUiThread {
            btnSelectPicture.isEnabled = true
            progressBar.visibility = View.GONE
            tvResult.text = result
        }
    }

    private fun drawFaceRectanglesOnBitmap(originalBitmap: Bitmap, faces: Array<Face>?): Bitmap {


        val bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.color = Color.GREEN
        paint.strokeWidth = 5F
        if (faces != null) {
            var count = 1
            val result = StringBuilder()

            for (face in faces) {

                result.append("Face $count: ${face.faceId} " +
                        "\nAge :${face.faceAttributes.age}"+
                        "\nGender :${face.faceAttributes.gender}"+
                        "\nSmile :${face.faceAttributes.smile}"+"\n--------------\n"
                )

                val faceRectangle = face.faceRectangle
                canvas.drawRect(
                    faceRectangle.left.toFloat(),
                    faceRectangle.top.toFloat(),
                    (faceRectangle.left + faceRectangle.width).toFloat(),
                    (faceRectangle.top + faceRectangle.height).toFloat(),
                    paint
                )

                count+=1
            }

            tvResult.text = result.toString()
        }
        return bitmap
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap {
        val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")!!
        val fileDescriptor = parcelFileDescriptor.fileDescriptor
        val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        return image
    }
}
