package com.eherbdoc

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.text.Editable
import android.text.TextWatcher
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private enum class Screen { HOME, SCAN, RESULTS, DETAILS, HISTORY, ABOUT }

    private val executor = Executors.newSingleThreadExecutor()
    private val forest = Color.rgb(39, 91, 52)
    private val leaf = Color.rgb(91, 139, 67)
    private val ink = Color.rgb(35, 49, 37)
    private val muted = Color.rgb(105, 119, 105)
    private val paper = Color.rgb(247, 248, 239)
    private val paleGreen = Color.rgb(231, 239, 219)
    private lateinit var root: LinearLayout
    private lateinit var apiUrl: EditText
    private lateinit var status: TextView
    private var screen = Screen.HOME
    private var selectedUri: Uri? = null
    private var selectedBytes: ByteArray? = null
    private var selectedName = "plant.jpg"
    private var identification: JSONObject? = null
    private var history = JSONArray()

    companion object {
        private const val REQUEST_GALLERY = 1001
        private const val REQUEST_CAMERA = 1002
        private const val MAX_IMAGE_SIDE = 1600
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = paper
        window.navigationBarColor = paper
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(paper) }
        setContentView(root)
        render()
        checkBackend()
    }

    private fun render() {
        root.removeAllViews()
        val header = row().apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(12), dp(20), dp(10)) }
        header.addView(text("🌿  E-HerbDoc", 20f, forest, true), LinearLayout.LayoutParams(0, dp(44), 1f))
        header.addView(text(if (screen == Screen.HOME) "Plant guide" else "‹  Back", 14f, forest, true).apply {
            gravity = Gravity.CENTER; setPadding(dp(12), 0, dp(8), 0)
            if (screen != Screen.HOME) setOnClickListener { goBack() }
        })
        root.addView(header)

        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        val page = column().apply { setPadding(dp(18), dp(8), dp(18), dp(20)) }
        scroll.addView(page)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        when (screen) {
            Screen.HOME -> home(page)
            Screen.SCAN -> scan(page)
            Screen.RESULTS -> results(page)
            Screen.DETAILS -> details(page)
            Screen.HISTORY -> historyPage(page)
            Screen.ABOUT -> about(page)
        }
        root.addView(bottomNavigation())
    }

    private fun home(page: LinearLayout) {
        page.addView(text("Discover the power\nof herbal medicine", 27f, ink, true))
        page.addView(text("Identify a plant from a photo and explore what the results can tell you.", 15f, muted).apply { setPadding(0, dp(8), 0, dp(16)) })

        val hero = card(paleGreen).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(17), dp(14), dp(17)) }
        val copy = column()
        copy.addView(text("Your plant,\nidentified.", 21f, forest, true))
        copy.addView(text("Start with a clear photo of its leaves or flowers.", 13f, muted).apply { setPadding(0, dp(6), 0, 0) })
        hero.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
        hero.addView(text("🌱", 58f, forest).apply { gravity = Gravity.CENTER; setPadding(dp(8), 0, 0, 0) })
        page.addView(hero)

        page.addView(text("What would you like to do?", 17f, ink, true).apply { setPadding(0, dp(22), 0, dp(10)) })
        val actions = row()
        actions.addView(actionCard("◎", "Identify plant", "Take a photo or choose one", forest) { screen = Screen.SCAN; render() }, LinearLayout.LayoutParams(0, dp(122), 1f))
        actions.addView(space(dp(10)))
        actions.addView(actionCard("▤", "My history", "See previous scans", leaf) { openHistory() }, LinearLayout.LayoutParams(0, dp(122), 1f))
        page.addView(actions)

        page.addView(text("Recent identification", 17f, ink, true).apply { setPadding(0, dp(22), 0, dp(10)) })
        val recent = identification
        if (recent != null) {
            val recentCard = card(Color.WHITE).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12)) }
            recentCard.addView(text("🌿", 28f, forest).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(48), dp(48)))
            val info = column().apply { setPadding(dp(10), 0, 0, 0) }
            info.addView(text(recent.optString("plant_name", "Plant match"), 16f, ink, true))
            info.addView(text(recent.optString("scientific_name", ""), 13f, muted))
            recentCard.addView(info)
            recentCard.setOnClickListener { screen = Screen.RESULTS; render() }
            page.addView(recentCard)
        } else {
            page.addView(text("Your plant identifications will appear here.", 14f, muted).apply { setPadding(0, dp(4), 0, 0) })
        }

        page.addView(text("Connection settings", 16f, ink, true).apply { setPadding(0, dp(24), 0, dp(5)) })
        page.addView(text("Backend API address", 13f, muted))
        apiUrl = EditText(this).apply {
            setSingleLine(true); textSize = 14f; setTextColor(ink); setHintTextColor(muted)
            hint = "http://192.168.1.20:8000"
            setText(getPreferences(MODE_PRIVATE).getString("api_url", "http://10.0.2.2:8000"))
            background = rounded(Color.WHITE, dp(12), Color.rgb(220, 226, 214), dp(1))
            setPadding(dp(12), 0, dp(12), 0)
        }
        apiUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                getPreferences(MODE_PRIVATE).edit().putString("api_url", s?.toString()?.trim()?.trimEnd('/')).apply()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        page.addView(apiUrl, LinearLayout.LayoutParams(-1, dp(48)))
        status = text("Checking backend…", 13f, muted).apply { setPadding(0, dp(7), 0, 0) }
        page.addView(status)
    }

    private fun scan(page: LinearLayout) {
        page.addView(text("Identify a plant", 25f, ink, true))
        page.addView(text("Frame one plant clearly. A close, well-lit photo gives the best chance of a useful match.", 14f, muted).apply { setPadding(0, dp(6), 0, dp(14)) })
        val photoCard = card(Color.rgb(229, 235, 219)).apply { gravity = Gravity.CENTER; setPadding(dp(12), dp(12), dp(12), dp(12)) }
        val preview = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setBackground(rounded(Color.rgb(215, 226, 201), dp(18))); clipToOutline = true }
        if (selectedUri != null) preview.setImageURI(selectedUri) else selectedBytes?.let { preview.setImageBitmap(BitmapFactory.decodeByteArray(it, 0, it.size)) }
        if (selectedUri == null && selectedBytes == null) {
            photoCard.addView(text("🌿\n\nPosition the plant\nin the frame", 20f, forest, true).apply { gravity = Gravity.CENTER; textAlignment = View.TEXT_ALIGNMENT_CENTER })
        } else {
            photoCard.addView(preview, LinearLayout.LayoutParams(-1, dp(260)))
        }
        page.addView(photoCard, LinearLayout.LayoutParams(-1, dp(290)))
        val pickers = row().apply { setPadding(0, dp(14), 0, dp(12)) }
        pickers.addView(button("📷  Take photo", forest) { launchCamera() }, LinearLayout.LayoutParams(0, dp(50), 1f))
        pickers.addView(space(dp(10)))
        pickers.addView(button("▧  Upload photo", Color.WHITE, forest) { openImagePicker() }, LinearLayout.LayoutParams(0, dp(50), 1f))
        page.addView(pickers)
        page.addView(button("Identify this plant", forest) { identifyPlant() }, LinearLayout.LayoutParams(-1, dp(52)))
        page.addView(text("JPG, PNG or WebP · Maximum 10 MB", 12f, muted).apply { gravity = Gravity.CENTER; setPadding(0, dp(9), 0, 0) })
        status = text("Choose a photo to begin.", 13f, muted).apply { gravity = Gravity.CENTER; setPadding(0, dp(12), 0, 0) }
        page.addView(status)
    }

    private fun results(page: LinearLayout) {
        page.addView(text("Identification result", 25f, ink, true).apply { setPadding(0, 0, 0, dp(13)) })
        status = text("PlantNet candidate match", 13f, muted).apply { setPadding(0, 0, 0, dp(8)) }
        page.addView(status)
        val result = identification
        if (result == null) {
            page.addView(text("No result yet. Take or upload a plant photo to begin.", 15f, muted))
            page.addView(button("Start a scan", forest) { screen = Screen.SCAN; render() })
            return
        }
        val resultCard = card(Color.WHITE).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(14)) }
        val picture = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setBackground(rounded(paleGreen, dp(12))); clipToOutline = true }
        if (selectedUri != null) picture.setImageURI(selectedUri) else selectedBytes?.let { picture.setImageBitmap(BitmapFactory.decodeByteArray(it, 0, it.size)) }
        resultCard.addView(picture, LinearLayout.LayoutParams(dp(90), dp(90)))
        val names = column().apply { setPadding(dp(12), 0, 0, 0) }
        names.addView(text(result.optString("plant_name", "Possible match"), 20f, forest, true))
        names.addView(text(result.optString("scientific_name", ""), 14f, muted).apply { setPadding(0, dp(3), 0, dp(8)) })
        names.addView(text("PlantNet confidence: ${result.opt("confidence") ?: "N/A"}%", 13f, ink, true))
        resultCard.addView(names)
        page.addView(resultCard)

        sectionTitle(page, "About this identification")
        page.addView(card(Color.WHITE).apply { addView(text(result.optString("report", "No report returned."), 14f, ink)) })
        val candidates = result.optJSONArray("candidates") ?: JSONArray()
        if (candidates.length() > 0) {
            sectionTitle(page, "Other possible matches")
            for (i in 0 until candidates.length()) {
                val item = candidates.optJSONObject(i) ?: continue
                val namesArr = item.optJSONArray("common_names")
                val common = if (namesArr != null && namesArr.length() > 0) " · ${namesArr.optString(0)}" else ""
                page.addView(text("${i + 1}. ${item.optString("scientific_name")}$common  ·  ${item.opt("confidence") ?: "?"}%", 14f, ink).apply { setPadding(dp(5), dp(5), 0, dp(5)) })
            }
        }
        sectionTitle(page, "Safety note")
        page.addView(card(paleGreen).apply { addView(text(result.optString("safety_notice", "Photo identification is not proof of medicinal safety."), 14f, forest, true)) })
        page.addView(button("View plant information", forest) { screen = Screen.DETAILS; render() }, LinearLayout.LayoutParams(-1, dp(50)))
    }

    private fun details(page: LinearLayout) {
        val result = identification
        page.addView(text("Plant information", 25f, ink, true).apply { setPadding(0, 0, 0, dp(12)) })
        if (result == null) { page.addView(text("Identify a plant to see its information.", 15f, muted)); return }
        val image = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setBackground(rounded(paleGreen, dp(18))); clipToOutline = true }
        if (selectedUri != null) image.setImageURI(selectedUri) else selectedBytes?.let { image.setImageBitmap(BitmapFactory.decodeByteArray(it, 0, it.size)) }
        page.addView(image, LinearLayout.LayoutParams(-1, dp(210)))
        page.addView(text(result.optString("plant_name", "Possible plant match"), 24f, forest, true).apply { setPadding(0, dp(14), 0, 0) })
        page.addView(text(result.optString("scientific_name", ""), 15f, muted).apply { setPadding(0, dp(2), 0, dp(10)) })
        infoCard(page, "IDENTIFICATION", "PlantNet match confidence: ${result.opt("confidence") ?: "N/A"}%")
        infoCard(page, "WHAT THE API RETURNED", result.optString("report", "No further information was returned."))
        infoCard(page, "SAFETY", result.optString("safety_notice", "Photo identification is not proof of medicinal safety."))
        page.addView(text("The identification result is a photo-based match. Confirm the species with a qualified local expert before using any plant medicinally.", 13f, muted).apply { setPadding(dp(2), dp(8), dp(2), dp(12)) })
    }

    private fun historyPage(page: LinearLayout) {
        page.addView(text("Recent identifications", 25f, ink, true).apply { setPadding(0, 0, 0, dp(5)) })
        page.addView(text("Saved by your E-HerbDoc backend", 14f, muted).apply { setPadding(0, 0, 0, dp(12)) })
        status = text("Loading history…", 13f, muted)
        page.addView(status)
        request("GET", "/api/history") { code, body ->
            try {
                val json = JSONObject(body)
                if (code != 200) { status.text = json.optString("detail", "Could not load history."); return@request }
                history = json.optJSONArray("history") ?: JSONArray()
                page.removeView(status)
                if (history.length() == 0) {
                    page.addView(card(Color.WHITE).apply { addView(text("No identifications yet. Your completed scans will appear here.", 15f, muted)) })
                } else {
                    for (i in 0 until history.length()) {
                        val item = history.optJSONObject(i) ?: continue
                        val report = item.optString("report", "")
                        val common = report.lineAfter("Common name:") ?: item.optString("image_name", "Plant identification")
                        val scientific = report.lineAfter("Scientific name:") ?: ""
                        val entry = card(Color.WHITE).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(13), dp(12), dp(13), dp(12)) }
                        entry.addView(text("🌿", 28f, forest), LinearLayout.LayoutParams(dp(46), -2))
                        val info = column()
                        info.addView(text(common, 16f, ink, true))
                        info.addView(text(scientific, 13f, muted))
                        info.addView(text(item.optString("created_at", ""), 11f, muted).apply { setPadding(0, dp(4), 0, 0) })
                        entry.addView(info)
                        entry.setOnClickListener {
                            identification = JSONObject().apply {
                                put("plant_name", common); put("scientific_name", scientific)
                                put("report", report); put("safety_notice", "Photo identification is not proof of medicinal safety.")
                            }
                            selectedUri = null; selectedBytes = null
                            screen = Screen.DETAILS; render()
                        }
                        page.addView(entry)
                    }
                }
                status.text = "History loaded"
            } catch (e: Exception) { status.text = e.message ?: "Could not read history." }
        }
    }

    private fun about(page: LinearLayout) {
        page.addView(text("About E-HerbDoc", 25f, ink, true).apply { setPadding(0, 0, 0, dp(12)) })
        page.addView(card(Color.WHITE).apply {
            addView(text("Identify. Learn. Heal.", 20f, forest, true))
            addView(text("E-HerbDoc sends your chosen plant photo to the configured backend for image identification. Results and history come from that service.", 14f, ink).apply { setPadding(0, dp(8), 0, 0) })
        })
        infoCard(page, "USE PHOTOS CAREFULLY", "PlantNet confidence is a candidate match, not a guarantee that the plant is safe or medicinal.")
        infoCard(page, "API CONNECTION", "On an emulator use http://10.0.2.2:8000. On a phone, use your computer's LAN IP and port 8000, or a deployed HTTPS address.")
    }

    private fun bottomNavigation(): LinearLayout {
        val nav = row().apply { gravity = Gravity.CENTER; setPadding(dp(6), dp(6), dp(6), dp(8)); setBackgroundColor(Color.WHITE) }
        val tabs = listOf(
            Triple("⌂", "Home", Screen.HOME), Triple("◷", "History", Screen.HISTORY),
            Triple("◎", "Scan", Screen.SCAN), Triple("ⓘ", "Info", Screen.ABOUT)
        )
        tabs.forEach { (icon, label, target) ->
            val active = screen == target || (target == Screen.SCAN && screen in listOf(Screen.RESULTS, Screen.DETAILS))
            val tab = column().apply { gravity = Gravity.CENTER; setPadding(0, dp(4), 0, dp(2)) }
            tab.addView(text(icon, 20f, if (active) forest else muted, true).apply { gravity = Gravity.CENTER })
            tab.addView(text(label, 11f, if (active) forest else muted, active).apply { gravity = Gravity.CENTER })
            tab.setOnClickListener {
                if (target == Screen.HISTORY) openHistory() else { screen = target; render() }
            }
            nav.addView(tab, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        return nav
    }

    private fun actionCard(icon: String, title: String, subtitle: String, accent: Int, action: () -> Unit): View = card(Color.WHITE).apply {
        setPadding(dp(12), dp(12), dp(8), dp(9)); isClickable = true; isFocusable = true
        addView(text(icon, 23f, accent, true))
        addView(text(title, 14f, ink, true).apply { setPadding(0, dp(4), 0, dp(2)) })
        addView(text(subtitle, 11f, muted))
        setOnClickListener { action() }
    }

    private fun sectionTitle(parent: LinearLayout, title: String) {
        parent.addView(text(title, 17f, ink, true).apply { setPadding(0, dp(18), 0, dp(8)) })
    }

    private fun infoCard(parent: LinearLayout, title: String, value: String) {
        val box = card(Color.WHITE)
        box.addView(text(title, 11f, leaf, true))
        box.addView(text(value, 14f, ink).apply { setPadding(0, dp(5), 0, 0) })
        parent.addView(box)
    }

    private fun openHistory() {
        screen = Screen.HISTORY
        render()
    }

    private fun goBack() {
        screen = when (screen) {
            Screen.DETAILS -> Screen.RESULTS
            Screen.RESULTS, Screen.HISTORY, Screen.ABOUT -> Screen.HOME
            else -> Screen.HOME
        }
        render()
    }

    private fun checkBackend() {
        request("GET", "/health") { code, body ->
            if (::status.isInitialized) status.text = if (code == 200) "Backend connected" else "Backend returned HTTP $code · $body"
        }
    }

    private fun identifyPlant() {
        val uri = selectedUri
        val capture = selectedBytes
        if (uri == null && capture == null) { status.text = "Take or upload a plant photo first."; return }
        status.text = "Identifying plant..."
        executor.execute {
            try {
                val bytes = capture ?: prepareJpeg(uri!!)
                val boundary = "----EHerbDoc${UUID.randomUUID().toString().replace("-", "")}"
                val safeName = selectedName.replace("\"", "_").replace("\r", "_").replace("\n", "_")
                val requestBody = ByteArrayOutputStream().apply {
                    write("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"$safeName\"\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray())
                    write(bytes)
                    write("\r\n--$boundary--\r\n".toByteArray())
                }.toByteArray()
                val (code, body) = send("POST", "/api/identify", requestBody, "multipart/form-data; boundary=$boundary", 120_000)
                val json = JSONObject(body)
                runOnUiThread {
                    if (code in 200..299) {
                        identification = json
                        screen = Screen.RESULTS
                        render()
                        status.text = "Identification complete"
                    } else {
                        if (::status.isInitialized) status.text = "Identification failed · HTTP $code"
                        showMessage(json.optString("detail", "Identification failed. Please try again."))
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { if (::status.isInitialized) status.text = "Could not reach backend"; showMessage(e.message ?: "Upload failed") }
            }
        }
    }

    private fun loadGalleryImage(uri: Uri) {
        try {
            selectedUri = uri
            selectedBytes = null
            selectedName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else "plant.jpg"
            } ?: "plant.jpg"
            screen = Screen.SCAN
            render()
            status.text = "Photo selected. Tap Identify this plant when ready."
        } catch (e: Exception) { showMessage(e.message ?: "Could not open this photo.") }
    }

    private fun launchCamera() {
        val intent = Intent("android.media.action.IMAGE_CAPTURE")
        if (intent.resolveActivity(packageManager) == null) { showMessage("No camera app is available on this device."); return }
        startActivityForResult(intent, REQUEST_CAMERA)
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        startActivityForResult(Intent.createChooser(intent, "Choose a plant photo"), REQUEST_GALLERY)
    }

    @Deprecated("Uses the platform picker callback for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        if (requestCode == REQUEST_GALLERY) {
            data?.data?.let { loadGalleryImage(it) }
        } else if (requestCode == REQUEST_CAMERA) {
            val bitmap = data?.extras?.get("data") as? Bitmap
            if (bitmap == null) { showMessage("The camera did not return a photo. Please try again."); return }
            selectedBytes = bitmapToJpeg(bitmap)
            selectedUri = null
            selectedName = "camera-photo.jpg"
            screen = Screen.SCAN
            render()
            status.text = "Photo captured. Tap Identify this plant when ready."
        }
    }

    private fun prepareJpeg(uri: Uri): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("The selected photo could not be read.")
        var sample = 1
        while (bounds.outWidth / sample > MAX_IMAGE_SIDE * 2 || bounds.outHeight / sample > MAX_IMAGE_SIDE * 2) sample *= 2
        val bitmap = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("The selected photo could not be decoded.")
        val scale = MAX_IMAGE_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height)
        val resized = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true) else bitmap
        return bitmapToJpeg(resized)
    }

    private fun bitmapToJpeg(bitmap: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        return out.toByteArray()
    }

    private fun api(path: String): String {
        val value = getPreferences(MODE_PRIVATE).getString("api_url", "http://10.0.2.2:8000")
            ?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() } ?: "http://10.0.2.2:8000"
        return value + path
    }

    private fun request(method: String, path: String, done: (Int, String) -> Unit) {
        executor.execute {
            try {
                val (code, body) = send(method, path, null, null)
                runOnUiThread { done(code, body) }
            } catch (e: Exception) {
                runOnUiThread { if (::status.isInitialized) status.text = "Backend unavailable: ${e.message}" }
            }
        }
    }

    private fun send(method: String, path: String, body: ByteArray?, type: String? = null, timeout: Int = 20_000): Pair<Int, String> {
        val connection = URL(api(path)).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = timeout
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", type)
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            val stream = if (code >= 400) connection.errorStream else connection.inputStream
            val response = stream?.use { it.readBytes().toString(Charsets.UTF_8) } ?: "{}"
            return code to response
        } finally { connection.disconnect() }
    }

    private fun showMessage(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
    }

    private fun String.lineAfter(key: String): String? = lineSequence().firstOrNull { it.startsWith(key) }?.substringAfter(key)?.trim()

    private fun text(value: String, size: Float = 14f, color: Int = ink, bold: Boolean = false): TextView = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun column(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    private fun space(width: Int): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(width, 1) }

    private fun card(color: Int): LinearLayout = column().apply {
        setPadding(dp(15), dp(13), dp(15), dp(13))
        background = rounded(color, dp(17), Color.rgb(226, 231, 219), dp(1))
        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = dp(10)
        layoutParams = params
    }

    private fun button(label: String, color: Int, textColor: Int = Color.WHITE, action: () -> Unit): Button = Button(this).apply {
        text = label; textSize = 14f; setTextColor(textColor); isAllCaps = false; typeface = Typeface.DEFAULT_BOLD
        background = rounded(color, dp(14), if (color == Color.WHITE) Color.rgb(214, 222, 208) else color, dp(1))
        setOnClickListener { action() }
    }

    private fun rounded(color: Int, radius: Int, stroke: Int = color, strokeWidth: Int = 0) = GradientDrawable().apply {
        setColor(color); cornerRadius = radius.toFloat(); if (strokeWidth > 0) setStroke(strokeWidth, stroke)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }
}
