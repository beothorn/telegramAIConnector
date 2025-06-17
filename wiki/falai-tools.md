# FalAi tools

These tools leverage [Fal.ai](https://fal.ai) models to work with images and audio.
To use it, you are required to install ffmpeg.

## editImage
Uses `fal-ai/flux-pro/kontext` to edit an uploaded image according to a text prompt and saves the result as a new file.

Arguments:
- `fileName` – name of the image in the upload folder.
- `prompt` – description of the desired modification.
- `outputFileName` – where to store the generated file.

## generateImage
Creates a new image from text using the `fal-ai/fast-sdxl` model.

Arguments:
- `prompt` – description of the image to generate.
- `outputFileName` – file name for the generated image.

## audioToText
Transcribes an audio file with the `fal-ai/whisper` model.

Argument:
- `fileName` – name of the audio file located in the upload folder.
