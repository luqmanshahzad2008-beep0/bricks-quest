package com.example.data

import android.content.Context
import android.content.SharedPreferences

class GameStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("bricks_breaker_quest_prefs", Context.MODE_PRIVATE)

    var highestStageUnlocked: Int
        get() = prefs.getInt("highest_stage_unlocked", 1)
        set(value) = prefs.edit().putInt("highest_stage_unlocked", value).apply()

    fun getStageStars(stage: Int): Int {
        return prefs.getInt("stage_stars_$stage", 0)
    }

    fun setStageStars(stage: Int, stars: Int) {
        val current = getStageStars(stage)
        if (stars > current) {
            prefs.edit().putInt("stage_stars_$stage", stars).apply()
        }
    }

    // Diamonds / Rubies (matches Picture 2 starting at 300)
    var diamonds: Int
        get() = prefs.getInt("diamonds_balance", 300)
        set(value) = prefs.edit().putInt("diamonds_balance", value.coerceAtLeast(0)).apply()

    fun addDiamonds(amount: Int) {
        diamonds += amount
    }

    var coins: Int
        get() = diamonds
        set(value) {
            diamonds = value
        }

    fun addCoins(amount: Int) {
        addDiamonds(amount)
    }

    var lightningCharges: Int
        get() = prefs.getInt("lightning_charges", 5)
        set(value) = prefs.edit().putInt("lightning_charges", value.coerceAtLeast(0)).apply()

    fun addLightningCharges(amount: Int) {
        lightningCharges += amount
    }

    var clusterCharges: Int
        get() = prefs.getInt("cluster_charges", 5)
        set(value) = prefs.edit().putInt("cluster_charges", value.coerceAtLeast(0)).apply()

    fun addClusterCharges(amount: Int) {
        clusterCharges += amount
    }

    var ballBalance: Int
        get() = prefs.getInt("total_balls_inventory", 57)
        set(value) = prefs.edit().putInt("total_balls_inventory", value.coerceAtLeast(0)).apply()

    fun addBalls(amount: Int) {
        ballBalance += amount
    }

    fun isChestClaimed(chestLevel: Int): Boolean {
        return prefs.getBoolean("chest_claimed_$chestLevel", false)
    }

    fun claimChest(chestLevel: Int) {
        prefs.edit().putBoolean("chest_claimed_$chestLevel", true).apply()
    }

    fun isPackUnlocked(packIndex: Int): Boolean {
        if (packIndex <= 1) return true
        val requiredStage = (packIndex - 1) * 100 + 1
        return prefs.getBoolean("pack_${packIndex}_unlocked", false) || highestStageUnlocked >= requiredStage
    }

    fun unlockPack(packIndex: Int) {
        prefs.edit().putBoolean("pack_${packIndex}_unlocked", true).apply()
    }

    var endlessHighScore: Int
        get() = prefs.getInt("endless_high_score", 0)
        set(value) {
            if (value > endlessHighScore) {
                prefs.edit().putInt("endless_high_score", value).apply()
            }
        }

    var rushHighScore: Int
        get() = prefs.getInt("rush_high_score", 0)
        set(value) {
            if (value > rushHighScore) {
                prefs.edit().putInt("rush_high_score", value).apply()
            }
        }

    var activeSkinId: String
        get() = prefs.getString("active_skin_id", "classic_pearl") ?: "classic_pearl"
        set(value) = prefs.edit().putString("active_skin_id", value).apply()

    fun isSkinUnlocked(skinId: String): Boolean {
        if (skinId == "classic_pearl") return true
        return prefs.getBoolean("skin_unlocked_$skinId", false)
    }

    fun unlockSkin(skinId: String) {
        prefs.edit().putBoolean("skin_unlocked_$skinId", true).apply()
    }

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean("haptics_enabled", true)
        set(value) = prefs.edit().putBoolean("haptics_enabled", value).apply()
}
