package com.ys.coil.sample

import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL
import com.ys.coil.load
import com.ys.coil.sample.Screen.Detail
import com.ys.sample.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

	private val viewModel: MainViewModel by viewModels()

	private lateinit var binding: ActivityMainBinding
	private lateinit var listAdapter: ImageListAdapter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding = ActivityMainBinding.inflate(layoutInflater)

		setContentView(binding.root)
		setSupportActionBar(binding.toolbar)

		if (SDK_INT >= 29) {
			window.setDecorFitsSystemWindowsCompat(false)
			binding.toolbar.setOnApplyWindowInsetsListener { view, insets ->
				view.updatePadding(top = insets.toCompat().getInsets(WindowInsetsCompat.Type.systemBars()).top)
				insets
			}
		}

		listAdapter = ImageListAdapter(this) { viewModel.screen.value = it }
		binding.list.apply {
			setHasFixedSize(true)
			layoutManager = StaggeredGridLayoutManager(listAdapter.numColumns, VERTICAL)
			adapter = listAdapter
		}

		lifecycleScope.apply {
			launch { viewModel.assetType.collect(::setAssetType) }
			launch { viewModel.images.collect(::setImages) }
			launch { viewModel.screen.collect(::setScreen) }
		}
	}

	private fun setScreen(screen: Screen) {
		when (screen) {
			is Screen.List -> {
				binding.list.isVisible = true
				binding.detail.isVisible = false
			}
			is Detail -> {
				binding.list.isVisible = false
				binding.detail.isVisible = true
				binding.detail.load(screen.image.uri) {
					placeholderMemoryCacheKey(screen.placeholder)
					parameters(screen.image.parameters)
				}
			}
		}
	}

	private fun setImages(images: List<Image>) {
		listAdapter.submitList(images) {
			// 목록 항목이 업데이트될 때 우리가 목록의 맨 위에 있는지 확인하십시오.
			binding.list.scrollToPosition(0)
		}
	}

	@Suppress("UNUSED_PARAMETER")
	private fun setAssetType(assetType: AssetType) {
		invalidateOptionsMenu()
	}


	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		val title = viewModel.assetType.value.name
		val item = menu.add(Menu.NONE, R.id.action_toggle_asset_type, Menu.NONE, title)
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.action_toggle_asset_type -> {
				val values = AssetType.values()
				val currentAssetType = viewModel.assetType.value
				val newAssetType = values[(values.indexOf(currentAssetType) + 1) % values.count()]
				viewModel.assetType.value = newAssetType
			}
			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}

	override fun onBackPressed() {
		if (!viewModel.onBackPressed()) {
			super.onBackPressed()
		}
	}
}
