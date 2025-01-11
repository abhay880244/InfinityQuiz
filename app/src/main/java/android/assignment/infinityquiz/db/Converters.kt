package android.assignment.infinityquiz.db

import android.assignment.infinityquiz.model.Solution
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromOptionsList(options: List<String>): String = Gson().toJson(options)

    @TypeConverter
    fun toOptionsList(optionsString: String): List<String> =
        Gson().fromJson(optionsString, object : TypeToken<List<String>>() {}.type)

    @TypeConverter
    fun fromSolutionList(solution: List<Solution>): String = Gson().toJson(solution)

    @TypeConverter
    fun toSolutionList(solutionString: String): List<Solution> =
        Gson().fromJson(solutionString, object : TypeToken<List<Solution>>() {}.type)
}
