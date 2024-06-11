package com.example.detectorRuSign

import android.graphics.Rect

class ModelOutput(var classIndex: Int, var score: Float, var box: Rect) : Comparable<ModelOutput> {
    override fun compareTo(other: ModelOutput): Int {
        return score.compareTo(other.score)
    }
}

class ModelResultPreprocessor {
    // The two methods nonMaxSuppression and IOU below are ported from https://github.com/hollance/YOLO-CoreML-MPSNNGraph/blob/master/Common/Helpers.swift
    fun nonMaxSuppression(outputs: ArrayList<ModelOutput>, limit: Int, threshold: Float): ArrayList<ModelOutput> {
        outputs.sort()
        val selected = ArrayList<ModelOutput>()
        val active = BooleanArray(outputs.size) { true }
        var numActive = active.size

        var i = 0
        while (i < outputs.size && selected.size < limit) {
            if (active[i]) {
                val output1 = outputs[i]
                selected.add(output1)
                for (j in i + 1 until outputs.size) {
                    if (active[j]) {
                        val output2 = outputs[j]
                        if (IOU(output1.box, output2.box) > threshold) {
                            active[j] = false
                            numActive--
                            if (numActive <= 0) break
                        }
                    }
                }
            }
            i++
        }
        return selected
    }

    fun IOU(a: Rect, b: Rect): Float {
        val areaA = ((a.right - a.left) * (a.bottom - a.top)).toFloat()
        if (areaA <= 0.0) return 0.0f
        val areaB = ((b.right - b.left) * (b.bottom - b.top)).toFloat()
        if (areaB <= 0.0) return 0.0f
        val intersectionMinX = Math.max(a.left, b.left).toFloat()
        val intersectionMinY = Math.max(a.top, b.top).toFloat()
        val intersectionMaxX = Math.min(a.right, b.right).toFloat()
        val intersectionMaxY = Math.min(a.bottom, b.bottom).toFloat()
        val intersectionArea = Math.max(intersectionMaxY - intersectionMinY, 0f) *
                Math.max(intersectionMaxX - intersectionMinX, 0f)
        return intersectionArea / (areaA + areaB - intersectionArea)
    }

    fun outputsToPredictions(
        outputs: FloatArray,
        imgScaleX: Float,
        imgScaleY: Float
    ): ArrayList<ModelOutput> {
        val results = ArrayList<ModelOutput>()
        for (i in 0 until numRows) {
            if (outputs[i * numColumns + 4] > threshold) {
                val x = outputs[i * numColumns]
                val y = outputs[i * numColumns + 1]
                val w = outputs[i * numColumns + 2]
                val h = outputs[i * numColumns + 3]
                var max = outputs[i * numColumns + 5]
                var cls = 0
                for (j in 0 until numColumns - 5) {
                    if (outputs[i * numColumns + 5 + j] > max) {
                        max = outputs[i * numColumns + 5 + j]
                        cls = j
                    }
                }

                val left = (imgScaleX * (x - w / 2)).toInt()
                val top = (imgScaleY * (y - h / 2)).toInt()
                val right = (imgScaleX * (x + w / 2)).toInt()
                val bottom = (imgScaleY * (y + h / 2)).toInt()
                val rect = Rect(left, top, right, bottom)
                val modelOutput = ModelOutput(cls, outputs[i * numColumns + 4], rect)
                results.add(modelOutput)
            }
        }
        return nonMaxSuppression(results, numPredsLimit, threshold)
    }

    companion object {
        //val classNames = arrayOf("2.1", "1.23", "1.17", "3.24", "8.2.1", "5.20", "5.19.1", "5.16", "3.25", "6.16", "7.15", "2.2", "2.4", "8.13.1", "4.2.1", "1.20.3", "1.25", "3.4", "8.3.2", "3.4.1", "4.1.6", "4.2.3", "4.1.1", "1.33", "5.15.5", "3.27", "1.15", "4.1.2.1", "6.3.1", "8.1.1", "6.7", "5.15.3", "7.3", "1.19", "6.4", "8.1.4", "8.8", "1.16", "1.11.1", "6.6", "5.15.1", "7.2", "5.15.2", "7.12", "3.18", "5.6", "5.5", "7.4", "4.1.2", "8.2.2", "7.11", "1.22", "1.27", "2.3.2", "5.15.2.2", "1.8", "3.13", "2.3", "8.3.3", "2.3.3", "7.7", "1.11", "8.13", "1.12.2", "1.20", "1.12", "3.32", "2.5", "3.1", "4.8.2", "3.20", "3.2", "2.3.6", "5.22", "5.18", "2.3.5", "7.5", "8.4.1", "3.14", "1.2", "1.20.2", "4.1.4", "7.6", "8.1.3", "8.3.1", "4.3", "4.1.5", "8.2.3", "8.2.4", "1.31", "3.10", "4.2.2", "7.1", "3.28", "4.1.3", "5.4", "5.3", "6.8.2", "3.31", "6.2", "1.21", "3.21", "1.13", "1.14", "2.3.4", "4.8.3", "6.15.2", "2.6", "3.18.2", "4.1.2.2", "1.7", "3.19", "1.18", "2.7", "8.5.4", "5.15.7", "5.14", "5.21", "1.1", "6.15.1", "8.6.4", "8.15", "4.5", "3.11", "8.18", "8.4.4", "3.30", "5.7.1", "5.7.2", "1.5", "3.29", "6.15.3", "5.12", "3.16", "1.30", "5.11", "1.6", "8.6.2", "6.8.3", "3.12", "3.33", "8.4.3", "5.8", "8.14", "8.17", "3.6", "1.26", "8.5.2", "6.8.1", "5.17", "1.10", "8.16", "7.18", "7.14", "8.23")
        //val classNames = arrayOf("Pedestrian crossing", "Stop", "Main road", "Radio", "End of the main road", "Give way", "Detour on the right", "Freight traffic is prohibited", "Left direction", "Move right or left", "Bus stop", "Start of strip", "End of strip", "Dangerous shoulder", "Parking lot", "Slippery Road", "Overpass", "Go straight", "One-way traffic on the right", "Turning point", "Car maintenance", "Direction of movement", "Traffic light", "Pedestrian crossing2", "Stopping prohibited", "Cafe", "Dangerous right turn", "Children", "Dangerous left turn", "Limit 40", "Speed bump", "Limit 30", "Hull 2", "Road narrowing", "Wild Animals", "Limit 60", "Dangerous turns", "Stop2", "Junction of secondary road", "Rough Road", "Taxi rank", "Limit 50", "Move to the right", "Limit 20", "Height limit", "Mesto otdiha", "Telephone", "Refueling", "Driving left", "Straight left", "Straight right", "Detour from right to left", "Underpass", "Tunnel", "Pedestrian traffic prohibited", "Detour on the left", "Circular motion", "Dig", "End of one-sided", "First aid station", "One-sided", "No parking", "Road for cars", "Deadlock", "Recommended 50", "No right turns allowed", "DPS", "End 50", "Double-sided", "No overtaking", "Steep climb", "Recommended 70", "Freight to the right", "Counter Advantage", "Limit 70", "The End of Everything", "Advantage over", "Gravel Burst", "Limit 5", "Peaceful", "Other", "Barrier", "Brick")
        val classNames = arrayOf("Главная дорога", "Дети", "Предупреждающий о искусственная неровность", "Ограничение максимальной скорости", "Зона действия", "Искусственная неровность", "Пешеходный переход", "Место остановки автобуса и (или) троллейбуса", "Конец зоны ограничения максимальной скорости", "Стоп-линия", "Зона приёма радиостанции, передающей информацию о дорожном движении", "Конец главной дороги", "Уступите дорогу", "8.13.1", "Объезд препятствия справа", "Сужение дороги слева", "Дорожные работы", "Движение грузовых автомобилей запрещено", "Направления действия налево", "Движение любых грузовых автомобилей запрещено", "Движение направо или налево", "Объезд препятствия справа или слева", "Движение прямо", "Прочие опасности", "Конец полосы", "Остановка запрещена", "Скользкая дорога", "Движение направо второй вариант", "Место для разворота", "Расстояние до объекта", "Надземный пешеходный переход", "Начало полосы", "Автозаправочная станция", "Опасная обочина", "Парковка (парковочное место)Парковка (платные услуги)Парковка (только для инвалидов)", "8.1.4. Расстояние до объекта", "Платные услуги", "Неровная дорога", "Опасный поворот направо", "Подземный пешеходный переход", "Направления движения по полосам", "Больница", "Направления движения по полосе", "Пост ДПС", "Поворот направо запрещён", "Конец дороги с односторонним движением", "Дорога с односторонним движением", "Техническое обслуживание автомобилей", "Движение направо", "Зона действия зона действия с 3.27 до 3.30", "Место отдыха", "Предупреждающий пешеходный переход", "Дикие животные", "3Примыкание справа второстепенной дороги", "Направления движения по полосе с поворотом налево", "Светофорное регулирование", "Ограничение высоты", "Пересечение со второстепенной дорогой", "Направления действия в обе стороны", "2Примыкание второстепенной дороги слева", "Пункт питания", "Опасный поворот налево", "Направление главной дороги", "Опасные повороты с первым поворотом налево", "Сужение дороги с обеих сторон", "Опасные повороты с первым поворотом направо", "Движение транспортных средств с опасными грузами запрещено", "Движение без остановки запрещено", "Въезд запрещён", "Направление движения направо транспортных средств с опасными грузами", "Обгон запрещён", "Движение запрещено", "6Примыканиесправа по диагонали второстепенной дороги", "Конец жилой зоны", "Место стоянки легковых такси", "5Примыкание слева по диагонали второстепенной дороги", "Мойка автомобилей", "действие знака на грузовые автомобили", "Ограничение ширины", "Железнодорожный переезд без шлагбаума", "Сужение дороги справа", "Движение прямо или направо", "Телефон", "Расстояние до объекта справа", "Направления действия направо", "Круговое движение", "Движение прямо или налево", "Зона действия указывает конец зоны действия знаков с 3.27 до 3.30", "Зона действия информирует водителей о нахождении их в зоне действия знаков с 3.27 до 3.30", "Тоннель", "Движение пешеходов запрещено", "Объезд препятствия слева", "Пункт первой медицинской помощи", "Стоянка запрещена", "Движение налево", "Конец дороги для автомобилей", "Дорога для автомобилей", "Справа тупик", "Конец зоны всех ограничений", "Рекомендуемая скорость", "Двустороннее движение", "Конец зоны запрещения обгона", "Крутой спуск", "Крутой подъем", "4Примыкание по диагонали второстепенной дороги", "Направление движения налево транспортных средств с опасными грузами", "Направление движения направо для грузовых автомобилей", "Преимущество встречного движения", "Поворот налево запрещён", "Движение налево вариант два", "Пересечение с круговым движением", "Разворот запрещён", "Выброс гравия", "Преимущество перед встречным движением", "Время действия", "Направление движения по полосам", "Полоса для маршрутных транспортных средств", "Жилая зона", "Железнодорожный переезд со шлагбаумом", "Направление движения прямо для грузовых автомобилей", "Способ постановки транспортного средства на стоянку спиной к тратуару", "Слепые пешеходы", "Пешеходная дорожка", "Ограничение массы", "Кроме инвалидов", "действие знака на троллейбусы", "Стоянка запрещена по чётным числам месяца", "Выезд налево на дорогу с односторонним движением", "Выезд направо на дорогу с односторонним движением", "Пересечение с трамвайной линией", "Стоянка запрещена по нечётным числам месяца", "Направление движения налево для грузовых автомобилей", "Конец дороги с полосой для маршрутных транспортных средств", "Ограничение минимальной дистанции", "Низколетящие самолеты", "Дорога с полосой для маршрутных транспортных средств", "Пересечение равнозначных дорог", "Способ постановки транспортного средства на стоянку одним колсом на тратуар", "Слева тупик", "Ограничение массы, приходящейся на ось транспортного средства", "Движение транспортных средств с взрывчатыми и легковоспламеняющимися грузами запрещено", "действие знака на легковые автомобили", "Реверсивное движение", "Полоса движения", "Инвалиды", "Движение тракторов запрещено", "Перегон скота", "Рабочие дни", "Прямо тупик", "Место остановки трамвая", "Выезд на набережную", "Влажное покрытие", "Туалет", "Пункт таможенного контроля", "Фотовидеофиксация")
        val NO_MEAN_RGB = floatArrayOf(0.0f, 0.0f, 0.0f)
        val NO_STD_RGB = floatArrayOf(1.0f, 1.0f, 1.0f)
        const val width = 640
        const val height = 640

        // model output is of size 25200*(num_of_class+5)
        private const val numRows = 25200 // as decided by the YOLOv5 model for input image of size 640*640
        private const val numColumns = 160 // left, top, right, bottom, score and classes probabilities
        private const val threshold = 0.40f // score above which a detection is generated
        private const val numPredsLimit = 6
    }
}