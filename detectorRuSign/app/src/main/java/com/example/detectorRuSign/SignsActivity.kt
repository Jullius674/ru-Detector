package com.example.detectorRuSign

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import android.widget.ExpandableListView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.IOException


class SignsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signs) // Указываете макет для этой активности

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_signs
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
                R.id.navigation_signs -> {
                    if (this !is SignsActivity) {
                        val intent = Intent(this, SignsActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                    }
                }
                R.id.navigation_rules -> {
                    val intent = Intent(this, RulesActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
                R.id.navigation_camera -> {
                    val intent = Intent(this, DetectionActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
            }
            true
        }

        val groupsWithChildren = readSignsFromFile()
        val sortedTitles = groupsWithChildren.keys.sortedByDescending { it.extractNumber() }
        sortedTitles.forEach { title ->
            Log.d("SignsActivity", "Group: $title, Number: ${title.extractNumber()}")
        }

        val adapter = CustomExpandableListAdapter2(this, sortedTitles, groupsWithChildren)
        val expandableListView = findViewById<ExpandableListView>(R.id.expandableListView)
        expandableListView.setAdapter(adapter)
    }

    // Вспомогательная функция для извлечения числа из строки
    private fun String.extractNumber(): Int {
        return this.substringBefore(".").trim().toIntOrNull() ?: 0
    }

    private fun readSignsFromFile(): HashMap<String, List<TrafficSign>> {
        val groupsWithChildren = HashMap<String, MutableList<TrafficSign>>()
        try {
            val inputStream = assets.open("signs.txt")
            inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split(":").map { it.trim() }
                    if (parts.size == 3) {
                        val groupName = parts[0]
                        val iconName = parts[1]
                        val childText = parts[2]
                        val resourceId = resources.getIdentifier(iconName, "drawable", packageName)
                        val trafficSign = TrafficSign(childText, resourceId)
                        groupsWithChildren.getOrPut(groupName) { mutableListOf() }.add(trafficSign)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return groupsWithChildren.mapValues { entry -> entry.value.toList() }.toMap(HashMap())
    }
}

class CustomExpandableListAdapter2(
    private val context: Context,
    private val titleList: List<String>, // Заголовки разделов
    private val dataList: HashMap<String, List<TrafficSign>> // Данные (здесь: правила)
) : BaseExpandableListAdapter() {

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return this.dataList[this.titleList[groupPosition]]!![childPosition]
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun getChildView(
        groupPosition: Int, childPosition: Int, isLastChild: Boolean,
        convertView: View?, parent: ViewGroup
    ): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = convertView ?: inflater.inflate(R.layout.list_item_with_icon, parent, false)

        val sign = getChild(groupPosition, childPosition) as TrafficSign
        val textView = view.findViewById<TextView>(R.id.textViewTitle)
        val imageView = view.findViewById<ImageView>(R.id.imageViewIcon)
            textView.text = sign.name
            imageView.setImageResource(sign.imageResId)
        return view
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return this.dataList[this.titleList[groupPosition]]?.size ?: 0
    }

    override fun getGroup(groupPosition: Int): Any {
        return this.titleList[groupPosition]
    }

    override fun getGroupCount(): Int {
        return this.titleList.size
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getGroupView(
        groupPosition: Int, isExpanded: Boolean, convertView: View?,
        parent: ViewGroup
    ): View {
        val convertView = convertView ?: LayoutInflater.from(context).inflate(R.layout.group_header, parent, false)
        val textView = convertView.findViewById<TextView>(R.id.tvGroup)
        val groupTitle = getGroup(groupPosition) as String
        textView.text = groupTitle
        if (isExpanded) {
            textView.setBackgroundColor(ContextCompat.getColor(context, R.color.group_expanded_background))
        } else {
            textView.setBackgroundColor(ContextCompat.getColor(context, R.color.group_collapsed_background))
        }
        return convertView
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }
}
