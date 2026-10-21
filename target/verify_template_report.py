from pathlib import Path
from collections import Counter
import hashlib
import json
import re
import pdfplumber
from docx import Document

root=Path(__file__).resolve().parents[1]
manifest=json.loads((root/'target/report_manifest.json').read_text())
doc=Document(root/'docs/Campus Event Management System Project Report.docx')
text='\n'.join(p.text for p in doc.paragraphs)
checks={}
for filename in manifest['java_files']:
    p=root/filename
    src=p.read_text(encoding='utf-8-sig')
    src=re.sub(r'(SMTP_APP_PASSWORD\s*=\s*)"[^"]+"',r'\1"[SMTP APP PASSWORD]"',src)
    src=re.sub(r'(env\("DB_PASSWORD",\s*)"[^"]+"',r'\1"[DATABASE PASSWORD]"',src)
    checks[p.name]=re.sub(r'\s+','',src) in re.sub(r'\s+','',text)
font_counts=Counter()
overflow=[]
with pdfplumber.open(root/'target/project-report-qa-final/report.pdf') as pdf:
    for page_index,p in enumerate(pdf.pages,1):
        for char in p.chars:
            font_counts[(char['fontname'],round(char['size'],1))]+=1
            # Border at 24 points, title header deliberately above it.
            if char['x0'] < 23 or char['x1'] > p.width-23:
                overflow.append((page_index,char['text']))
    pages=len(pdf.pages)
result={'pages':pages,'all_java_listings_complete':all(checks.values()),'files':checks,
        'pdf_fonts_and_sizes':{str(k):v for k,v in font_counts.items()},
        'horizontal_overflow':overflow,
        'reference_unchanged':hashlib.sha256(Path(r'C:\Users\srikr\Downloads\FSD-II FRONT PAGES TEMPLATE.docx').read_bytes()).hexdigest()==manifest['reference_sha256']}
(root/'target/report_verification.json').write_text(json.dumps(result,indent=2))
print(json.dumps(result,indent=2))
