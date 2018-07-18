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

/***
 * Activity to query Fakebox for results.
 * Displays the results when it comes back.
 * Acts as a share target that accepts ACTION_SEND intents for content to send to Fakebox.
 * If the intent comes from the Twitter app, grabs the body of the tweet using a Twitter API wrapper.
 */

class QueryActivity : AppCompatActivity() {

    // Twitter API stuff
    lateinit var mTwitterFactory: TwitterFactory

    // the Volley request queue for Fakebox and Factually requests
    lateinit var mRequestQueue: RequestQueue

    // vars for the recycler view to display results from Factually
    // doesn't play much of a role in the logic itself
    lateinit var mRecyclerView: RecyclerView
    private lateinit var mViewAdapter: RecyclerView.Adapter<*>
    private lateinit var mViewManager: RecyclerView.LayoutManager

    // array to hold results from Factually
    private var mSearchResults = JSONArray()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        setSupportActionBar(toolbar)

        // HACK: turning off NetworkOnMainThread errors
        // **not necessary after changing to request queue**
//        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
//        StrictMode.setThreadPolicy(policy)

        // Twitter api wrapper config
        val cb = ConfigurationBuilder()
        cb.apply {
            setOAuthConsumerKey("3rMrPuq810NIWWXOWKYoMgDCg")
            setOAuthConsumerSecret("atlQtEkheu3reH9JS1QfAxuCpNI9lIHxgB0Mdkoe17gok169OB")
            setOAuthAccessToken("2939202811-DxXRfaoktLcMUGfm4Rm3uZ1mYPJ0Nwk7a7Ed2Q2")
            setOAuthAccessTokenSecret("QIEJvegDlg2uL4ypJLKXQjfXznKy5Bb8rS0YQxoeyvUf8")
        }
        mTwitterFactory = TwitterFactory(cb.build())

        // initialise a volley queue for all the HTTP requests
        mRequestQueue = Volley.newRequestQueue(this)

        // gets the intent that started the activity, should be a ACTION_SEND
        val intent = getIntent()
        val action = intent.action
        val type = intent.type

        // check the Intent and see if it's _actually_ an ACTION_SEND (share) intent with text
        if (Intent.ACTION_SEND == action && type != null) {
            if (type == "text/plain") {
                handleShareIntent(intent)
            }
        }

        // set up the recycler (list) view that displays results from Factually
        mViewManager = LinearLayoutManager(this)
        mRecyclerView = findViewById<RecyclerView>(R.id.recyclerView).apply {
            setHasFixedSize(false)
            layoutManager = mViewManager
        }
    }

    /***
     * Handles the share intent that started the activity.
     */
    private fun handleShareIntent(intent: Intent) {
        Log.d("HandleShareIntent", intent.extras.keySet().reduce({ acc, s -> acc + " " + s }))
        Log.d("HandleShareIntent", intent.getStringExtra(Intent.EXTRA_TEXT))

        // text to send to Fakebox.
        // if the intent has a tweet_id attached to it, get the body of the tweet with the Twitter API wrapper.
        // otherwise, get either the SUBJECT (usually a webpage title) or a BODY (everything else)
        val queryText = if (intent.extras.containsKey("tweet_id")) {
            val twitter = mTwitterFactory.instance
            val status = twitter.showStatus(intent.extras.getLong("tweet_id"))
            status.text
        } else {
            if (intent.extras.containsKey(Intent.EXTRA_SUBJECT)) intent.getStringExtra(Intent.EXTRA_SUBJECT) else intent.getStringExtra(Intent.EXTRA_TEXT)
        }

        // Initialise the Fakebox request.
        // if the intent is a tweet or the SUBJECT exists, query it as a title.
        // otherwise, query it as content.
        // More details in FactChecker.kt
        val fc =
                if (intent.extras.containsKey("tweet_id") || intent.extras.containsKey(Intent.EXTRA_SUBJECT))
                    FactChecker(title = queryText, requestQueue = mRequestQueue)
                else
                    FactChecker(content = queryText, requestQueue = mRequestQueue)
        Log.d("handleShareIntent", queryText)
        // fire the request. after the request, onCheckerSuccess will be run with the result.
        fc.getCheckerResults(onCheckerSuccess,
                        onFail = { Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show() }
                )
    }


    // the definition for onCheckerSuccess and onFactuallyResutls looks a bit different from the normal fun definitions
    // they're supposed to be lambdas put in the function invocation itself, but it's clearer to bring it out
    // try out a kotlin tutorial if you're interested

    /**
     * The callback fired when the Fakebox request completes.
     * Mostly just setting views, but also fires the Factually request for results.
     */
    val  onCheckerSuccess = { results: CheckerResult ->
        Log.d("onCheckerSuccess:", results.score.toString())

        // set results in the view.
        val resultText = findViewById<TextView>(R.id.resultText) as TextView
        val scoreBar = findViewById<ProgressBar>(R.id.resultScore) as ProgressBar
        scoreBar.isIndeterminate = false
        scoreBar.progress = (results.score * 100L).toInt()
        resultText.text = if (results.isImpartial) "Impartial" else "Biased"

        // if results.keywords.size is zero, it's most likely that the Fakebox request was a TITLE request.
        // Fakebox only parses keywords for CONTENT requests.

        // this searches Factually for relevant content if keywords are returned.
        if (results.keywords.size != 0) {
            // custom google search url, with all the keywords joined together by "+"
            val url = "https://www.googleapis.com/customsearch/v1?key=AIzaSyCBlQ02bb_mTBGD6ODqkBB1vJ7SE2sU4Sw&cx=012550057659332407480:c89sf7f9nuw&q=site%3Awww.gov.sg%2Ffactually+${results.keywords.reduce { acc, s -> acc + "+" + s }}"
            Log.d("onCheckerSuccess", url)
            // add a simple request for Factually search; when it's done onFactuallyResults will be called.
            val request = JsonObjectRequest(Request.Method.GET, url, null, Response.Listener(onFactuallyResults), Response.ErrorListener(onFactuallyError))
            mRequestQueue.add(request)
        }
        // HACK: just return Unit b/c of weird kotlin typing
        Unit
    }

    /**
     * Handles results from Factually search.
     */
    val onFactuallyResults = { results: org.json.JSONObject ->
        // Try to get items from the result.
        try {
            // get all the results in the results
            mSearchResults = results.getJSONArray("items")

            // ...and pass it to a searchResultAdapter
            // look at onBindViewHolder() in SearchResultAdapter.kt to see what it does with the results.
            mViewAdapter = SearchResultAdapter(mSearchResults)

            // ...then attach the adapter to the recyclerView, in order to show the results in the view.
            mRecyclerView.adapter = mViewAdapter

            // ideally you're supposed to attach the adapter way before you actually get any data and just notify the adapter that the data changed,
            // but it wasn't working so i just put it here. ::shrug::

        // if there aren't any results and you try to get results.items, a JSONException is raised.
        } catch (e: JSONException) {
            Toast.makeText(this, "No results found.", Toast.LENGTH_SHORT).show()

        }
    }

    // generic catcher
    val onFactuallyError = { error: VolleyError ->
        Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show()
    }

    // extra boilerplate from the template that isn't ever used lmao
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
