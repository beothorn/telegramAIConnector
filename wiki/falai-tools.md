# FalAi tools

These tools leverage [Fal.ai](https://fal.ai) models to work with images and audio.

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

## describeImage
Runs an image through `fal-ai/llava-1.5-7b` and returns a description and OCR text.

Argument:
- `fileName` – uploaded image to analyse.

## audioToText
Transcribes an audio file with the `fal-ai/whisper` model.

Argument:
- `fileName` – name of the audio file located in the upload folder.
