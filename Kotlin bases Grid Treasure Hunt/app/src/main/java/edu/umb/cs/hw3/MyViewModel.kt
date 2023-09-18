package edu.umb.cs.hw3

import android.util.Log
import android.widget.ArrayAdapter
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.umb.cs.hw3.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList

class MyViewModel: ViewModel() {
    private var counterJob: Job?=null

    private val r = Random()
    val w = 5
    var tiles = ArrayList<String>(w * w)
    val timeout=20

    var curx=0
    var cury=0

    var _treaureOnFloorCount = MutableLiveData (0)
    val treaureCount: LiveData<Int>
        get() = _treaureOnFloorCount

    var _treasures = MutableLiveData (0)
    val treasuresCollected: LiveData<Int>
        get() = _treasures

    var _tiles = MutableLiveData (0)
    val tilesCollected: LiveData<Int>
        get() = _tiles

    var _highScore = MutableLiveData (0)
    val highScore: LiveData<Int>
        get() = _highScore

    var _latestScores = MutableLiveData<String>("(displays here)")
    val latestScores: LiveData<String>
        get() = _latestScores

    var playerPosition: Int? = null

    /*********/
    init{
        myInit()
    }
    var _counter = MutableLiveData (0)
    val counter: LiveData<Int>
        get() = _counter

    fun myInit(){
        tiles.clear()
        repeat (w*w) {
            tiles.add(" ")
        }
        curx = r.nextInt(w)
        cury = r.nextInt(w)
        playerPosition = 0
        playerPosition = (cury * w + curx)
        var i = 0
        tiles.set(cury * w + curx, "O")

        _treaureOnFloorCount.value = 0
        _treasures.value = 0
        _tiles.value = 0
//        _latestScores.value = "(list displays here)"
//        _highScore.value = 0
    }
    fun setCounter(i:Int){_counter.value=i}

    fun decCounter(){
        _counter.value = _counter.value?.minus(1)
    }


    fun counter1() {
        viewModelScope.launch{
            initTimer()
            while(counter.value!! >0){
                delay(1000)
                decCounter()
            }
        }
    }

    fun validateTreasure(position: Int, binding: ActivityMainBinding): Int {
        if(counterJob?.isActive == true) CoroutineScope(Dispatchers.Main).launch {

            var currentX = position % 5
            var currentY = position / 5

            var playerX = playerPosition?.toInt()?.mod(5)
            var playerY = playerPosition?.toInt()?.div(5)

            Log.i(
                "MYTAG",
                "Player position: " + playerPosition + " (" + playerX + "," + playerY + ")"
            )
            Log.i("MYTAG", "Desired position: " + position + " (" + currentX + "," + currentY + ")")

            while (playerPosition != position) {

                tiles.set(playerPosition!!, " ")
                when {
                    playerX!! < currentX -> {
                        playerX = playerX + 1
                    }
                    playerX!! > currentX -> {
                        playerX = playerX - 1
                    }
                    playerY!! < currentY -> {
                        playerY = playerY + 1
                    }
                    playerY!! > currentY -> {
                        playerY = playerY - 1
                    }
                }
                playerPosition = (playerY!! * w + playerX!!)
                _tiles.value = _tiles.value?.plus(1)

                Log.i(
                    "MYTAG",
                    "Player Navigation Path: " + (playerY * w + playerX).toString() + " (" + playerX + "," + playerY + ")"
                )

                if(counterJob?.isActive!= true) {
                    tiles.set(playerPosition!!, "O")
                    (binding.gridView1.adapter as ArrayAdapter<String>).notifyDataSetChanged()
                    break
                }

                if (tiles.get(playerPosition!!).contains("X")) {
                    _treasures.value = _treasures.value?.plus(1)
                    _treaureOnFloorCount.value = _treaureOnFloorCount.value?.minus(1)
                    generateTreasures()
                    Log.i("MYTAG", "Captured Treasure")
                    tiles.set(playerPosition!!, "O")
                    (binding.gridView1.adapter as ArrayAdapter<String>).notifyDataSetChanged()
                    delay(500)
                    if (playerPosition!! != position) {
                        tiles.set(playerPosition!!, " ")
                        (binding.gridView1.adapter as ArrayAdapter<String>).notifyDataSetChanged()
                    }
                    continue
                }
                tiles.set(playerPosition!!, "O")
                (binding.gridView1.adapter as ArrayAdapter<String>).notifyDataSetChanged()
                delay(500)
            }
        }

        return _treasures.value!!.toInt()
    }

    fun updateHighScore(highScore: Int) {
        _highScore.value = highScore
    }

    fun generateTreasures() {
        while(_treaureOnFloorCount.value!! < 4 && counterJob?.isActive == true)
        {
            curx = r.nextInt(w)
            cury = r.nextInt(w)
            if(tiles.get(cury * w + curx).contains("X").or(tiles.get(cury * w + curx).contains("O"))) {
                continue
            }
            tiles[cury * w + curx] = "X"
            _treaureOnFloorCount.value = _treaureOnFloorCount.value?.plus(1)
        }
    }

    fun genTreasure(binding: ActivityMainBinding) {
        viewModelScope.launch {
            while(_treaureOnFloorCount.value!! < 4)
            {
                curx = r.nextInt(w)
                cury = r.nextInt(w)
                if(tiles.get(cury * w + curx).contains("X").or(tiles.get(cury * w + curx).contains("O"))) {
                    continue
                }
                delay(500)
                tiles[cury * w + curx] = "X"
                (binding.gridView1.adapter as ArrayAdapter<String>).notifyDataSetChanged()
                _treaureOnFloorCount.value = _treaureOnFloorCount.value?.plus(1)
            }
        }
    }

    fun counter2(dataStore: DataStore<Preferences>){
        if(counterJob?.isActive == true) counterJob?.cancel()
        counterJob=viewModelScope.launch{
            initTimer()
            try {
                repeat(timeout) {
                    delay(1000)
                    decCounter()
                }
            }catch (e:CancellationException){}
        }
    }

    fun counter3(binding: ActivityMainBinding) {
        viewModelScope.launch(Dispatchers.Default){
            var i=0
            println(Thread.currentThread().name)
            while(i <=timeout){
                delay(1000)
                //Thread.sleep(1000)
                //decCounter()
                withContext(Dispatchers.Main) {
                    binding.textView2.text = (timeout - i).toString()
                }
                i++
            }
        }
    }

    fun initTimer(){
        setCounter(timeout);
    }

    fun getHighestScore() : Int {
        if(counterJob?.isActive != true) {
            return _highScore.value!!
        }
        else return 0
    }

    fun updateLatestScores(Scores: String)
    {
        Log.i("MYTAG", "Here" + Scores)
        _latestScores.value = Scores
        Log.i("MYTAG", "Here" + _latestScores.value)
    }

}