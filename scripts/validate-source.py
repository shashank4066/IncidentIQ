"""Dependency-free repository checks (plus PyYAML if installed). Does not claim a build or deployment."""
from pathlib import Path
import json,sys,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
errors=[]
for p in root.rglob('*'):
 if not p.is_file() or any(part in {'target','node_modules','.git','dist'} for part in p.relative_to(root).parts):continue
 try:
  if p.suffix=='.json':json.loads(p.read_text(encoding='utf-8'))
  if p.name=='pom.xml':ET.parse(p)
 except Exception as e:errors.append(f'{p.relative_to(root)}: {e}')
try:
 import yaml
 class UniqueLoader(yaml.SafeLoader):pass
 def mapping(loader,node,deep=False):
  result={}
  for key,value in node.value:
   k=loader.construct_object(key,deep=deep)
   if k in result:raise ValueError('Duplicate YAML key: '+str(k))
   result[k]=loader.construct_object(value,deep=deep)
  return result
 UniqueLoader.add_constructor(yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG,mapping)
 for p in list(root.rglob('*.yml'))+list(root.rglob('*.yaml')):
  if 'node_modules' in p.parts or 'target' in p.parts:continue
  try:list(yaml.load_all(p.read_text(encoding='utf-8'),Loader=UniqueLoader))
  except Exception as e:errors.append(f'{p.relative_to(root)}: {e}')
except ImportError:print('PyYAML unavailable; YAML checks skipped.')
for required in ['README.md','pom.xml','frontend/src/main.jsx','compose.yml','compose.demo.yml','infra/k8s/base/kustomization.yaml','.github/workflows/ci.yml']:
 if not (root/required).is_file():errors.append('Missing '+required)
if errors:print('\n'.join(errors));sys.exit(1)
print('PASS: JSON, Maven XML, required files, and available YAML checks')
