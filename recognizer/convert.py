from transformers import AutoImageProcessor, AutoModelForImageClassification
import torch
from PIL import Image
from transformers import AutoTokenizer

model_name = "DenisNovac/nsfw_image_detection"

model = AutoModelForImageClassification.from_pretrained(model_name, torchscript=True, return_dict=False)

processor = AutoImageProcessor.from_pretrained(model_name)

image = Image.open("images/hentai.jpg")
image_inputs = processor(images=image, return_tensors="pt")

config = {'forward': [image_inputs['pixel_values']]}
converted = torch.jit.trace_module(model,  config)

torch.jit.save(converted, "nsfw_model.pt")
