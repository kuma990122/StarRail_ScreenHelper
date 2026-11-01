"""Generate union alpha bounds across all animation frames, without re-encoding assets."""
from pathlib import Path
from PIL import Image, ImageSequence

root = Path(__file__).resolve().parents[1] / "app/src/main/assets/mimi"
for path in sorted(root.glob("*.webp")):
    with Image.open(path) as image:
        union = None
        for frame in ImageSequence.Iterator(image):
            bounds = frame.convert("RGBA").getchannel("A").getbbox()
            if bounds:
                union = bounds if union is None else (
                    min(union[0], bounds[0]), min(union[1], bounds[1]),
                    max(union[2], bounds[2]), max(union[3], bounds[3]),
                )
        if union:
            normalized = [value / image.size[index % 2] for index, value in enumerate(union)]
            path.with_suffix(".bounds").write_text(
                ",".join(f"{value:.8f}" for value in normalized), encoding="ascii")
            print(path.name, union)
