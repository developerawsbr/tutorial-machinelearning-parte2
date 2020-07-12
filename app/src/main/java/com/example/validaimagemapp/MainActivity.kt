package com.example.validaimagemapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var btn: Button
    private lateinit var txv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn = findViewById<Button>(R.id.btnCarregarImagem);
        txv = findViewById<TextView>(R.id.txvSelecione);
        btn.setOnClickListener{pickImage()}
    }

    private fun pickImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Selecione uma imagem"), 2)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 2) {
            val source = ImageDecoder.createSource(this.contentResolver, data!!.data!!)
            val bmp = ImageDecoder.decodeBitmap(source)
            var output = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, output)
            val imgData = output.toByteArray()
            analizeImage(Base64.encodeToString(imgData, Base64.NO_WRAP))
        }
    }

    private fun analizeImage(imgData: String) {
        btn.isEnabled = false
        btn.setBackgroundColor(Color.GRAY)
        txv.setText("Analisando imagem. Aguarde...")

        Fuel.post("api-url")
            .header("Content-Type", "application/json")
            .jsonBody("{\"image\": \"$imgData\"}")
            .responseString { result ->
                when (result) {
                    is Result.Success -> {
                        validateImage(result.get())
                    }
                    is Result.Failure -> {
                        Log.d("App", result.getException().toString())
                    }
                }
            }
    }

    private fun validateImage(results: String) {
        val json = JSONObject(results)
        val data = json.getJSONObject("data")
        val labels = data.getJSONArray("Labels")
        var isValid = false

        for (i in 0 until labels.length()) {
            val item = labels.getJSONObject(i)
            if (item.getString("Name") in arrayOf("Automobile", "Vehicle")) {
                if (item.getDouble("Confidence") > 90.0) {
                    isValid = true
                    break
                }
            }
        }

        runOnUiThread(Runnable {
            btn.isEnabled = true
            btn.setBackgroundColor(Color.BLUE)

            if (isValid) {
                txv.setText("A imagem é válida")
            } else {
                txv.setText("A imagem é inválida")
            }
        })
    }
}