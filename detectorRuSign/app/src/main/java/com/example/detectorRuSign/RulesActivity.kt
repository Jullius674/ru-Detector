package com.example.detectorRuSign

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import android.widget.ExpandableListView
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class RulesActivity : AppCompatActivity() {
    private lateinit var expandableListView: ExpandableListView
    private lateinit var adapter: CustomExpandableListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rules)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_rules
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
                R.id.navigation_signs -> {
                    val intent = Intent(this, SignsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
                R.id.navigation_rules -> {
                    if (this !is RulesActivity) {
                        val intent = Intent(this, RulesActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                    }
                }
                R.id.navigation_camera -> {
                    val intent = Intent(this, DetectionActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
            }
            true
        }

        // Подготовка данных для адаптера
        val rulesMap = readRulesFromFile()

        // Извлекаем номера разделов, сортируем их и переназначаем их начиная с максимального номера
        val sortedTitles = rulesMap.keys.sortedByDescending { it.split(".")[0].toInt() }
        val maxIndex = sortedTitles.size  // Получаем количество элементов для использования в перенумерации

        val renumberedTitles = sortedTitles.mapIndexed { index, title ->
            "${maxIndex - index}.${title.substringAfter(".")}"
        }

        val dataList = HashMap<String, List<String>>()
        sortedTitles.zip(renumberedTitles).forEach { (oldTitle, newTitle) ->
            dataList[newTitle] = rulesMap[oldTitle] ?: emptyList()
        }



        // Инициализация адаптера и привязка к ExpandableListView
        adapter = CustomExpandableListAdapter(this, renumberedTitles, dataList)
        expandableListView = findViewById(R.id.expandableListView)
        expandableListView.setAdapter(adapter)
    }

    private fun readRulesFromFile(): HashMap<String, List<String>> {
        val rulesMap = hashMapOf<String, MutableList<String>>()
        try {
            val inputStream = assets.open("rules.txt")
            inputStream.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    if (line.trim().isNotEmpty()) {
                        val parts = line.split(":")
                        if (parts.size > 1) {
                            val key = parts[0].trim()
                            val value = parts[1].trim()
                            if (!rulesMap.containsKey(key)) {
                                rulesMap[key] = mutableListOf()
                            }
                            rulesMap[key]?.add(value)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }



        // Convert MutableList to List for each key
        val sortedMap = hashMapOf<String, List<String>>()
        rulesMap.forEach { (key, valueList) ->
            // Сортировка значений в обратном порядке
            val sortedValues = valueList.sortedWith(compareByDescending<String> { it.split(".")[0].toIntOrNull() ?: 0 }.thenBy { it })
            sortedMap[key] = sortedValues
        }
        return sortedMap
    }
}


class CustomExpandableListAdapter(
    private val context: Context,
    private val titleList: List<String>, // Заголовки разделов
    private val dataList: HashMap<String, List<String>> // Данные (здесь: правила)
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
        val view = convertView ?: inflater.inflate(R.layout.custom_child_item, parent, false)
        val textView = view.findViewById<TextView>(R.id.textViewItem)
        val childText = getChild(groupPosition, childPosition) as String
        textView.text = childText
        view.setBackgroundResource(R.drawable.selector_expandable_listview_background)
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



