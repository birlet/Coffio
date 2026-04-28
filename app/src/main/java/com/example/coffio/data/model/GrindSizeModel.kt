package com.example.coffio.data.model

import com.example.coffio.data.local.entities.Brew

/**
 * Weighted linear regression model that predicts grind size from target yield
 * for a specific coffee + sieve combination.
 *
 * Model: grindSize = slope * targetYield + intercept
 *
 * Brews are sorted by timestamp (newest first) and each successive older
 * brew is weighted by [decayFactor]^index, so the newest brew has weight 1,
 * the next has weight [decayFactor], then [decayFactor]², etc.
 *
 * Requires at least 2 data points to fit. With fewer points it falls back
 * to the average grind size (slope = 0).
 */
class GrindSizeModel(private val decayFactor: Double = 0.95) {

    private var slope: Double = 0.0
    private var intercept: Double = 0.0
    private var fitted: Boolean = false

    /**
     * Fit the weighted linear model from historical brews that share the same
     * coffeeId and sieveId. Only brews with a non-zero grindSize are used.
     * Newer brews receive higher weight via exponential decay.
     *
     * @return true if the model was fitted successfully.
     */
    fun fit(brews: List<Brew>): Boolean {
        val data = brews.filter { it.grindSize > 0.0 }
            .sortedByDescending { it.timestamp }
        if (data.isEmpty()) {
            fitted = false
            return false
        }

        // Compute weights: newest = 1, next = decayFactor, next = decayFactor², ...
        val weights = data.indices.map { i -> Math.pow(decayFactor, i.toDouble()) }

        if (data.size == 1) {
            slope = 0.0
            intercept = data.first().grindSize
            fitted = true
            return true
        }

        // Weighted linear regression:  y = grindSize, x = targetYield, w = weight
        val sumW = weights.sum()
        val sumWX = data.indices.sumOf { i -> weights[i] * data[i].targetYield }
        val sumWY = data.indices.sumOf { i -> weights[i] * data[i].grindSize }
        val sumWXY = data.indices.sumOf { i -> weights[i] * data[i].targetYield * data[i].grindSize }
        val sumWX2 = data.indices.sumOf { i -> weights[i] * data[i].targetYield * data[i].targetYield }

        val denominator = sumW * sumWX2 - sumWX * sumWX
        if (denominator == 0.0) {
            // All target yields are identical – can't compute a slope.
            slope = 0.0
            intercept = sumWY / sumW
        } else {
            slope = (sumW * sumWXY - sumWX * sumWY) / denominator
            intercept = (sumWY - slope * sumWX) / sumW
        }

        fitted = true
        return true
    }

    /**
     * Predict the best grind size for the given [targetYield].
     * Returns null when the model has not been fitted yet.
     */
    fun predict(targetYield: Double): Double? {
        if (!fitted) return null
        return slope * targetYield + intercept
    }
}
