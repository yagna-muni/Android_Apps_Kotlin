package edu.umb.cs.hw3

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import edu.umb.cs.hw3.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class MainActivity : AppCompatActivity() {
    var highScorevalue = 0
    var list = mutableListOf<Int>()
    var sortedListofScores = mutableListOf<Int>()
    private val viewModel: MyViewModel by viewModels()
    lateinit var binding: ActivityMainBinding
    val HIGHEST_SCORE = intPreferencesKey("highest_score")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.viewmodel = viewModel
        binding.lifecycleOwner = this

        var curHS = dataStore.data.map { it[HIGHEST_SCORE] }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                curHS.first()?.let { score ->
                    list.add(score)
                    highScorevalue = score
                    viewModel.updateHighScore(score)
                    Log.i("TAG", "Hero $score")
                } ?: run {
                    Log.e("TAG", "High score is null")
                }
            } catch (e: Exception) {
                Log.e("TAG", "Error fetching high score: ${e.localizedMessage}")
            }
        }

        binding.button2.setOnClickListener {
            GlobalScope.launch {
                dataStore.edit { it[HIGHEST_SCORE] = 0 }
            }
            viewModel.updateHighScore(0)
        }

        val gridView = binding.gridView1
        val adapter = ArrayAdapter(this, R.layout.list_item, R.id.textView, viewModel.tiles)
        gridView.adapter = adapter

        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            viewModel.validateTreasure(position, binding)?.let { returnHighScore ->
                Log.i("MYTAG", "Return Score: $returnHighScore")
                list.add(viewModel.getHighestScore())
                sortedListofScores = list.sortedDescending().distinct().toMutableList()

                try {
                    val slicedSortedArrays = sortedListofScores.slice(0..4)
                    viewModel.updateLatestScores(slicedSortedArrays.toString())
                } catch (e: RuntimeException) {
                    viewModel.updateLatestScores(sortedListofScores.toString())
                }

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val safeReturnHighScore = returnHighScore ?: 0
                        val safeHighScoreValue = highScorevalue ?: 0

                        if (safeReturnHighScore > safeHighScoreValue) {
                            GlobalScope.launch {
                                dataStore.edit {
                                    it[HIGHEST_SCORE] = safeReturnHighScore
                                    Log.i("MYTAG", "set $safeReturnHighScore")
                                }
                            }
                            viewModel.updateHighScore(safeReturnHighScore)
                        }
                    } catch (e: Exception) {
                        Log.e("MYTAG", "Error updating high score: ${e.localizedMessage}")
                    }
                }
            } ?: Log.e("MYTAG", "returnHighScore is null")
        }

        binding.button.setOnClickListener {
            viewModel.myInit()
            viewModel.genTreasure(binding)
            (binding.gridView1.adapter as ArrayAdapter<String>).notifyDataSetChanged()
            viewModel.counter2(dataStore)

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    highScorevalue = curHS.first()!!.toInt()
                    viewModel.updateHighScore(curHS.first()!!.toInt())
                } catch (e: Exception) {
                    Log.e("TAG", "Error updating counter high score: ${e.localizedMessage}")
                }
            }
        }
    }
}
