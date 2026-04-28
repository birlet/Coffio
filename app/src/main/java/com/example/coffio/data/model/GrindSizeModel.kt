package com.example.coffio.data.model

import com.example.coffio.data.local.entities.Brew

/**
 * Weighted multiple linear regression model that predicts grind size
 * from target yield and desired brew time for a specific coffee + sieve combination.
 *
 * Model: grindSize = a * targetYield + b * brewTime + c
 *
 * Brews are sorted by timestamp (newest first) and each successive older
 * brew is weighted by [decayFactor]^index, so the newest brew has weight 1,
 * the next has weight [decayFactor], then [decayFactor]², etc.
 */
class GrindSizeModel(private val decayFactor: Double = 0.95) {

    private var a: Double = 0.0  // coefficient for x1 (targetYield)
    private var b: Double = 0.0  // coefficient for x2 (brewTime)
    private var c: Double = 0.0  // intercept
    private var fitted: Boolean = false

    /**
     * Fit the weighted model from historical brews.
     * Uses targetYield and brewTime as inputs, grindSize as output.
     * Only brews with non-zero grindSize and brewTime are used.
     *
     * @return true if the model was fitted successfully.
     */
    fun fit(brews: List<Brew>): Boolean {
        val data = brews.filter { it.grindSize > 0.0 && it.brewTime > 0 }
            .sortedByDescending { it.timestamp }
        if (data.isEmpty()) {
            fitted = false
            return false
        }

        val x1 = data.map { it.targetYield }
        val x2 = data.map { it.brewTime.toDouble() }
        val y = data.map { it.grindSize }
        return fitMultiple(x1, x2, y)
    }

    /**
     * Fit a weighted multiple linear regression: y = a*x1 + b*x2 + c.
     * Values are assumed to be ordered newest-first for decay weighting.
     *
     * @return true if the model was fitted successfully.
     */
    fun fitMultiple(
        x1Values: List<Double>,
        x2Values: List<Double>,
        yValues: List<Double>
    ): Boolean {
        require(x1Values.size == x2Values.size && x1Values.size == yValues.size) {
            "All input lists must have the same size"
        }
        val n = x1Values.size
        if (n == 0) {
            fitted = false
            return false
        }

        val weights = x1Values.indices.map { i -> Math.pow(decayFactor, i.toDouble()) }

        if (n == 1) {
            a = 0.0
            b = 0.0
            c = yValues.first()
            fitted = true
            return true
        }

        // Weighted multiple linear regression using normal equations:
        // [Sw1_1  Sw12   Sw1 ] [a]   [Sw1Y]
        // [Sw12   Sw2_2  Sw2 ] [b] = [Sw2Y]
        // [Sw1    Sw2    Sw  ] [c]   [SwY ]
        val sw = weights.sum()
        val sw1 = weights.indices.sumOf { i -> weights[i] * x1Values[i] }
        val sw2 = weights.indices.sumOf { i -> weights[i] * x2Values[i] }
        val swy = weights.indices.sumOf { i -> weights[i] * yValues[i] }
        val sw11 = weights.indices.sumOf { i -> weights[i] * x1Values[i] * x1Values[i] }
        val sw22 = weights.indices.sumOf { i -> weights[i] * x2Values[i] * x2Values[i] }
        val sw12 = weights.indices.sumOf { i -> weights[i] * x1Values[i] * x2Values[i] }
        val sw1y = weights.indices.sumOf { i -> weights[i] * x1Values[i] * yValues[i] }
        val sw2y = weights.indices.sumOf { i -> weights[i] * x2Values[i] * yValues[i] }

        // Solve 3x3 system via Cramer's rule
        val det = sw11 * (sw22 * sw - sw2 * sw2) -
                  sw12 * (sw12 * sw - sw2 * sw1) +
                  sw1 * (sw12 * sw2 - sw22 * sw1)

        if (Math.abs(det) < 1e-12) {
            // Singular matrix — fall back to weighted average
            a = 0.0
            b = 0.0
            c = swy / sw
            fitted = true
            return true
        }

        a = (sw1y * (sw22 * sw - sw2 * sw2) -
             sw12 * (sw2y * sw - sw2 * swy) +
             sw1 * (sw2y * sw2 - sw22 * swy)) / det

        b = (sw11 * (sw2y * sw - sw2 * swy) -
             sw1y * (sw12 * sw - sw2 * sw1) +
             sw1 * (sw12 * swy - sw2y * sw1)) / det

        c = (sw11 * (sw22 * swy - sw2y * sw2) -
             sw12 * (sw12 * swy - sw2y * sw1) +
             sw1y * (sw12 * sw2 - sw22 * sw1)) / det

        fitted = true
        return true
    }

    /**
     * Predict grind size for the given [targetYield] and [brewTime].
     * Returns null when the model has not been fitted yet.
     */
    fun predict(targetYield: Double, brewTime: Double): Double? {
        if (!fitted) return null
        return a * targetYield + b * brewTime + c
    }
}
