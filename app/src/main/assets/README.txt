# AI Image Generator Model Files

This directory should contain the following files:

1. optimized_stable_diffusion.pt - A PyTorch Mobile optimized version of Stable Diffusion
2. stable_diffusion_vocab.txt - Vocabulary file for tokenization

Due to size constraints, these files are not included in the APK directly.
They should be downloaded or included separately during app deployment.

For development purposes:
- You can download lightweight versions of Stable Diffusion models
- Convert them to PyTorch Mobile format using the optimization tools
- Place them in this directory

Recommended models:
- Stable Diffusion 2.0 pruned (pruned to 50% of weights)
- MobileStableDiffusion
- SD-XL Turbo (with INT8 quantization)

For the vocabulary file, use the CLIP tokenizer vocabulary in text format.
