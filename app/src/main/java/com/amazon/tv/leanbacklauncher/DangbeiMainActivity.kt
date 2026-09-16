package com.amazon.tv.leanbacklauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 星河桌面 - 当贝风格主界面
 */
class DangbeiMainActivity : Activity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var wallpaperView: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000)
        }
    }

    private var allApps: List<AppInfo> = emptyList()

    data class AppInfo(
        val name: String,
        val packageName: String,
        val icon: Drawable
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wallpaperView = findViewById(R.id.db_wallpaper)
        clockText = findViewById(R.id.db_clock)
        dateText = findViewById(R.id.db_date)
        viewPager = findViewById(R.id.db_view_pager)

        // 设置默认壁纸
        wallpaperView.setImageResource(R.drawable.edit_mode_background)

        // 加载应用
        loadApps()

        // 设置 ViewPager
        setupViewPager()

        // 设置快捷栏
        setupShortcuts()

        // 设置设置按钮
        findViewById<View>(R.id.db_settings).setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(clockRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(clockRunnable)
    }

    private fun updateClock() {
        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("M月d日 EEEE", Locale.CHINA)
        clockText.text = timeFormat.format(now)
        dateText.text = dateFormat.format(now)
    }

    private fun loadApps() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)

        allApps = resolveInfos
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString() }
            .map {
                AppInfo(
                    name = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = it.loadIcon(pm)
                )
            }
    }

    private fun setupViewPager() {
        // 每页显示的应用数量
        val appsPerPage = 20 // 4行 x 5列
        val pages = mutableListOf<List<AppInfo>>()

        if (allApps.isEmpty()) {
            pages.add(emptyList())
        } else {
            for (i in allApps.indices step appsPerPage) {
                val end = minOf(i + appsPerPage, allApps.size)
                pages.add(allApps.subList(i, end))
            }
        }

        viewPager.adapter = AppsPagerAdapter(pages)
        viewPager.offscreenPageLimit = pages.size
    }

    private fun setupShortcuts() {
        // 清理
        findViewById<View>(R.id.db_shortcut_clean).setOnClickListener {
            // 简单的内存清理提示
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            startActivity(intent)
        }

        // 文件
        findViewById<View>(R.id.db_shortcut_file).setOnClickListener {
            // 尝试打开文件管理器
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // 没有文件管理器
            }
        }

        // 壁纸
        findViewById<View>(R.id.db_shortcut_wallpaper).setOnClickListener {
            val intent = Intent(Intent.ACTION_SET_WALLPAPER)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // 没有壁纸选择器
            }
        }

        // 设置
        findViewById<View>(R.id.db_shortcut_settings).setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
        }
    }

    /**
     * ViewPager 适配器
     */
    inner class AppsPagerAdapter(private val pages: List<List<AppInfo>>) :
        RecyclerView.Adapter<AppsPagerAdapter.PageViewHolder>() {

        inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val recyclerView: RecyclerView = itemView.findViewById(R.id.db_app_grid)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.db_apps_page, parent, false)
            return PageViewHolder(view)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            val apps = pages[position]
            holder.recyclerView.layoutManager = GridLayoutManager(this@DangbeiMainActivity, 5)
            holder.recyclerView.adapter = AppsAdapter(apps)
            holder.recyclerView.setHasFixedSize(true)
        }

        override fun getItemCount(): Int = pages.size
    }

    /**
     * 应用图标适配器
     */
    inner class AppsAdapter(private val apps: List<AppInfo>) :
        RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {

        inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val icon: ImageView = itemView.findViewById(R.id.db_app_icon)
            val name: TextView = itemView.findViewById(R.id.db_app_name)
            val focusBorder: View = itemView.findViewById(R.id.db_app_focus_border)
            val root: View = itemView.findViewById(R.id.db_app_item_root)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.db_app_item, parent, false)
            return AppViewHolder(view)
        }

        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            val app = apps[position]
            holder.icon.setImageDrawable(app.icon)
            holder.name.text = app.name

            holder.root.setOnFocusChangeListener { _, hasFocus ->
                holder.focusBorder.visibility = if (hasFocus) View.VISIBLE else View.INVISIBLE
                holder.root.scaleX = if (hasFocus) 1.1f else 1.0f
                holder.root.scaleY = if (hasFocus) 1.1f else 1.0f
            }

            holder.root.setOnClickListener {
                val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                }
            }
        }

        override fun getItemCount(): Int = apps.size
    }
}
