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
 *
 * Additionally, different sieves are linked by a constant grind size offset:
 * For the same brew ratio and brew time, grindSize_sieveB = grindSize_sieveA + offset(A→B).
 * Use [SieveLinkedModel] to compute and apply these offsets across sieves.
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

    fun getCoefficients(): Triple<Double, Double, Double>? {
        if (!fitted) return null
        return Triple(a, b, c)
    }
}

/**
 * Links multiple sieves by computing constant grind size offsets between them.
 *
 * Given brews for the same coffee across different sieves, this model computes
 * a per-sieve offset such that:
 *   grindSize = a * targetYield + b * brewTime + c + sieveOffset[sieveId]
 *
 * The offset for a reference sieve is 0; other sieves have offsets relative to it.
 * This allows transferring a model fitted on one sieve to predict for another.
 */
class SieveLinkedModel(private val decayFactor: Double = 0.95) {

    private val perSieveModels = mutableMapOf<Long, GrindSizeModel>()
    private val sieveOffsets = mutableMapOf<Long, Double>()
    private var referenceSieveId: Long? = null
    private var globalA: Double = 0.0
    private var globalB: Double = 0.0
    private var fitted: Boolean = false

    /**
     * Fit a linked model across all sieves for a given coffee.
     * Groups brews by sieve, fits a model per sieve, then computes
     * the constant offset between sieves (difference in intercepts
     * while sharing the same slopes).
     *
     * @param brewsBySieve map of sieveId to the list of brews for that sieve
     * @return true if at least one sieve model was fitted
     */
    fun fit(brewsBySieve: Map<Long, List<Brew>>): Boolean {
        perSieveModels.clear()
        sieveOffsets.clear()
        referenceSieveId = null
        fitted = false

        // Fit individual models per sieve
        val fittedModels = mutableMapOf<Long, Triple<Double, Double, Double>>()
        for ((sieveId, brews) in brewsBySieve) {
            val model = GrindSizeModel(decayFactor)
            if (model.fit(brews)) {
                perSieveModels[sieveId] = model
                model.getCoefficients()?.let { fittedModels[sieveId] = it }
            }
        }

        if (fittedModels.isEmpty()) return false

        // Compute global slopes as weighted average of per-sieve slopes
        // (weighted by number of valid brews per sieve)
        val sieveCounts = brewsBySieve.mapValues { (_, brews) ->
            brews.count { it.grindSize > 0.0 && it.brewTime > 0 }.toDouble()
        }
        val totalCount = fittedModels.keys.sumOf { sieveCounts[it] ?: 0.0 }

        if (totalCount == 0.0) return false

        globalA = fittedModels.entries.sumOf { (sieveId, coeff) ->
            coeff.first * (sieveCounts[sieveId] ?: 0.0)
        } / totalCount

        globalB = fittedModels.entries.sumOf { (sieveId, coeff) ->
            coeff.second * (sieveCounts[sieveId] ?: 0.0)
        } / totalCount

        // Use the sieve with the most data as reference
        referenceSieveId = fittedModels.keys.maxByOrNull { sieveCounts[it] ?: 0.0 }

        // Compute per-sieve offset: offset_i = c_i + (a_i - globalA)*meanX1_i + (b_i - globalB)*meanX2_i
        // Simplified: since we want a constant shift at any point, we use the intercept difference
        // offset_i = c_i (individual intercept relative to global slopes)
        // Re-fit with shared slopes to get true offsets
        for ((sieveId, brews) in brewsBySieve) {
            val filtered = brews.filter { it.grindSize > 0.0 && it.brewTime > 0 }
                .sortedByDescending { it.timestamp }
            if (filtered.isEmpty()) continue

            val weights = filtered.indices.map { i -> Math.pow(decayFactor, i.toDouble()) }
            val sumW = weights.sum()

            // Compute residual: grindSize - globalA*targetYield - globalB*brewTime
            val offset = filtered.indices.sumOf { i ->
                weights[i] * (filtered[i].grindSize - globalA * filtered[i].targetYield - globalB * filtered[i].brewTime)
            } / sumW

            sieveOffsets[sieveId] = offset
        }

        // Normalize offsets relative to reference sieve
        val refOffset = sieveOffsets[referenceSieveId] ?: 0.0
        for (sieveId in sieveOffsets.keys.toList()) {
            sieveOffsets[sieveId] = (sieveOffsets[sieveId] ?: 0.0) - refOffset
        }

        fitted = true
        return true
    }

    /**
     * Predict grind size for a given sieve, target yield, and brew time.
     */
    fun predict(sieveId: Long, targetYield: Double, brewTime: Double): Double? {
        if (!fitted) return null
        val offset = sieveOffsets[sieveId] ?: return null
        val refOffset = 0.0 // reference is already normalized to 0
        return globalA * targetYield + globalB * brewTime + (sieveOffsets[referenceSieveId] ?: 0.0) + refOffset + offset +
            // Add back the reference intercept
            getBaseIntercept()
    }

    private fun getBaseIntercept(): Double {
        // The base intercept is the reference sieve's fitted offset (which is 0 after normalization)
        // plus the original reference offset before normalization
        val refModel = perSieveModels[referenceSieveId] ?: return 0.0
        val refCoeffs = refModel.getCoefficients() ?: return 0.0
        return refCoeffs.third
    }

    /**
     * Get the offset between two sieves.
     * Returns the grind size shift needed when switching from [fromSieveId] to [toSieveId].
     */
    fun getOffset(fromSieveId: Long, toSieveId: Long): Double? {
        if (!fitted) return null
        val fromOffset = sieveOffsets[fromSieveId] ?: return null
        val toOffset = sieveOffsets[toSieveId] ?: return null
        return toOffset - fromOffset
    }

    /**
     * Get all computed sieve offsets (relative to reference sieve).
     */
    fun getSieveOffsets(): Map<Long, Double> = sieveOffsets.toMap()

    /**
     * Get the reference sieve ID.
     */
    fun getReferenceSieveId(): Long? = referenceSieveId

    /**
     * Get the shared global slopes.
     */
    fun getGlobalCoefficients(): Pair<Double, Double>? {
        if (!fitted) return null
        return Pair(globalA, globalB)
    }
}
