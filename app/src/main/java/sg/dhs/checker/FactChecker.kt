package sg.dhs.checker

import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject

/**
 * A simple class that holds results from Fakebox.
 * This class is needed as opposed to passing data around in a dictionary because strong typing.
 */

data class CheckerResult(val keywords: Array<String>, val isImpartial: Boolean, val score: Double)


/**
 * NOT AN ACTIVITY!
 * This is a utility class that helps with all the stuff needed for querying Fakebox.
 */
class FactChecker(val title: String = "", val content: String = "", val requestQueue: RequestQueue) {

    // after initializing the query, this fires the request to Fakebox.
    // the function takes two arguements: the callbacks for if the request succeeds with results, and for if the request fails
    fun getCheckerResults(onSuccess: (CheckerResult) -> Unit, onFail: () -> Unit) {
        // hardcoded to the IP address of my macbook on the day.
        val url = "http://192.168.43.118:8080/fakebox/check"

        // the data passed to Fakebox for checking.
        val requestBody = JSONObject(" { \"content\": \"${content}\", \"title\": \"$title\" } ")

        // one big-ass request
        val request = JsonObjectRequest(
                Request.Method.POST, // the HTTP method used to query Fakebox
                url, // where Fakebox is
                requestBody, // what to query Fakebox with

                // this is run if the request returns successfully
                // it's a function that takes the response and parses it nicely into the data class defined above
                { response: JSONObject ->
                    Log.d("JSON received", response.toString())

                    val titleResults = response.getJSONObject("title")
                    val contentResults = response.getJSONObject("content")

                    if (titleResults.length() != 0) {
                        // request was made as title, extract isImpartial and score only
                        val isImpartial = titleResults.getString("decision") == "impartial"
                        val score = titleResults.getDouble("score")

                        // call the success callback with the result as a nice data class, passing data back up to the query activity
                        // keywords is passed as an empty array, so the query activity checks if the keywords array is zero
                        onSuccess(CheckerResult(isImpartial = isImpartial, score = score, keywords = emptyArray()))

                    } else if (contentResults.length() != 0) {
                        // result was made as content, extract everything including keywords
                        var keywords = emptyArray<String>()

                        // extract all keywords from keywords array
                        val allKeywords = contentResults.getJSONArray("keywords")
                        for (i in 0..(allKeywords.length() - 1)) {
                            keywords += allKeywords.getJSONObject(i).getString("keyword")
                        }

                        // extract isImpartial and score, same way as above
                        val isImpartial = contentResults.getString("decision") == "impartial"
                        val score = contentResults.getDouble("score")

                        // calls the success callback with the results, same as above
                        onSuccess(CheckerResult(keywords = keywords, isImpartial = isImpartial, score = score))
                    }
                },

                // if the request fails, just call the fail callback
                // the `_` is a placeholder for the error that is supposed to be handled, but who cares about that?
                { _ -> onFail() }
        )

        // fire the request using the requestqueue passed in from the queryactivity.
        requestQueue.add(request)

    }
}