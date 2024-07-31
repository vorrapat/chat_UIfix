package th.ac.rmutto.chatbot

import android.os.Bundle
import android.os.Handler
import android.os.StrictMode
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    var recyclerView: RecyclerView? = null
    var textViewMessage: EditText? = null
    val mHandler = Handler()
    var custID: Int? = null
    var empID: Int? = null
    var pastCount = 0
    var currentCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        //val sharedPrefer = getSharedPreferences("appPrefer", Context.MODE_PRIVATE)
        //custID = sharedPrefer?.getString("custIDPref", null)

        custID = 1

        recyclerView = findViewById(R.id.recyclerView)
        textViewMessage = findViewById(R.id.textViewMessage)
        val buttonSend: ImageButton = findViewById(R.id.buttonSend)

        showDataList()

        buttonSend.setOnClickListener {
            postMessage()
        }

        textViewMessage!!.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                postMessage()
                true
            } else {
                false
            }
        }
    }


    fun postMessage() {
        val url = getString(R.string.root_url) + getString(R.string.gpt_url)
        Log.d("tag",url)

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        val formBody: RequestBody = FormBody.Builder()
            .add("custID", custID.toString())
            .add("message", textViewMessage!!.text.toString())
            .build()
        val request: Request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if(response.isSuccessful){
            val obj = JSONObject(response.body!!.string())
            val status = obj["status"].toString()
            if (status == "true") {
                showDataList()
                textViewMessage!!.setText("")
            }
        }else{
            Toast.makeText(this, "ไม่สามารถเชื่อต่อกับเซิร์ฟเวอร์ได้", Toast.LENGTH_LONG).show()
        }
    }

    
    fun showDataList() {
        val custID = custID.toString()
        val data = ArrayList<Data>()
        val url: String = getString(R.string.root_url) + getString(R.string.gpt_show_url) + custID
        val okHttpClient = OkHttpClient()

        val request: Request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()

        if (response.isSuccessful) {
            val res = JSONArray(response.body!!.string())
            if (res.length() > 0) {

                for (i in 0 until res.length()) {
                    val item: JSONObject = res.getJSONObject(i)
                    data.add(
                        Data(
                            item.getString("message"),
                            item.getString("chatTime"),
                            item.getString("sender")
                        )
                    )

                    // Log.e("tag", item.getString("orderid"))
                }

                currentCount = data.count()
                if(pastCount != currentCount) {
                    recyclerView!!.adapter = DataAdapter(data)
                    recyclerView!!.scrollToPosition(data.count() - 1)
                    pastCount = currentCount
                }

            }
        } else {
            Toast.makeText(this, "ไม่สามารถเชื่อต่อกับเซิร์ฟเวอร์ได้", Toast.LENGTH_LONG).show()
        }
    }


    internal class Data(
        var message: String, var chatTime: String,
        var sender: String
    )

    internal inner class DataAdapter(private val list: List<Data>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemViewType(position: Int): Int {
            val data = list[position]
            if(data.sender == "c"){
                return 1
            }else{
                return 0
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            if (viewType === 1) {
                val view: View = inflater.inflate(R.layout.item_chat_customer, parent, false)
                return CustomerViewHolder(view)
            } else {
                val view: View = inflater.inflate(R.layout.item_chat_bot, parent, false)
                return EmployeeViewHolder(view)
            }

        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val data = list[position]

            if (holder.itemViewType === 1) {
                var holder = holder as CustomerViewHolder

                holder.textViewMessage.text = data.message
                holder.textViewchatTime.text = data.chatTime
            } else {
                var holder = holder as EmployeeViewHolder

//                var url = getString(R.string.root_url) +
//                        getString(R.string.employee_image_url) + data.imageFile
//                Picasso.get().load(url).into(holder.imageView)

                val markwon = Markwon.create(applicationContext)
                markwon.setMarkdown(holder.textViewMessage, data.message)

                //holder.textViewMessage.text = data.message
                holder.textViewchatTime.text = data.chatTime
            }

        }

        override fun getItemCount(): Int {
            return list.size
        }

        internal inner class CustomerViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            var textViewMessage: TextView = itemView.findViewById(R.id.textViewMessage)
            var textViewchatTime: TextView = itemView.findViewById(R.id.textViewChatTime)
        }

        internal inner class EmployeeViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            //var imageView: ImageView = itemView.findViewById(R.id.imageView)
            var textViewMessage: TextView = itemView.findViewById(R.id.textViewMessage)
            var textViewchatTime: TextView = itemView.findViewById(R.id.textViewChatTime)
        }
    }


    private val mRunnable = object : Runnable {
        override fun run() {
            // Code to reload the RecyclerView
            if(currentCount>0) {
                showDataList()
                recyclerView!!.adapter!!.notifyDataSetChanged()
                mHandler.postDelayed(this, 2000) // Reload every 10 seconds
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mHandler.postDelayed(mRunnable, 2000)
    }

    override fun onPause() {
        super.onPause()
        mHandler.removeCallbacks(mRunnable)
    }
}