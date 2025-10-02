package com.example.superphoto.ui.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.superphoto.R
import com.example.superphoto.adapter.PhotoCardAdapter
import com.example.superphoto.model.PhotoCard
import com.example.superphoto.ui.activities.MainActivity
import com.example.superphoto.ui.activities.TemplateVideoActivity

class SearchFragment : Fragment() {

    private lateinit var searchEditText: EditText
    private lateinit var backButton: ImageView
    private lateinit var tvSearch: TextView
    private lateinit var scrollSearch: ScrollView
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var searchResultsAdapter: PhotoCardAdapter
    private var initialSearchText: String? = null

    private val allPhotoCards = listOf(
        PhotoCard("1", "Dance on street", "🔥 Hot", R.drawable.img1),
        PhotoCard("2", "Magic Elf", "✨ Premium", R.drawable.img2),
        PhotoCard("3", "Dance at sunset", "🔥 Hot", R.drawable.img3),
        PhotoCard("4", "AI Lion", "✨ Premium", R.drawable.img4),
        PhotoCard("5", "Heart Hands", "💖 Popular", R.drawable.img5),
        PhotoCard("6", "Magic Heads", "✨ Premium", R.drawable.img6),
        PhotoCard("7", "Character Style", "🎨 New", R.drawable.img7),
        PhotoCard("8", "Motor Couple", "🔥 Hot", R.drawable.img8),
        PhotoCard("9", "City Lights", "✨ New", R.drawable.img9),
        PhotoCard("10", "Nature Scene", "🌿 Nature", R.drawable.img10),
        PhotoCard("11", "Portrait Art", "🎨 Art", R.drawable.img11),
        PhotoCard("12", "Sunset Beach", "🌅 Scenic", R.drawable.img12)
    )

    private var filteredPhotoCards = mutableListOf<PhotoCard>()

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val SEARCH_DELAY_MS = 300L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            initialSearchText = it.getString(ARG_SEARCH_TEXT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupSearchFunctionality()
        setupBackPressedCallback()

        initialSearchText?.let { text ->
            if (text.isNotEmpty()) {
                searchEditText.setText(text)
                searchEditText.setSelection(text.length)
            }
        }

        searchEditText.requestFocus()
        showKeyboard()
    }

    private fun initViews(view: View) {
        searchEditText = view.findViewById(R.id.searchEditText)
        backButton = view.findViewById(R.id.backButton)
        tvSearch = view.findViewById(R.id.tvSearch)
        scrollSearch = view.findViewById(R.id.scrollSearch)
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView)

        backButton.setOnClickListener {
            (activity as? MainActivity)?.returnFromSearch()
        }

        tvSearch.setOnClickListener {
            performSearch()
        }

        setupSearchResultsRecyclerView()
    }

    private fun setupSearchFunctionality() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val searchText = s?.toString()?.trim() ?: ""
                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                if (searchText.isEmpty()) {
                    showScrollSearch()
                } else {
                    searchRunnable = Runnable {
                        performRealTimeSearch(searchText)
                    }
                    searchHandler.postDelayed(searchRunnable!!, SEARCH_DELAY_MS)
                }
            }
        })
    }

    private fun setupBackPressedCallback() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (activity as? MainActivity)?.returnFromSearch()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun setupSearchResultsRecyclerView() {
        searchResultsAdapter = PhotoCardAdapter(filteredPhotoCards) { photoCard ->
            onPhotoCardSelected(photoCard)
        }
        searchResultsRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = searchResultsAdapter
            setHasFixedSize(true)   // tối ưu hiệu năng
            itemAnimator = null     // giảm giật lag khi update
        }
    }

    private fun performSearch() {
        val searchText = searchEditText.text.toString().trim()
        performRealTimeSearch(searchText)
    }

    private fun performRealTimeSearch(searchText: String) {
        if (searchText.isNotEmpty()) {
            val newList = allPhotoCards.filter { photoCard ->
                photoCard.title.contains(searchText, ignoreCase = true)
            }
            filteredPhotoCards = newList.toMutableList()
            showSearchResults()
            searchResultsAdapter.updatePhotoCards(filteredPhotoCards)
        }
    }

    private fun showSearchResults() {
        scrollSearch.visibility = View.GONE
        searchResultsRecyclerView.visibility = View.VISIBLE
    }

    private fun showScrollSearch() {
        scrollSearch.visibility = View.VISIBLE
        searchResultsRecyclerView.visibility = View.GONE
        filteredPhotoCards.clear()
        searchResultsAdapter.updatePhotoCards(filteredPhotoCards)
    }

    private fun onPhotoCardSelected(photoCard: PhotoCard) {
        // Navigate to TemplateVideoActivity with PhotoCard data
        val intent = Intent(requireContext(), TemplateVideoActivity::class.java).apply {
            putExtra("TEMPLATE_ID", photoCard.id)
            putExtra("TEMPLATE_TITLE", photoCard.title)
            putExtra("TEMPLATE_BADGE", photoCard.badge)
            putExtra("TEMPLATE_IMAGE_RESOURCE", photoCard.imageResource)
        }
        startActivity(intent)
    }

    private fun showKeyboard() {
        searchEditText.postDelayed({
            if (isAdded && searchEditText.isFocused) {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        }, 150)
    }

    companion object {
        private const val ARG_SEARCH_TEXT = "search_text"

        fun newInstance(searchText: String = "") = SearchFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_SEARCH_TEXT, searchText)
            }
        }
    }
}