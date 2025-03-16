package com.aiart.stablediffusion.ml

import android.util.Log
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor

/**
 * Handles the text encoding part of Stable Diffusion
 * Converts text prompts to embeddings that guide the image generation
 */
class TextEncoder {
    companion object {
        private const val TAG = "TextEncoder"
        private const val MAX_LENGTH = 77 // CLIP tokenizer max length
    }

    /**
     * Encode text prompt into embeddings for the diffusion model
     */
    fun encode(prompt: String, negativePrompt: String = ""): FloatArray {
        val modelManager = ModelManager.getInstance()
        val textEncoder = modelManager.getTextEncoder() 
            ?: throw IllegalStateException("Text encoder model not loaded")

        try {
            Log.d(TAG, "Encoding prompt: $prompt")
            
            // For real implementation, we'd need a proper CLIP tokenizer
            // Here we use a simplified approach
            val tokenizedPrompt = simulateTokenization(prompt)
            val tokenizedNegativePrompt = if (negativePrompt.isNotEmpty()) {
                simulateTokenization(negativePrompt)
            } else {
                // Default to empty string tokens
                simulateTokenization("")
            }
            
            // Encode unconditional and conditional inputs
            val unconditionalEmbeddings = encodeTokens(textEncoder, tokenizedNegativePrompt)
            val conditionalEmbeddings = encodeTokens(textEncoder, tokenizedPrompt)
            
            // Combine embeddings for classifier-free guidance
            // Format: [unconditional_embeddings, conditional_embeddings]
            val embeddings = FloatArray(unconditionalEmbeddings.size + conditionalEmbeddings.size)
            System.arraycopy(unconditionalEmbeddings, 0, embeddings, 0, unconditionalEmbeddings.size)
            System.arraycopy(conditionalEmbeddings, 0, embeddings, unconditionalEmbeddings.size, conditionalEmbeddings.size)
            
            return embeddings
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding text", e)
            throw e
        }
    }

    /**
     * Encode tokenized input using the text encoder model
     */
    private fun encodeTokens(textEncoder: Module, tokens: LongArray): FloatArray {
        // Create input tensor from tokens
        val inputTensor = Tensor.fromBlob(tokens, longArrayOf(1, tokens.size.toLong()))
        
        // Run text encoder
        val outputTensor = textEncoder.forward(IValue.from(inputTensor)).toTensor()
        
        // Get output shape and data
        val shape = outputTensor.shape()
        val size = shape[1].toInt() * shape[2].toInt()
        
        // Copy to float array
        val embeddings = FloatArray(size)
        outputTensor.getDataAsFloatArray(embeddings)
        
        return embeddings
    }

    /**
     * Simple simulation of CLIP tokenization
     * In a real app, you would use a proper CLIP tokenizer
     */
    private fun simulateTokenization(text: String): LongArray {
        // This is a simulation only - actual tokenization would use the CLIP tokenizer
        // Create a token array of fixed length (MAX_LENGTH)
        val tokens = LongArray(MAX_LENGTH)
        
        // Set BOS (Beginning of Sequence) token at start
        tokens[0] = 49406 // BOS token ID for CLIP
        
        // Fill the middle with dummy values
        // In a real implementation, these would be actual token IDs from the tokenizer
        for (i in 1 until MAX_LENGTH - 1) {
            tokens[i] = 0 // Padding token
        }
        
        // Set EOS (End of Sequence) token at end
        tokens[MAX_LENGTH - 1] = 49407 // EOS token ID for CLIP
        
        return tokens
    }
}
