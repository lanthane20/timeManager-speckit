// Remplace, dans le build de production, les marqueurs ${VARIABLE} de environment.prod.ts par les
// variables d'environnement du déploiement. Usage : npm run inject-env [-- <dossier du build>]
import { readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const VARIABLES = ['GOOGLE_CLIENT_ID', 'APP_BASE_URL'];
// Les valeurs sont insérées dans des chaînes JavaScript : on refuse tout caractère qui pourrait en
// sortir (guillemets, antislash, espaces…).
const SAFE_VALUE = /^[\w.:/-]+$/;

const buildDir = process.argv[2] ?? 'dist/frontend/browser';

const invalid = VARIABLES.filter(name => !SAFE_VALUE.test(process.env[name] ?? ''));
if (invalid.length > 0) {
  console.error(`Variables absentes ou invalides : ${invalid.join(', ')}`);
  process.exit(1);
}

const values = Object.fromEntries(
  VARIABLES.map(name => [name, process.env[name].replace(/\/+$/, '')])
);

let updatedFiles = 0;
for (const file of readdirSync(buildDir).filter(name => name.endsWith('.js'))) {
  const path = join(buildDir, file);
  const source = readFileSync(path, 'utf8');
  const result = VARIABLES.reduce(
    (text, name) => text.replaceAll(`\${${name}}`, values[name]),
    source
  );
  if (result !== source) {
    writeFileSync(path, result);
    updatedFiles++;
  }
}

if (updatedFiles === 0) {
  console.error(`Aucun marqueur trouvé dans ${buildDir} : le build utilise-t-il environment.prod.ts ?`);
  process.exit(1);
}
console.warn(`Configuration de production injectée dans ${updatedFiles} fichier(s).`);
