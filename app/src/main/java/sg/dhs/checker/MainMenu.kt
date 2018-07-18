package sg.dhs.checker

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.widget.EditText

import kotlinx.android.synthetic.main.activity_main_menu.*
import kotlinx.android.synthetic.main.content_main_menu.*

class MainMenu : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->

            val queryIntent = Intent(Intent.ACTION_SEND)
            queryIntent.putExtra(Intent.EXTRA_TEXT, (queryButton as EditText).text.toString())
            queryIntent.setType("text/plain")
            startActivity(queryIntent)
        }
    }

}
