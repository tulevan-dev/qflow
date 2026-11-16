import os, json, re
from playwright.sync_api import sync_playwright
src='mmd'; out='render'
os.makedirs(out,exist_ok=True)
files=sorted(os.listdir(src))
res=[]
with sync_playwright() as p:
    b=p.chromium.launch(); pg=b.new_page(viewport={'width':1600,'height':1000})
    pg.set_content('<html><body></body></html>')
    pg.add_script_tag(path='node_modules/mermaid/dist/mermaid.min.js')
    pg.evaluate("mermaid.initialize({startOnLoad:false, securityLevel:'loose', theme:'default'})")
    for i,f in enumerate(files):
        code=open(os.path.join(src,f),encoding='utf8').read()
        try:
            r=pg.evaluate("""async ([id,code])=>{ const d=document.createElement('div'); d.id='c'+id; document.body.appendChild(d);
              const {svg}=await mermaid.render('g'+id, code, d); 
              const m=d.querySelector('svg'); const bb=m? m.getBoundingClientRect():{width:0,height:0};
              d.remove(); return {svg, w:Math.round(bb.width), h:Math.round(bb.height)}; }""",[i,code])
            name=f.replace('.mmd','.svg')
            open(os.path.join(out,name),'w',encoding='utf8').write(r['svg'])
            res.append((f,r['w'],r['h'],'ok'))
        except Exception as e:
            res.append((f,0,0,'ERR '+str(e)[:100]))
    b.close()
bad=[x for x in res if x[3]!='ok']
print('rendered',len(res)-len(bad),'errors',len(bad))
for x in bad: print(x)
big=sorted([x for x in res if x[3]=='ok'], key=lambda x:-x[1])[:12]
print('widest:'); [print(x) for x in big]
tall=sorted([x for x in res if x[3]=='ok'], key=lambda x:-x[2])[:8]
print('tallest:'); [print(x) for x in tall]
json.dump(res,open('render/_report.json','w'))
