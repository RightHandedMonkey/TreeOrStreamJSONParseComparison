package com.example.android.recyclerview

import android.os.Bundle
import android.os.Debug
import android.provider.Contacts
import android.support.annotation.MainThread
import android.support.annotation.WorkerThread
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import com.example.android.common.logger.Log
import com.example.android.json.MediumJsonStream
import com.example.android.json.MediumJsonTree
import com.example.android.json.ParseInterface
import com.example.android.json.SmallJsonTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.lang.System.nanoTime

/**
 * Demonstrates the use of [RecyclerView] with a [LinearLayoutManager] and a
 * [GridLayoutManager].
 */
class RecyclerViewFragment : Fragment() {
    private enum class DocEndpoint(val url: String) {
        Small("https://www.chapelhillopendata.org/api/v2/catalog/datasets/census-diversity"),
        Medium("https://opendata.maryland.gov/api/views/ryxx-aeaf/rows.json?accessType=DOWNLOAD"),
        Large("https://data.ny.gov/api/views/tk82-7km5/rows.json?accessType=DOWNLOAD")
    }

    private lateinit var currentLayoutManagerType: LayoutManagerType
    private lateinit var recyclerView: RecyclerView
    private lateinit var spinner: Spinner
    private lateinit var textView: TextView
    private lateinit var layoutManager: RecyclerView.LayoutManager
    private lateinit var dataset: ArrayList<String>
    var timerMS = 0L
    var logText = ""

    private fun updateTimeMS() {
        timerMS = nanoTime() / 1000000
    }

    private fun diffTimeMS(): Long {
        val timeMSNew = nanoTime() / 1000000
        val diffMS = timeMSNew - timerMS
        updateTimeMS()
        return diffMS
    }

    private fun getHeapMB(): Long {
        val nativeHeapSize = Debug.getNativeHeapSize()
        val nativeHeapFreeSize = Debug.getNativeHeapFreeSize()
        return (nativeHeapSize - nativeHeapFreeSize) / (1024 * 1024)
    }

    var client = OkHttpClient()

    enum class LayoutManagerType { GRID_LAYOUT_MANAGER, LINEAR_LAYOUT_MANAGER }

    override fun onCreate(savedInstanceState: Bundle?) {
        updateTimeMS()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.recycler_view_frag,
                container, false).apply { tag = TAG }

        recyclerView = rootView.findViewById(R.id.recyclerView)
        spinner = rootView.findViewById(R.id.spinner)
        textView = rootView.findViewById(R.id.speedResults)

        // LinearLayoutManager is used here, this will layout the elements in a similar fashion
        // to the way ListView would layout elements. The RecyclerView.LayoutManager defines how
        // elements are laid out.
        layoutManager = LinearLayoutManager(activity)

        currentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER

        if (savedInstanceState != null) {
            // Restore saved layout manager type.
            currentLayoutManagerType = savedInstanceState
                    .getSerializable(KEY_LAYOUT_MANAGER) as LayoutManagerType
        }
        setRecyclerViewLayoutManager(currentLayoutManagerType)

        // Set CustomAdapter as the adapter for RecyclerView.
        //recyclerView.adapter = CustomAdapter(dataset)

        rootView.findViewById<RadioButton>(R.id.tree_button).setOnClickListener {
            // initTreeList()
            GlobalScope.launch {
                fetchList(getSelectedSize(), false)
            }
        }

        rootView.findViewById<RadioButton>(R.id.stream_button).setOnClickListener {
            // initStreamList()
            GlobalScope.launch {
                fetchList(getSelectedSize(), true)
            }
        }

        return rootView
    }

    @MainThread
    private suspend fun fetchList(listSize: DocEndpoint, streamParse: Boolean) {
        dataset = fetchData(listSize, streamParse) as ArrayList<String>
        withContext(Dispatchers.Main) {
            updateData(dataset)
        }
    }


    private fun updateData(dataset: List<String>) {
        recyclerView.adapter = CustomAdapter(dataset)
        textView.text = logText
    }

    private fun getSelectedSize(): DocEndpoint {
        return when (spinner.selectedItemPosition) {
            0 -> DocEndpoint.Small
            1 -> DocEndpoint.Medium
            2 -> DocEndpoint.Large
            else -> DocEndpoint.Small
        }
    }

    private suspend fun fetchData(listSize: DocEndpoint, stream: Boolean): List<String> {
        logText = ""
        var newArray: List<String> = arrayListOf("")
        withContext(Dispatchers.IO) {
            newArray = initTreeList(stream)
        }
        return newArray
    }

    @WorkerThread
    fun initTreeList(stream: Boolean): List<String> {
        updateTimeMS()
        Log.d("PTK", "Start network call, time: " + timerMS)
        val inputStream = getURLStream(getSelectedSize().url)
        logText += "Initial heap MB: " + getHeapMB() + "\r\n"

        var diffMS = diffTimeMS()
        Log.d("PTK", "Start network call, time: " + timerMS + ", diff: " + diffMS)
        logText += "Time to get Stream from network MS: " + diffMS + "\r\n"
        val jsonParser = getParser(stream)
        val list = jsonParser.parse(inputStream)
        inputStream.close()
        diffMS = diffTimeMS()
        Log.d("PTK", "Close network call, time: " + timerMS + ", diff: " + diffMS)
        logText += "Time to close Stream from network MS: " + diffMS + "\r\n"
        logText += "Initial heap MB: " + getHeapMB() + "\r\n"
        logText += jsonParser.getNetTimeAndParseTime()
        logText += "Total items: "+ list.size+"\r\n"
        return list //arrayListOf("Tree item 1", "Tree item 2")
    }

    fun getParser(stream: Boolean): ParseInterface {
        return when (spinner.selectedItemPosition) {
            0 -> if (stream) SmallJsonTree() else SmallJsonTree()
            1 -> if (stream) MediumJsonStream() else MediumJsonTree()
            2 -> if (stream) MediumJsonStream() else MediumJsonTree()
            else -> SmallJsonTree()
        }
    }

    fun getURLStream(url: String): InputStream {
        val request = Request.Builder()
                .url(url)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build()
        val call = client.newCall(request)
        return call.execute().body?.byteStream() ?: "".byteInputStream()
    }

    /**
     * Set RecyclerView's LayoutManager to the one given.
     *
     * @param layoutManagerType Type of layout manager to switch to.
     */
    private fun setRecyclerViewLayoutManager(layoutManagerType: LayoutManagerType) {
        var scrollPosition = 0

        // If a layout manager has already been set, get current scroll position.
        if (recyclerView.layoutManager != null) {
            scrollPosition = (recyclerView.layoutManager as LinearLayoutManager)
                    .findFirstCompletelyVisibleItemPosition()
        }

        when (layoutManagerType) {
            RecyclerViewFragment.LayoutManagerType.GRID_LAYOUT_MANAGER -> {
                layoutManager = GridLayoutManager(activity, SPAN_COUNT)
                currentLayoutManagerType = LayoutManagerType.GRID_LAYOUT_MANAGER
            }
            RecyclerViewFragment.LayoutManagerType.LINEAR_LAYOUT_MANAGER -> {
                layoutManager = LinearLayoutManager(activity)
                currentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER
            }
        }

        with(recyclerView) {
            layoutManager = this@RecyclerViewFragment.layoutManager
            scrollToPosition(scrollPosition)
        }

    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {

        // Save currently selected layout manager.
        savedInstanceState.putSerializable(KEY_LAYOUT_MANAGER, currentLayoutManagerType)
        super.onSaveInstanceState(savedInstanceState)
    }

    companion object {
        private val TAG = "RecyclerViewFragment"
        private val KEY_LAYOUT_MANAGER = "layoutManager"
        private val SPAN_COUNT = 2
        private val DATASET_COUNT = 60
    }
}

