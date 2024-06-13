package com.example.homework19.client

import android.content.Context
import android.content.pm.PackageManager
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.example.homework19.db.AppDatabase
import com.example.homework19.model.Landmark
import java.io.IOException

object RetrofitClient {

    private const val BASE_URL = "https://api.opentripmap.com/0.1/en"

    suspend fun fetchLandmarks(
        context: Context,
        latitude: Double,
        longitude: Double
    ): List<Landmark>? {
        val client = OkHttpClient()
        val apiKey = getApiKeyFromManifest(context)
        val request = Request.Builder()
            .url("$BASE_URL/places/radius?radius=1000&lon=$longitude&lat=$latitude&kinds=interesting_places&apikey=$apiKey")
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val responseBody = response.body()
                val responseData = responseBody?.string() ?: ""
                val landmarks = parseLandmarks(responseData)
                landmarks?.let {
                    saveLandmarksToLocalDatabase(context, it)
                }
                landmarks
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun parseLandmarks(jsonData: String?): List<Landmark>? {
        val landmarks = mutableListOf<Landmark>()

        try {
            val jsonArray = JSONObject(jsonData).getJSONArray("features")
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val properties = jsonObject.getJSONObject("properties")
                val name = properties.getString("name")
                val latitude =
                    jsonObject.getJSONObject("geometry").getJSONArray("coordinates").getDouble(1)
                val longitude =
                    jsonObject.getJSONObject("geometry").getJSONArray("coordinates").getDouble(0)
                val kinds =
                    properties.getString("kinds")
                val landmark = Landmark(
                    name = name,
                    photoPath = "",
                    photoDate = "",
                    latitude = latitude,
                    longitude = longitude,
                    description = kinds
                )
                landmarks.add(landmark)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        return landmarks
    }

    private suspend fun saveLandmarksToLocalDatabase(context: Context, landmarks: List<Landmark>) {
        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "landmark_database"
        ).build()

        db.landmarkDao().insertAll(landmarks)

        db.close()
    }

    private fun getApiKeyFromManifest(context: Context): String {
        val appInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        return appInfo.metaData.getString("API_KEY_OPEN_TRIP_MAP") ?: throw IllegalStateException("API key not found in manifest")
    }
}