package com.example.engine

import java.util.Random
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

class MinMaxScaler {
    var minVal: Double = 0.0
    var maxVal: Double = 1.0

    fun fit(data: List<Double>) {
        if (data.isEmpty()) return
        minVal = data.minOrNull() ?: 0.0
        maxVal = data.maxOrNull() ?: 1.0
        if (minVal == maxVal) {
            maxVal += 1e-5
        }
    }

    fun transform(value: Double): Double {
        return (value - minVal) / (maxVal - minVal)
    }

    fun inverseTransform(normalized: Double): Double {
        return normalized * (maxVal - minVal) + minVal
    }

    fun transformList(values: List<Double>): List<Double> {
        return values.map { transform(it) }
    }
}

/**
 * 3-Hidden Layer MLP Neural Network with Backpropagation
 * Layer Sizes: 60 (Input) -> 128 (ReLU) -> 64 (ReLU) -> 32 (ReLU) -> 1 (Linear/Output)
 */
class BackpropNeuralNetwork(
    val inputSize: Int = 60,
    val h1Size: Int = 128,
    val h2Size: Int = 64,
    val h3Size: Int = 32,
    val outputSize: Int = 1
) {
    // Weights and Biases
    private var w1 = Array(h1Size) { DoubleArray(inputSize) }
    private var b1 = DoubleArray(h1Size)

    private var w2 = Array(h2Size) { DoubleArray(h1Size) }
    private var b2 = DoubleArray(h2Size)

    private var w3 = Array(h3Size) { DoubleArray(h2Size) }
    private var b3 = DoubleArray(h3Size)

    private var w4 = Array(outputSize) { DoubleArray(h3Size) }
    private var b4 = DoubleArray(outputSize)

    init {
        initializeWeights()
    }

    private fun initializeWeights() {
        val rand = Random(42) // Seeded for reproducibility
        
        // He (Kaiming) Normal Initialization for ReLU layers
        fillHe(w1, inputSize, rand)
        fillHe(w2, h1Size, rand)
        fillHe(w3, h2Size, rand)
        
        // Xavier/Glorot Normal for output layer
        val scale = sqrt(2.0 / (h3Size + outputSize))
        for (i in 0 until outputSize) {
            for (j in 0 until h3Size) {
                w4[i][j] = rand.nextGaussian() * scale
            }
            b4[i] = 0.0
        }
    }

    private fun fillHe(weights: Array<DoubleArray>, previousLayerSize: Int, rand: Random) {
        val std = sqrt(2.0 / previousLayerSize)
        for (i in weights.indices) {
            for (j in weights[i].indices) {
                weights[i][j] = rand.nextGaussian() * std
            }
            b1.set(i, 0.0) // Bias starting at zero
        }
    }

    // Activations
    private fun relu(x: Double) = if (x > 0.0) x else 0.0
    private fun reluDerivative(x: Double) = if (x > 0.0) 1.0 else 0.0

    /**
     * Train using gradient descent on local datasets
     * Returns a list of Training History Losses
     */
    fun train(
        inputs: List<DoubleArray>, // list of double arrays of size [60]
        targets: List<Double>,      // target values (length = inputs.size)
        epochs: Int = 200,
        learningRate: Double = 0.01,
        onEpochComplete: (epoch: Int, mseLoss: Double, valLoss: Double) -> Unit = { _, _, _ -> }
    ): List<Double> {
        val history = mutableListOf<Double>()
        val nSamples = inputs.size
        if (nSamples == 0) return history

        // Validation split 20%
        val valCount = (nSamples * 0.2).toInt()
        val trainCount = nSamples - valCount

        val trainInputs = inputs.take(trainCount)
        val trainTargets = targets.take(trainCount)

        val valInputs = inputs.drop(trainCount)
        val valTargets = targets.drop(trainCount)

        var lr = learningRate

        for (epoch in 1..epochs) {
            var totalMse = 0.0

            // Reduce learning rate after epochs (ReduceLROnPlateau simulation)
            if (epoch == 100) lr *= 0.5
            if (epoch == 150) lr *= 0.5

            for (s in 0 until trainCount) {
                val x = trainInputs[s]
                val t = trainTargets[s]

                // --- 1. FORWARD PASS ---
                // Layer 1
                val z1 = DoubleArray(h1Size)
                val a1 = DoubleArray(h1Size)
                for (i in 0 until h1Size) {
                    var sum = b1[i]
                    for (j in 0 until inputSize) {
                        sum += w1[i][j] * x[j]
                    }
                    z1[i] = sum
                    a1[i] = relu(sum)
                }

                // Layer 2
                val z2 = DoubleArray(h2Size)
                val a2 = DoubleArray(h2Size)
                for (i in 0 until h2Size) {
                    var sum = b2[i]
                    for (j in 0 until h1Size) {
                        sum += w2[i][j] * a1[j]
                    }
                    z2[i] = sum
                    a2[i] = relu(sum)
                }

                // Layer 3
                val z3 = DoubleArray(h3Size)
                val a3 = DoubleArray(h3Size)
                for (i in 0 until h3Size) {
                    var sum = b3[i]
                    for (j in 0 until h2Size) {
                        sum += w3[i][j] * a2[j]
                    }
                    z3[i] = sum
                    a3[i] = relu(sum)
                }

                // Layer 4 (Output Linear)
                var pred = b4[0]
                for (j in 0 until h3Size) {
                    pred += w4[0][j] * a3[j]
                }

                val err = pred - t
                totalMse += err * err

                // --- 2. BACKPROPAGATION ---
                // dOut / dPred = err
                val dOut = err

                // Gradients for w4, b4
                val dw4 = DoubleArray(h3Size)
                val db4 = dOut
                val da3 = DoubleArray(h3Size) // gradient w.r.t layer 3 activations
                for (j in 0 until h3Size) {
                    dw4[j] = dOut * a3[j]
                    da3[j] = dOut * w4[0][j]
                }

                // Node delta for Layer 3 (ReLU)
                val delta3 = DoubleArray(h3Size)
                for (i in 0 until h3Size) {
                    delta3[i] = da3[i] * reluDerivative(z3[i])
                }

                // Gradients for w3, b3
                val dw3 = Array(h3Size) { DoubleArray(h2Size) }
                val db3 = DoubleArray(h3Size)
                val da2 = DoubleArray(h2Size)
                for (i in 0 until h3Size) {
                    db3[i] = delta3[i]
                    for (j in 0 until h2Size) {
                        dw3[i][j] = delta3[i] * a2[j]
                        da2[j] += delta3[i] * w3[i][j]
                    }
                }

                // Node delta for Layer 2
                val delta2 = DoubleArray(h2Size)
                for (i in 0 until h2Size) {
                    delta2[i] = da2[i] * reluDerivative(z2[i])
                }

                // Gradients for w2, b2
                val dw2 = Array(h2Size) { DoubleArray(h1Size) }
                val db2 = DoubleArray(h2Size)
                val da1 = DoubleArray(h1Size)
                for (i in 0 until h2Size) {
                    db2[i] = delta2[i]
                    for (j in 0 until h1Size) {
                        dw2[i][j] = delta2[i] * a1[j]
                        da1[j] += delta2[i] * w2[i][j]
                    }
                }

                // Node delta for Layer 1
                val delta1 = DoubleArray(h1Size)
                for (i in 0 until h1Size) {
                    delta1[i] = da1[i] * reluDerivative(z1[i])
                }

                // Gradients for w1, b1
                val dw1 = Array(h1Size) { DoubleArray(inputSize) }
                val db1 = DoubleArray(h1Size)
                for (i in 0 until h1Size) {
                    db1[i] = delta1[i]
                    for (j in 0 until inputSize) {
                        dw1[i][j] = delta1[i] * x[j]
                    }
                }

                // --- 3. WEIGHT UPDATES (SGD) ---
                // Layer 4
                for (j in 0 until h3Size) {
                    w4[0][j] -= lr * dw4[j]
                }
                b4[0] -= lr * db4

                // Layer 3
                for (i in 0 until h3Size) {
                    b3[i] -= lr * db3[i]
                    for (j in 0 until h2Size) {
                        w3[i][j] -= lr * dw3[i][j]
                    }
                }

                // Layer 2
                for (i in 0 until h2Size) {
                    b2[i] -= lr * db2[i]
                    for (j in 0 until h1Size) {
                        w2[i][j] -= lr * dw2[i][j]
                    }
                }

                // Layer 1
                for (i in 0 until h1Size) {
                    b1[i] -= lr * db1[i]
                    for (j in 0 until inputSize) {
                        w1[i][j] -= lr * dw1[i][j]
                    }
                }
            }

            val epochMse = totalMse / trainCount
            history.add(epochMse)

            // Compute validation MSE
            var valMse = 0.0
            if (valInputs.isNotEmpty()) {
                for (s in valInputs.indices) {
                    val predVal = predict(valInputs[s])
                    val errVal = predVal - valTargets[s]
                    valMse += errVal * errVal
                }
                valMse /= valInputs.size
            } else {
                valMse = epochMse
            }

            onEpochComplete(epoch, epochMse, valMse)
        }
        return history
    }

    /**
     * Single Forward Pass Prediction on inputs
     */
    fun predict(x: DoubleArray): Double {
        // Layer 1
        val a1 = DoubleArray(h1Size)
        for (i in 0 until h1Size) {
            var sum = b1[i]
            for (j in 0 until inputSize) {
                sum += w1[i][j] * x[j]
            }
            a1[i] = relu(sum)
        }

        // Layer 2
        val a2 = DoubleArray(h2Size)
        for (i in 0 until h2Size) {
            var sum = b2[i]
            for (j in 0 until h1Size) {
                sum += w2[i][j] * a1[j]
            }
            a2[i] = relu(sum)
        }

        // Layer 3
        val a3 = DoubleArray(h3Size)
        for (i in 0 until h3Size) {
            var sum = b3[i]
            for (j in 0 until h2Size) {
                sum += w3[i][j] * a2[j]
            }
            a3[i] = relu(sum)
        }

        // Output Linear
        var pred = b4[0]
        for (j in 0 until h3Size) {
            pred += w4[0][j] * a3[j]
        }

        return pred
    }

    /**
     * Calculates model performance stats on double lists (actuals vs predictions)
     */
    companion object {
        fun evaluate(actuals: List<Double>, predictions: List<Double>): PerformanceMetrics {
            val n = actuals.size
            if (n == 0) return PerformanceMetrics(0.0, 0.0, 0.0, 0.0, 0.0)

            var sumSqErr = 0.0
            var sumAbsErr = 0.0
            var sumAbsPctErr = 0.0
            var sumActual = 0.0

            for (i in 0 until n) {
                val act = actuals[i]
                val pred = predictions[i]
                val err = act - pred
                sumSqErr += err * err
                sumAbsErr += abs(err)
                sumAbsPctErr += if (act != 0.0) abs(err) / act else 0.0
                sumActual += act
            }

            val mse = sumSqErr / n
            val rmse = sqrt(mse)
            val mae = sumAbsErr / n
            val mape = (sumAbsPctErr / n) * 100.0

            // R2 score
            val avgActual = sumActual / n
            var sumTotalSq = 0.0
            for (i in 0 until n) {
                val diff = actuals[i] - avgActual
                sumTotalSq += diff * diff
            }
            val r2 = if (sumTotalSq != 0.0) 1.0 - (sumSqErr / sumTotalSq) else 0.0

            return PerformanceMetrics(
                mse = mse,
                rmse = rmse,
                mae = mae,
                mape = mape,
                r2 = r2
            )
        }
    }
}

data class PerformanceMetrics(
    val mse: Double,
    val rmse: Double,
    val mae: Double,
    val mape: Double,
    val r2: Double
)
