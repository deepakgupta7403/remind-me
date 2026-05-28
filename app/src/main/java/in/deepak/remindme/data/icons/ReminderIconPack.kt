package `in`.deepak.remindme.data.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Curated set of icons a user can attach to a reminder.
 *
 * The icon is persisted as a stable [ReminderIcon.key] string (not the
 * [ImageVector], which can't be serialised) so the on-disk format survives
 * icon-library upgrades and renames. An unknown or null key falls back to the
 * per-type default at the render site — adding/removing icons here never
 * crashes older data.
 *
 * Each entry carries [ReminderIcon.keywords] (synonyms the user might type)
 * so the picker's search box matches on intent, not just the visible label —
 * e.g. typing "gym" finds the dumbbell, "h2o" finds the water drop.
 *
 * Like [in.deepak.remindme.data.templates.TemplateCatalog], curation lives in
 * code: this list IS the product decision about what icons to offer.
 */
data class ReminderIcon(
    val key: String,
    val label: String,
    val image: ImageVector,
    val keywords: List<String> = emptyList(),
) {
    /** True if [query] (case/space-insensitive) matches the label, key, or a keyword. */
    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        return label.lowercase().contains(q) ||
            key.contains(q) ||
            keywords.any { it.contains(q) }
    }
}

object ReminderIconPack {

    // Keys are stable identifiers — never rename an existing one (persisted on
    // disk and referenced by TemplateCatalog). Adding new entries is always safe.
    val all: List<ReminderIcon> = listOf(
        // --- Health & wellness ---
        ReminderIcon("water", "Water", Icons.Filled.WaterDrop, listOf("drink", "hydrate", "h2o", "thirst")),
        ReminderIcon("medication", "Medication", Icons.Filled.Medication, listOf("pill", "medicine", "drug", "tablet", "pharmacy")),
        ReminderIcon("vaccine", "Vaccine", Icons.Filled.Vaccines, listOf("shot", "injection", "syringe", "jab")),
        ReminderIcon("health", "Health", Icons.Filled.Favorite, listOf("heart", "love", "wellness", "like")),
        ReminderIcon("heartbeat", "Heartbeat", Icons.Filled.MonitorHeart, listOf("pulse", "cardio", "ecg", "vitals")),
        ReminderIcon("medical", "Medical", Icons.Filled.MedicalServices, listOf("doctor", "hospital", "clinic", "firstaid", "checkup")),
        ReminderIcon("meditate", "Meditate", Icons.Filled.SelfImprovement, listOf("yoga", "mindfulness", "zen", "calm", "relax")),
        ReminderIcon("spa", "Spa", Icons.Filled.Spa, listOf("relax", "wellness", "massage", "self care")),
        ReminderIcon("sleep", "Sleep", Icons.Filled.Bedtime, listOf("night", "moon", "rest", "bed")),
        ReminderIcon("eye", "Eye rest", Icons.Filled.Visibility, listOf("vision", "look", "eyes", "screen", "break")),
        ReminderIcon("workout", "Workout", Icons.Filled.FitnessCenter, listOf("gym", "exercise", "dumbbell", "lift", "fitness")),
        ReminderIcon("run", "Run", Icons.AutoMirrored.Filled.DirectionsRun, listOf("running", "jog", "cardio", "exercise")),
        ReminderIcon("walk", "Walk", Icons.AutoMirrored.Filled.DirectionsWalk, listOf("walking", "steps", "stroll")),
        ReminderIcon("bike", "Bike", Icons.AutoMirrored.Filled.DirectionsBike, listOf("cycling", "bicycle", "ride")),
        ReminderIcon("swim", "Swim", Icons.Filled.Pool, listOf("swimming", "pool", "water", "exercise")),
        ReminderIcon("hiking", "Hiking", Icons.Filled.Hiking, listOf("hike", "trek", "outdoor", "mountain", "trail")),

        // --- Food & drink ---
        ReminderIcon("food", "Food", Icons.Filled.Restaurant, listOf("eat", "meal", "dining", "lunch", "dinner", "fork")),
        ReminderIcon("breakfast", "Breakfast", Icons.Filled.FreeBreakfast, listOf("morning", "tea", "coffee", "meal")),
        ReminderIcon("coffee", "Coffee", Icons.Filled.LocalCafe, listOf("cafe", "drink", "caffeine", "tea")),
        ReminderIcon("fastfood", "Fast food", Icons.Filled.Fastfood, listOf("burger", "snack", "junk")),
        ReminderIcon("pizza", "Pizza", Icons.Filled.LocalPizza, listOf("food", "italian", "slice")),
        ReminderIcon("icecream", "Ice cream", Icons.Filled.Icecream, listOf("dessert", "sweet", "cold")),
        ReminderIcon("cake", "Cake", Icons.Filled.Cake, listOf("birthday", "dessert", "sweet", "party")),
        ReminderIcon("drinks", "Drinks", Icons.Filled.LocalBar, listOf("alcohol", "cocktail", "bar", "party")),

        // --- Work & money ---
        ReminderIcon("work", "Work", Icons.Filled.Work, listOf("job", "office", "briefcase", "business")),
        ReminderIcon("business", "Business", Icons.Filled.BusinessCenter, listOf("office", "job", "suitcase", "career")),
        ReminderIcon("computer", "Computer", Icons.Filled.Computer, listOf("pc", "desktop", "work")),
        ReminderIcon("laptop", "Laptop", Icons.Filled.Laptop, listOf("computer", "work", "portable")),
        ReminderIcon("code", "Code", Icons.Filled.Code, listOf("programming", "develop", "software", "dev")),
        ReminderIcon("email", "Email", Icons.Filled.Email, listOf("mail", "message", "inbox", "letter")),
        ReminderIcon("call", "Call", Icons.Filled.Call, listOf("phone", "telephone", "ring", "dial")),
        ReminderIcon("meeting", "Meeting", Icons.Filled.Event, listOf("calendar", "appointment", "date", "schedule")),
        ReminderIcon("team", "Team", Icons.Filled.Groups, listOf("people", "group", "conference", "meeting")),
        ReminderIcon("task", "Task", Icons.Filled.TaskAlt, listOf("todo", "done", "check", "complete")),
        ReminderIcon("checklist", "Checklist", Icons.Filled.Checklist, listOf("todo", "list", "tasks", "chores")),
        ReminderIcon("money", "Money", Icons.Filled.AttachMoney, listOf("cash", "dollar", "pay", "finance")),
        ReminderIcon("payment", "Payment", Icons.Filled.Payments, listOf("pay", "bill", "money", "finance")),
        ReminderIcon("savings", "Savings", Icons.Filled.Savings, listOf("piggybank", "save", "money", "budget")),
        ReminderIcon("card", "Card", Icons.Filled.CreditCard, listOf("payment", "bank", "debit", "credit")),
        ReminderIcon("receipt", "Receipt", Icons.Filled.Receipt, listOf("bill", "invoice", "expense")),
        ReminderIcon("shopping", "Shopping", Icons.Filled.ShoppingCart, listOf("buy", "store", "cart", "groceries")),
        ReminderIcon("bag", "Shopping bag", Icons.Filled.ShoppingBag, listOf("shopping", "buy", "store")),

        // --- Home & lifestyle ---
        ReminderIcon("home", "Home", Icons.Filled.Home, listOf("house", "family")),
        ReminderIcon("cleaning", "Cleaning", Icons.Filled.CleaningServices, listOf("clean", "chores", "broom", "tidy")),
        ReminderIcon("laundry", "Laundry", Icons.Filled.LocalLaundryService, listOf("wash", "clothes", "washing")),
        ReminderIcon("plants", "Plants", Icons.Filled.LocalFlorist, listOf("flower", "garden", "water", "plant")),
        ReminderIcon("pets", "Pets", Icons.Filled.Pets, listOf("dog", "cat", "animal", "paw")),
        ReminderIcon("lightbulb", "Idea", Icons.Filled.Lightbulb, listOf("idea", "light", "bright", "lamp")),
        ReminderIcon("wifi", "Wi-Fi", Icons.Filled.Wifi, listOf("internet", "network", "connection")),
        ReminderIcon("lock", "Lock", Icons.Filled.Lock, listOf("secure", "password", "safe")),
        ReminderIcon("key", "Key", Icons.Filled.Key, listOf("lock", "unlock", "password", "access")),
        ReminderIcon("repair", "Repair", Icons.Filled.Handyman, listOf("fix", "tools", "maintenance", "diy")),

        // --- Travel & places ---
        ReminderIcon("car", "Car", Icons.Filled.DirectionsCar, listOf("drive", "vehicle", "auto", "travel")),
        ReminderIcon("flight", "Flight", Icons.Filled.Flight, listOf("plane", "travel", "airport", "trip")),
        ReminderIcon("train", "Train", Icons.Filled.Train, listOf("railway", "commute", "travel")),
        ReminderIcon("bus", "Bus", Icons.Filled.DirectionsBus, listOf("transit", "commute", "travel")),
        ReminderIcon("fuel", "Fuel", Icons.Filled.LocalGasStation, listOf("gas", "petrol", "fill", "car")),
        ReminderIcon("map", "Map", Icons.Filled.Map, listOf("location", "navigation", "directions")),
        ReminderIcon("place", "Place", Icons.Filled.Place, listOf("location", "pin", "marker", "map")),
        ReminderIcon("hotel", "Hotel", Icons.Filled.Hotel, listOf("stay", "travel", "room", "lodging")),
        ReminderIcon("beach", "Beach", Icons.Filled.BeachAccess, listOf("holiday", "vacation", "sea", "umbrella")),

        // --- Learning & hobbies ---
        ReminderIcon("study", "Study", Icons.Filled.School, listOf("education", "learn", "class", "graduate", "homework")),
        ReminderIcon("book", "Book", Icons.AutoMirrored.Filled.MenuBook, listOf("read", "study", "reading")),
        ReminderIcon("read", "Reading", Icons.Filled.AutoStories, listOf("book", "story", "novel")),
        ReminderIcon("music", "Music", Icons.Filled.MusicNote, listOf("song", "audio", "play", "sound")),
        ReminderIcon("headphones", "Headphones", Icons.Filled.Headphones, listOf("music", "audio", "listen", "podcast")),
        ReminderIcon("movie", "Movie", Icons.Filled.Movie, listOf("film", "cinema", "watch", "video")),
        ReminderIcon("game", "Game", Icons.Filled.SportsEsports, listOf("gaming", "controller", "play")),
        ReminderIcon("art", "Art", Icons.Filled.Palette, listOf("paint", "draw", "color", "creative")),
        ReminderIcon("brush", "Brush", Icons.Filled.Brush, listOf("paint", "art", "draw")),
        ReminderIcon("camera", "Camera", Icons.Filled.PhotoCamera, listOf("photo", "picture", "photography")),

        // --- General ---
        ReminderIcon("star", "Star", Icons.Filled.Star, listOf("favorite", "important", "rating")),
        ReminderIcon("bookmark", "Bookmark", Icons.Filled.Bookmark, listOf("save", "mark", "favorite")),
        ReminderIcon("flag", "Flag", Icons.Filled.Flag, listOf("mark", "important", "goal")),
        ReminderIcon("alarm", "Alarm", Icons.Filled.Alarm, listOf("clock", "wake", "time", "alert")),
        ReminderIcon("clock", "Clock", Icons.Filled.Schedule, listOf("time", "hour", "watch", "schedule")),
        ReminderIcon("notification", "Notification", Icons.Filled.NotificationsActive, listOf("bell", "alert", "remind", "ping")),
        ReminderIcon("celebration", "Celebration", Icons.Filled.Celebration, listOf("party", "confetti", "celebrate")),
        ReminderIcon("gift", "Gift", Icons.Filled.CardGiftcard, listOf("present", "birthday", "reward")),
        ReminderIcon("trophy", "Trophy", Icons.Filled.EmojiEvents, listOf("award", "win", "achievement", "prize", "goal")),
        ReminderIcon("sun", "Sun", Icons.Filled.WbSunny, listOf("morning", "weather", "day", "bright")),
        ReminderIcon("cloud", "Cloud", Icons.Filled.Cloud, listOf("weather", "sky", "storage")),
        ReminderIcon("umbrella", "Umbrella", Icons.Filled.Umbrella, listOf("rain", "weather")),
        ReminderIcon("eco", "Eco", Icons.Filled.Eco, listOf("nature", "green", "leaf", "environment")),
        ReminderIcon("bolt", "Energy", Icons.Filled.Bolt, listOf("energy", "power", "electric", "fast")),
    )

    private val index: Map<String, ReminderIcon> = all.associateBy { it.key }

    /** The full [ReminderIcon] for [key], or null if the key is null/unknown. */
    fun get(key: String?): ReminderIcon? = key?.let { index[it] }

    /** The [ImageVector] for [key], or null if the key is null/unknown. */
    fun imageOrNull(key: String?): ImageVector? = get(key)?.image

    /** Icons whose label, key, or keywords match [query]; the full list when blank. */
    fun search(query: String): List<ReminderIcon> =
        if (query.isBlank()) all else all.filter { it.matches(query) }
}
