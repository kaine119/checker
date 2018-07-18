package sg.dhs.checker

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.JsonRequest
import com.android.volley.toolbox.Volley

import kotlinx.android.synthetic.main.activity_result.*
import kotlinx.android.synthetic.main.content_result.*
import kotlinx.android.synthetic.main.factually_result_row.*
import org.json.JSONArray
import org.json.JSONException
import twitter4j.JSONObject
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder

class QueryActivity : AppCompatActivity() {

    lateinit var mTwitterFactory: TwitterFactory
    lateinit var mRequestQueue: RequestQueue

    lateinit var mRecyclerView: RecyclerView
    private lateinit var mViewAdapter: RecyclerView.Adapter<*>
    private lateinit var mViewManager: RecyclerView.LayoutManager

    private var mSearchResults = JSONArray()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        setSupportActionBar(toolbar)

        // HACK: turning off NetworkOnMainThread errors
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val cb = ConfigurationBuilder()
        cb.apply {
            setOAuthConsumerKey("3rMrPuq810NIWWXOWKYoMgDCg")
            setOAuthConsumerSecret("atlQtEkheu3reH9JS1QfAxuCpNI9lIHxgB0Mdkoe17gok169OB")
            setOAuthAccessToken("2939202811-DxXRfaoktLcMUGfm4Rm3uZ1mYPJ0Nwk7a7Ed2Q2")
            setOAuthAccessTokenSecret("QIEJvegDlg2uL4ypJLKXQjfXznKy5Bb8rS0YQxoeyvUf8")
        }


        mTwitterFactory = TwitterFactory(cb.build())

        mRequestQueue = Volley.newRequestQueue(this)

        val intent = getIntent()
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if (type == "text/plain") {
                handleShareIntent(intent)
            }
        }


        mViewManager = LinearLayoutManager(this)

        mRecyclerView = findViewById<RecyclerView>(R.id.recyclerView).apply {
            setHasFixedSize(false)
            layoutManager = mViewManager
        }
    }

    private fun handleShareIntent(intent: Intent) {
        Log.d("HandleShareIntent", intent.extras.keySet().reduce({ acc, s -> acc + " " + s }))
        Log.d("HandleShareIntent", intent.getStringExtra(Intent.EXTRA_TEXT))

        val queryText = if (intent.extras.containsKey("tweet_id")) {
            val twitter = mTwitterFactory.instance
            val status = twitter.showStatus(intent.extras.getLong("tweet_id"))
            status.text
        } else {
            if (intent.extras.containsKey(Intent.EXTRA_SUBJECT)) intent.getStringExtra(Intent.EXTRA_SUBJECT) else intent.getStringExtra(Intent.EXTRA_TEXT)
        }

        val fc =
                if (intent.extras.containsKey("tweet_id") || intent.extras.containsKey(Intent.EXTRA_SUBJECT))
                    FactChecker(title = queryText, requestQueue = mRequestQueue)
                else
                    FactChecker(content = queryText, requestQueue = mRequestQueue)
        Log.d("handleShareIntent", queryText)
        fc.getCheckerResults(onCheckerSuccess,
                        onFail = { Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show() }
                )
    }

    val  onCheckerSuccess = { results: CheckerResult ->
        Log.d("onCheckerSuccess:", results.score.toString())
        val resultText = findViewById<TextView>(R.id.resultText) as TextView
        val scoreBar = findViewById<ProgressBar>(R.id.resultScore) as ProgressBar
        scoreBar.isIndeterminate = false
        scoreBar.progress = (results.score * 100L).toInt()
        resultText.text = if (results.isImpartial) "Impartial" else "Biased"

        if (results.keywords.size != 0) {

            val url = "https://www.googleapis.com/customsearch/v1?key=AIzaSyCBlQ02bb_mTBGD6ODqkBB1vJ7SE2sU4Sw&cx=012550057659332407480:c89sf7f9nuw&q=site%3Awww.gov.sg%2Ffactually+${results.keywords.reduce { acc, s -> acc + "+" + s }}"
            Log.d("onCheckerSuccess", url)
            val request = JsonObjectRequest(Request.Method.GET, url, null, Response.Listener(onFactuallyResults), Response.ErrorListener(onFactuallyError))
            mRequestQueue.add(request)
        }
        Unit
    }

    val onFactuallyResults = { results: org.json.JSONObject ->
        try {
            mSearchResults = results.getJSONArray("items")
            mViewAdapter = SearchResultAdapter(mSearchResults)

            mRecyclerView.adapter = mViewAdapter
        } catch (e: JSONException) {
            Toast.makeText(this, "No results found.", Toast.LENGTH_SHORT).show()

        }
    }

    val onFactuallyError = { error: VolleyError ->
        Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

}
