package com.example.superphoto.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.superphoto.R
import com.example.superphoto.adapter.EffectsPhotoCardAdapter
import com.example.superphoto.adapter.FeaturedCardAdapter
import com.example.superphoto.adapter.HorizontalPhotoCardAdapter
import com.example.superphoto.model.FeaturedCard
import com.example.superphoto.model.PhotoCard
import com.example.superphoto.ui.activities.TemplateVideoActivity

class HomeFragment : Fragment() {

    private lateinit var featuredViewPager: ViewPager2
    private lateinit var recentlyViewedRecyclerView: RecyclerView
    private lateinit var hotEffectsRecyclerView: RecyclerView
    private lateinit var trendingRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupFeaturedCards()
        setupRecyclerViews()
    }

    private fun initViews(view: View) {
        featuredViewPager = view.findViewById(R.id.featuredViewPager)
        recentlyViewedRecyclerView = view.findViewById(R.id.recentlyViewedRecyclerView)
        hotEffectsRecyclerView = view.findViewById(R.id.hotEffectsRecyclerView)
        trendingRecyclerView = view.findViewById(R.id.trendingRecyclerView)

        // Setup See All click listeners
        view.findViewById<View>(R.id.seeAllRecentlyViewed)?.setOnClickListener {
            // TODO: Navigate to Recently Viewed full screen
        }

        view.findViewById<View>(R.id.seeAllHotEffects)?.setOnClickListener {
            // TODO: Navigate to Hot Effects full screen
        }

        view.findViewById<View>(R.id.seeAllTrending)?.setOnClickListener {
            // TODO: Navigate to Trending full screen
        }
    }

    private fun setupFeaturedCards() {
        val featuredCards = listOf(
            FeaturedCard(
                id = "1",
                title = "AI Portrait",
                badge = "🔥 Trending",
                imageResource = R.drawable.img1
            ),
            FeaturedCard(
                id = "2",
                title = "Vintage Filter",
                badge = "✨ New",
                imageResource = R.drawable.img12
            ),
            FeaturedCard(
                id = "3",
                title = "Neon Glow",
                badge = "🔥 Hot",
                imageResource = R.drawable.img13
            )
        )

        val featuredAdapter = FeaturedCardAdapter { card ->
            navigateToTemplateVideoActivity(card.id, card.title, card.badge, card.imageResource)
        }
        featuredViewPager.adapter = featuredAdapter
        featuredAdapter.submitList(featuredCards)
    }

    private fun setupRecyclerViews() {
        // Recently Viewed
        val recentlyViewedCards = listOf(
            PhotoCard("1", "Photo 1", "🔥 Hot", imageResource = R.drawable.img1),
            PhotoCard("2", "Photo 2", "", imageResource = R.drawable.img2),
            PhotoCard("3", "Photo 3", "✨ New", imageResource = R.drawable.img3),
            PhotoCard("4", "Photo 4", "", imageResource = R.drawable.img4),
            PhotoCard("5", "Photo 5", "🔥 Popular", imageResource = R.drawable.img5)
        )

        recentlyViewedRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val recentlyViewedAdapter = HorizontalPhotoCardAdapter { card ->
            navigateToTemplateVideoActivity(card.id, card.title, card.badge, card.imageResource)
        }
        recentlyViewedRecyclerView.adapter = recentlyViewedAdapter
        recentlyViewedAdapter.submitList(recentlyViewedCards)

        // Hot Effects
        val hotEffectsCards = listOf(
            PhotoCard("6", "Photo 6", "🔥 Hot", imageResource = R.drawable.img6),
            PhotoCard("7", "Photo 7", "", imageResource = R.drawable.img7),
            PhotoCard("8", "Photo 8", "✨ Classic", imageResource = R.drawable.img8),
            PhotoCard("9", "Photo 9", "🔥 Trending", imageResource = R.drawable.img9),
            PhotoCard("10", "Photo 10", "", imageResource = R.drawable.img10)
        )

        hotEffectsRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val hotEffectsAdapter = EffectsPhotoCardAdapter { card ->
            navigateToTemplateVideoActivity(card.id, card.title, card.badge, card.imageResource)
        }
        hotEffectsRecyclerView.adapter = hotEffectsAdapter
        hotEffectsAdapter.submitList(hotEffectsCards)

        // Trending
        val trendingCards = listOf(
            PhotoCard("11", "Dance at sunset", "🔥 Hot", imageResource = R.drawable.img11),
            PhotoCard("12", "Heart Hands", "🔥 Popular", imageResource = R.drawable.img12),
            PhotoCard("13", "City Lights", "✨ New", imageResource = R.drawable.img13),
            PhotoCard("14", "Nature", "", imageResource = R.drawable.img1),
            PhotoCard("15", "Portrait", "🔥 Trending", imageResource = R.drawable.img2)
        )

        trendingRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val trendingAdapter = HorizontalPhotoCardAdapter { card ->
            navigateToTemplateVideoActivity(card.id, card.title, card.badge, card.imageResource)
        }
        trendingRecyclerView.adapter = trendingAdapter
        trendingAdapter.submitList(trendingCards)
    }

    private fun navigateToTemplateVideoActivity(id: String, title: String, badge: String, imageResource: Int?) {
        try {
            val intent = Intent(requireContext(), TemplateVideoActivity::class.java).apply {
                putExtra("ITEM_ID", id)
                putExtra("ITEM_TITLE", title)
                putExtra("ITEM_BADGE", badge)
                imageResource?.let { putExtra("ITEM_IMAGE_RESOURCE", it) }
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}