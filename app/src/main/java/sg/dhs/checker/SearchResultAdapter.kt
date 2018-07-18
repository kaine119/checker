package sg.dhs.checker

import android.content.Intent
import android.net.Uri
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import org.json.JSONArray

/**
 * Created by student on 14/7/2018.
 */
class SearchResultAdapter(val results: JSONArray) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {
    class ViewHolder(val layoutView: ConstraintLayout) : RecyclerView.ViewHolder(layoutView)

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        holder?.layoutView?.findViewById<TextView>(R.id.resultTitle)?.text = results.getJSONObject(position).getString("title")
        holder?.layoutView?.findViewById<TextView>(R.id.resultDescription)?.text = results.getJSONObject(position).getString("snippet")
        holder?.layoutView?.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW)
            browserIntent.setData(Uri.parse(results.getJSONObject(position).getString("link")))
            it.context.startActivity(browserIntent)
        }
    }

    override fun getItemCount(): Int = results.length()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val container = LayoutInflater.from(parent.context)
                .inflate(R.layout.factually_result_row, parent, false) as ConstraintLayout
        return ViewHolder(container)
    }



}