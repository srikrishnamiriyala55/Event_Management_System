from PIL import Image, ImageOps, ImageDraw
from pathlib import Path
import sys

folder = Path(sys.argv[1])
pages = sorted(folder.glob('page-*.png'))
for start in range(0, len(pages), 6):
    sheet = Image.new('RGB', (1500, 2150), '#d3d8df')
    draw = ImageDraw.Draw(sheet)
    for offset, path in enumerate(pages[start:start + 6]):
        im = Image.open(path).convert('RGB')
        im.thumbnail((480, 1000))
        x = (offset % 3) * 500 + (500 - im.width) // 2
        y = (offset // 3) * 1075 + 35
        sheet.paste(im, (x, y))
        draw.text((x, y - 24), path.stem, fill='black')
    sheet.save(folder / f'contact-{start // 6 + 1:02}.png')
print('Pages',len(pages))
