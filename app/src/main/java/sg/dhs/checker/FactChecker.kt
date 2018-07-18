package sg.dhs.checker

import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject

data class CheckerResult(val keywords: Array<String>, val isImpartial: Boolean, val score: Double)

class FactChecker(val title: String = "", val content: String = "", val requestQueue: RequestQueue) {


    fun getCheckerResults(onSuccess: (CheckerResult) -> Unit, onFail: () -> Unit) {
        val url = "http://192.168.43.118:8080/fakebox/check"
        val requestBody = JSONObject(" { \"content\": \"${content}\", \"title\": \"$title\" } ")

        val request = JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,
                { response: JSONObject ->
                    Log.d("JSON received", response.toString())
                    val titleResults = response.getJSONObject("title")
                    val contentResults = response.getJSONObject("content")
                    if (titleResults.length() != 0) {
                        // request was made as title, extract isImpartial and score only
                        val isImpartial = titleResults.getString("decision") == "impartial"
                        val score = titleResults.getDouble("score")
                        onSuccess(CheckerResult(isImpartial = isImpartial, score = score, keywords = emptyArray()))
                    } else if (contentResults.length() != 0) {
                        var keywords = emptyArray<String>()
                        // extract all keywords from keywords array
                        val allKeywords = contentResults.getJSONArray("keywords")
                        for (i in 0..(allKeywords.length() - 1)) {
                            keywords += allKeywords.getJSONObject(i).getString("keyword")
                        }
                        val isImpartial = contentResults.getString("decision") == "impartial"
                        val score = contentResults.getDouble("score")
                        onSuccess(CheckerResult(keywords = keywords, isImpartial = isImpartial, score = score))
                    }
                },
                { _ -> onFail() }
        )
        requestQueue.add(request)

    }
}