package com.example.automus

import android.content.Context
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class AppleMusicAPI(private val context: Context) {

    private val client = OkHttpClient()
    private val baseUrl = "https://api.music.apple.com/v1/"
    private val developerToken = "YOUR_DEVELOPER_TOKEN"

    fun authenticateUser(authCode: String, callback: (Boolean) -> Unit) {
        val url = "$baseUrl/me"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $developerToken")
            .addHeader("Music-User-Token", authCode)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("AppleMusicAPI", "Authentication failed", e)
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
        })
    }

    fun searchMusic(query: String, callback: (List<MusicItem>) -> Unit) {
        val url = "$baseUrl/catalog/us/search?term=$query&types=songs,albums,artists,playlists"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $developerToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("AppleMusicAPI", "Search failed", e)
                callback(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val musicItems = parseMusicItems(responseBody)
                    callback(musicItems)
                } else {
                    callback(emptyList())
                }
            }
        })
    }

    private fun parseMusicItems(responseBody: String?): List<MusicItem> {
        val musicItems = mutableListOf<MusicItem>()
        responseBody?.let {
            val jsonObject = JSONObject(it)
            val results = jsonObject.getJSONObject("results")
            val songs = results.getJSONArray("songs")
            for (i in 0 until songs.length()) {
                val song = songs.getJSONObject(i)
                val attributes = song.getJSONObject("attributes")
                val title = attributes.getString("name")
                val artist = attributes.getString("artistName")
                val album = attributes.getString("albumName")
                val artworkUrl = attributes.getJSONObject("artwork").getString("url")
                musicItems.add(MusicItem(title, artist, album, artworkUrl))
            }
        }
        return musicItems
    }
}

data class MusicItem(
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String
)
