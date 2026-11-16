// Usage: node tools/validate.mjs   (cần: npm i mermaid@11 jsdom)
// Parse mọi block ```mermaid trong các file .md và extract ra mmd/*.mmd
import { JSDOM } from 'jsdom';
import fs from 'fs'; import path from 'path';
const dom = new JSDOM('<!DOCTYPE html><body></body>');
globalThis.window = dom.window; globalThis.document = dom.window.document;
Object.defineProperty(globalThis,'navigator',{value:dom.window.navigator,configurable:true});
const mermaid = (await import('mermaid')).default;
mermaid.initialize({startOnLoad:false});
const root = path.resolve(process.argv[2] || '.');
const out = path.join(root,'mmd'); fs.mkdirSync(out,{recursive:true});
let ok=0, fail=0;
function walk(d){ return fs.readdirSync(d,{withFileTypes:true}).flatMap(e=>{
  const p=path.join(d,e.name);
  if(e.isDirectory()) return ['node_modules','mmd','tools'].includes(e.name)?[]:walk(p);
  return p.endsWith('.md')?[p]:[]; }); }
for (const f of walk(root)) {
  const txt = fs.readFileSync(f,'utf8');
  const secId = path.basename(f).split('-')[0];
  const fence = /```mermaid\n([\s\S]*?)```/g;
  const idRe = /^###\s+((?:S|P|PH)\d+-D\d+|CL-D\d+)\b/gm;
  let m, n=0;
  while((m=fence.exec(txt))){
    n++;
    let id=null, h; idRe.lastIndex=0;
    const before = txt.slice(0,m.index);
    while((h=idRe.exec(before))) id=h[1];
    id = id || `${secId}-D${n}`;
    try { await mermaid.parse(m[1]); ok++; fs.writeFileSync(path.join(out,id+'.mmd'),m[1]); }
    catch(e){ fail++; console.log('FAIL',path.basename(f),id,String(e.message).split('\n').slice(0,3).join(' | ')); }
  }
}
console.log(`parsed OK=${ok} FAIL=${fail}`);
process.exit(fail?1:0);
