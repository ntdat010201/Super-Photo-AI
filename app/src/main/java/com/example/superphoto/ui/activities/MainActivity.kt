package com.example.superphoto.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.superphoto.R
import com.example.superphoto.base.BaseActivity
import com.example.superphoto.databinding.ActivityMainBinding
import com.example.superphoto.ui.fragment.AssetsFragment
import com.example.superphoto.ui.fragment.CreateFragment
import com.example.superphoto.ui.fragment.HomeFragment
import com.example.superphoto.ui.fragment.SearchFragment
import com.example.superphoto.ui.fragment.TemplatesFragment
import com.example.superphoto.ui.fragment.ToolsFragment

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private var currentFragmentIndex = 0
    private var previousFragmentIndex = 0

    private val bottomNavItems by lazy {
        listOf(
            binding.homeContainer,
            binding.templatesContainer,
            binding.createContainer,
            binding.toolsContainer,
            binding.assetsContainer
        )
    }

    // Permission launcher for multiple permissions
    private val multiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "Tất cả quyền đã được cấp", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "Cần cấp đầy đủ quyền để truy cập ảnh, video và âm thanh",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Request permissions
        requestMediaPermissions()

        setupBottomNavigation()
        setupClickListeners()

        if (savedInstanceState == null) {
            loadFragment(HomeFragment.newInstance(), 0)
            updateBottomNavSelection(0)
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (currentFragment !is SearchFragment) {
                binding.headerLayout.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {
        binding.settingsIcon.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }

        binding.searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) openSearchFragment()
        }

        binding.searchEditText.setOnClickListener {
            openSearchFragment()
        }
    }

    private fun openSearchFragment() {
        val searchText = binding.searchEditText.text.toString()
        val searchFragment = SearchFragment.newInstance(searchText)
        previousFragmentIndex = currentFragmentIndex
        loadFragment(searchFragment, 5)
        binding.headerLayout.visibility = android.view.View.GONE
        binding.searchEditText.clearFocus()
        updateBottomNavSelection(-1)
    }

    private fun setupBottomNavigation() {
        binding.homeContainer.setOnClickListener { selectBottomNavItem(0) }
        binding.templatesContainer.setOnClickListener { selectBottomNavItem(1) }
        binding.createContainer.setOnClickListener { selectBottomNavItem(2) }
        binding.toolsContainer.setOnClickListener { selectBottomNavItem(3) }
        binding.assetsContainer.setOnClickListener { selectBottomNavItem(4) }
    }

    private fun selectBottomNavItem(index: Int) {
        if (currentFragmentIndex == index) return

        val fragment = when (index) {
            0 -> HomeFragment.newInstance()
            1 -> TemplatesFragment.newInstance()
            2 -> CreateFragment.newInstance()
            3 -> ToolsFragment.newInstance()
            4 -> AssetsFragment.newInstance()
            5 -> SearchFragment.newInstance("")
            else -> HomeFragment.newInstance()
        }

        loadFragment(fragment, index)
        updateBottomNavSelection(index)

        if (index == 5) {
            binding.headerLayout.visibility = android.view.View.GONE
        } else {
            binding.headerLayout.visibility = android.view.View.VISIBLE
        }
    }

    private fun loadFragment(fragment: Fragment, index: Int) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        currentFragmentIndex = index
    }

    private fun updateBottomNavSelection(selectedIndex: Int) {
        val iconIds = listOf(
            binding.homeContainer.findViewById<ImageView>(R.id.home_icon),
            binding.templatesContainer.findViewById<ImageView>(R.id.templates_icon),
            binding.createContainer.findViewById<ImageView>(R.id.create_icon),
            binding.toolsContainer.findViewById<ImageView>(R.id.tools_icon),
            binding.assetsContainer.findViewById<ImageView>(R.id.assets_icon)
        )

        val labelIds = listOf(
            binding.homeContainer.findViewById<TextView>(R.id.home_text),
            binding.templatesContainer.findViewById<TextView>(R.id.templates_text),
            binding.createContainer.findViewById<TextView>(R.id.create_text),
            binding.toolsContainer.findViewById<TextView>(R.id.tools_text),
            binding.assetsContainer.findViewById<TextView>(R.id.assets_text)
        )

        iconIds.forEachIndexed { index, imageView ->
            if (index != 2) {
                imageView.imageTintList = ColorStateList.valueOf(
                    if (index == selectedIndex) getColor(R.color.bottom_nav_selected) else getColor(R.color.bottom_nav_unselected)
                )
            }
        }

        labelIds.forEachIndexed { index, textView ->
            if (index != 2) {
                textView.setTextColor(
                    if (index == selectedIndex) getColor(R.color.bottom_nav_selected) else getColor(R.color.bottom_nav_unselected)
                )
            }
        }
    }

    fun returnFromSearch() {
        selectBottomNavItem(previousFragmentIndex)
    }

    fun requestMediaPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            permissionsToRequest.addAll(
                listOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }
        } else { // Android 6 - 12
            permissionsToRequest.addAll(
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }

        // Add CAMERA permission if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        // Check if any permission is not granted
        val permissionsNeeded = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            // Show rationale if needed
            permissionsNeeded.forEach { permission ->
                if (shouldShowRequestPermissionRationale(permission)) {
                    Toast.makeText(
                        this,
                        "Ứng dụng cần quyền để truy cập ảnh, video, âm thanh hoặc camera",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            multiplePermissionsLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }

    fun hasMediaPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: Check granular media permissions
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 6-12: Check storage permissions
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
}