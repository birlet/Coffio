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
     * Uses targetYield as x, grindSize as y.
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

        val xValues = data.map { it.targetYield }
        val yValues = data.map { it.grindSize }
        return fitXY(xValues, yValues)
    }

    /**
     * Fit a weighted linear regression on arbitrary x/y pairs.
     * Values are assumed to be ordered newest-first for decay weighting.
     *
     * @return true if the model was fitted successfully.
     */
    fun fitXY(xValues: List<Double>, yValues: List<Double>): Boolean {
        require(xValues.size == yValues.size) { "x and y must have the same size" }
        if (xValues.isEmpty()) {
            fitted = false
            return false
        }

        val weights = xValues.indices.map { i -> Math.pow(decayFactor, i.toDouble()) }

        if (xValues.size == 1) {
            slope = 0.0
            intercept = yValues.first()
            fitted = true
            return true
        }

        val sumW = weights.sum()
        val sumWX = xValues.indices.sumOf { i -> weights[i] * xValues[i] }
        val sumWY = yValues.indices.sumOf { i -> weights[i] * yValues[i] }
        val sumWXY = xValues.indices.sumOf { i -> weights[i] * xValues[i] * yValues[i] }
        val sumWX2 = xValues.indices.sumOf { i -> weights[i] * xValues[i] * xValues[i] }

        val denominator = sumW * sumWX2 - sumWX * sumWX
        if (denominator == 0.0) {
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
