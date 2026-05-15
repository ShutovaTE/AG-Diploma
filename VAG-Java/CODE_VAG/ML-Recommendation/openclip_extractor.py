import argparse
import json
from PIL import Image
import numpy as np

try:
    import open_clip
    import torch
except Exception as e:
    print(json.dumps({"error": "missing_dependency", "message": str(e)}))
    raise


LABELS = [
    "portrait", "landscape", "abstract", "still life", "illustration",
    "sketch", "photograph", "digital art", "nature", "city", "people",
    "animals", "flowers", "food", "architecture", "black and white",
    "colorful", "surreal", "fantasy"
]


def avg_color(img: Image.Image):
    small = img.convert('RGB').resize((1, 1))
    return list(small.getpixel((0, 0)))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--image', required=True, help='Path to image file')
    parser.add_argument('--model', default='ViT-B-32', help='OpenCLIP model name')
    parser.add_argument('--topk', type=int, default=5, help='Top-k labels to return')
    args = parser.parse_args()

    try:
        model_name = args.model
        device = 'cpu'
        model, _, preprocess = open_clip.create_model_and_transforms(model_name, pretrained='openai')
        tokenizer = open_clip.get_tokenizer(model_name)
        model.to(device)
        model.eval()

        img = Image.open(args.image)
        img_t = preprocess(img).unsqueeze(0).to(device)

        with torch.no_grad():
            image_features = model.encode_image(img_t)
            # prepare text
            text_tokens = tokenizer(LABELS)
            text_tokens = text_tokens.to(device)
            text_features = model.encode_text(text_tokens)

            image_features = image_features / image_features.norm(dim=-1, keepdim=True)
            text_features = text_features / text_features.norm(dim=-1, keepdim=True)

            sims = (image_features @ text_features.T).squeeze(0).cpu().numpy()
            idxs = sims.argsort()[::-1][: args.topk]
            top_labels = [LABELS[i] for i in idxs.tolist()]

        out = {
            'tags': top_labels,
            'avg_color': avg_color(img)
        }
        print(json.dumps(out, ensure_ascii=False))
    except Exception as e:
        import traceback
        print(json.dumps({
            "error": "runtime_error",
            "message": str(e),
            "trace": traceback.format_exc()
        }))


if __name__ == '__main__':
    main()
