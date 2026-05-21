import argparse
import json
from PIL import Image

try:
    import open_clip
    import torch
except Exception as e:
    print(json.dumps({"error": "missing_dependency", "message": str(e)}))
    raise


LABELS = [
    "portrait",
    "landscape",
    "abstract",
    "still life",
    "illustration",
    "sketch",
    "photograph",
    "digital art",
    "nature",
    "city",
    "people",
    "animals",
    "flowers",
    "food",
    "architecture",
    "black and white",
    "colorful",
    "surreal",
    "fantasy"
]

DEVICE = "cpu"

MODEL = None
PREPROCESS = None
TOKENIZER = None
TEXT_FEATURES = None


def load_model():
    global MODEL
    global PREPROCESS
    global TOKENIZER
    global TEXT_FEATURES

    if MODEL is not None:
        return

    model_name = "ViT-B-32"

    MODEL, _, PREPROCESS = open_clip.create_model_and_transforms(
        model_name,
        pretrained="openai"
    )

    TOKENIZER = open_clip.get_tokenizer(model_name)

    MODEL.to(DEVICE)
    MODEL.eval()

    with torch.no_grad():
        text_tokens = TOKENIZER(LABELS).to(DEVICE)

        TEXT_FEATURES = MODEL.encode_text(text_tokens)
        TEXT_FEATURES /= TEXT_FEATURES.norm(
            dim=-1,
            keepdim=True
        )


def avg_color(img):
    small = img.convert("RGB").resize((1, 1))
    return list(small.getpixel((0, 0)))


def classify(image_path, topk=5):
    load_model()

    img = Image.open(image_path)

    image_tensor = PREPROCESS(img).unsqueeze(0).to(DEVICE)

    with torch.no_grad():
        image_features = MODEL.encode_image(image_tensor)

        image_features /= image_features.norm(
            dim=-1,
            keepdim=True
        )

        similarities = (
                image_features @ TEXT_FEATURES.T
        ).squeeze(0).cpu().numpy()

    indices = similarities.argsort()[::-1][:topk]

    labels = [LABELS[i] for i in indices.tolist()]

    return {
        "tags": labels,
        "avg_color": avg_color(img)
    }


def main():
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "--image",
        required=True
    )

    parser.add_argument(
        "--topk",
        type=int,
        default=5
    )

    args = parser.parse_args()

    try:
        result = classify(
            args.image,
            args.topk
        )

        print(
            json.dumps(
                result,
                ensure_ascii=False
            )
        )

    except Exception as e:
        import traceback

        print(json.dumps({
            "error": "runtime_error",
            "message": str(e),
            "trace": traceback.format_exc()
        }))


if __name__ == "__main__":
    main()