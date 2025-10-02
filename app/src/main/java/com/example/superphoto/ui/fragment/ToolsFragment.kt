package com.example.superphoto.ui.fragment

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.superphoto.R
import com.example.superphoto.adapter.ToolsAdapter
import com.example.superphoto.databinding.FragmentToolsBinding
import com.example.superphoto.model.PhotoCard
import com.example.superphoto.ui.activities.TemplateVideoActivity
import com.example.superphoto.utils.BottomSpaceItemDecoration

class ToolsFragment : Fragment() {
    private lateinit var binding: FragmentToolsBinding
    private lateinit var adapter: ToolsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentToolsBinding.inflate(inflater, container, false)
        setupRecyclerView()
        initData()
        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = ToolsAdapter { tool ->
            navigateToTemplateVideoActivity(
                tool.id,
                tool.title,
                tool.badge,
                tool.imageResource
            )
        }
        
        binding.rcvTool.apply {
            layoutManager = GridLayoutManager(context, 2).apply {
                // Tối ưu hóa span size để cải thiện hiệu suất
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int = 1
                }
            }
            adapter = this@ToolsFragment.adapter
            setHasFixedSize(true)  // Cải thiện hiệu suất khi size không thay đổi
            itemAnimator = null    // Tắt animation để cuộn mượt hơn
            
            // Thêm các tối ưu hóa bổ sung
            setItemViewCacheSize(20)  // Tăng cache size
            // Loại bỏ deprecated drawing cache methods
            
            // Thêm ItemDecoration để tạo spacing bottom cho item cuối
            addItemDecoration(BottomSpaceItemDecoration(300))
        }
    }

    private fun initData() {
        val dataList = listOf(
            PhotoCard(id = "1", title = "AI Enhance", imageResource = R.drawable.img1, badge = ""),
            PhotoCard(
                id = "2",
                title = "Remove Background",
                imageResource = R.drawable.img2,
                badge = ""
            ),
            PhotoCard(id = "3", title = "Cartoonize", imageResource = R.drawable.img3, badge = ""),
            PhotoCard(
                id = "4",
                title = "Portrait Studio",
                imageResource = R.drawable.img4,
                badge = ""
            ),
            PhotoCard(
                id = "5",
                title = "AI Transform",
                imageResource = R.drawable.img5,
                badge = ""
            ),
            PhotoCard(
                id = "6",
                title = "Reference to Images",
                imageResource = R.drawable.img6,
                badge = ""
            ),
            PhotoCard(id = "7", title = "Lip Sync", imageResource = R.drawable.img7, badge = ""),
            PhotoCard(id = "8", title = "Blur Effect", imageResource = R.drawable.img8, badge = ""),
            PhotoCard(
                id = "9",
                title = "Reference to Video",
                imageResource = R.drawable.img9,
                badge = ""
            ),
        )

        adapter.submitList(dataList)
    }

    companion object {
        fun newInstance() = ToolsFragment()
    }

    private fun navigateToTemplateVideoActivity(
        id: String,
        title: String,
        badge: String,
        imageResource: Int?
    ) {
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
}