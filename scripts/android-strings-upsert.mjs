import fs from "node:fs";
import path from "node:path";

const APPLY = process.argv.includes("--apply");
const ALL_DEFAULT = process.argv.includes("--all-default");

const ROOT = process.cwd();
const RES_DIR = path.join(ROOT, "app", "src", "main", "res");
const UPSERT_FILE = path.join(ROOT, "scripts", "android-strings-upsert.json");

function escapeXml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "\\'");
}

function escapeRegExp(value) {
    return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function getStringFiles() {
    return fs
        .readdirSync(RES_DIR, { withFileTypes: true })
        .filter((entry) => entry.isDirectory())
        .filter((entry) => entry.name === "values" || entry.name.startsWith("values-"))
        .map((entry) => {
            const filePath = path.join(RES_DIR, entry.name, "strings.xml");

            return {
                resourceDir: entry.name,
                filePath,
                exists: fs.existsSync(filePath),
            };
        })
        .filter((entry) => entry.exists);
}

function createStringsXml() {
    return `<?xml version="1.0" encoding="utf-8"?>\n<resources>\n</resources>\n`;
}

function ensureStringsFile(resourceDir) {
    const dirPath = path.join(RES_DIR, resourceDir);
    const filePath = path.join(dirPath, "strings.xml");

    if (!fs.existsSync(dirPath)) {
        fs.mkdirSync(dirPath, { recursive: true });
    }

    if (!fs.existsSync(filePath)) {
        fs.writeFileSync(filePath, createStringsXml(), "utf8");
    }

    return filePath;
}

function upsertString(xml, name, value) {
    const escapedName = escapeRegExp(name);
    const pattern = new RegExp(
        `(<string\\b(?=[^>]*\\bname="${escapedName}")[^>]*>)[\\s\\S]*?(<\\/string>)`,
        "m",
    );

    const nextValue = escapeXml(value);

    if (pattern.test(xml)) {
        return xml.replace(pattern, `$1${nextValue}$2`);
    }

    const newNode = `    <string name="${name}">${nextValue}</string>\n`;

    if (!xml.includes("</resources>")) {
        throw new Error("strings.xml no contiene </resources>");
    }

    return xml.replace("</resources>", `${newNode}</resources>`);
}

function loadUpsertFile() {
    if (!fs.existsSync(UPSERT_FILE)) {
        throw new Error(`No existe ${UPSERT_FILE}`);
    }

    const parsed = JSON.parse(fs.readFileSync(UPSERT_FILE, "utf8"));

    if (!Array.isArray(parsed.items)) {
        throw new Error("android-strings-upsert.json debe tener items: []");
    }

    return parsed.items;
}

function validateItems(items) {
    for (const item of items) {
        if (!item.name || typeof item.name !== "string") {
            throw new Error("Cada item debe tener name como string.");
        }

        if (!/^[a-zA-Z0-9_]+$/.test(item.name)) {
            throw new Error(`Nombre inválido para string resource: ${item.name}`);
        }

        if (!item.values || typeof item.values !== "object") {
            throw new Error(`El item ${item.name} debe tener values.`);
        }

        if (!("values" in item.values)) {
            throw new Error(`El item ${item.name} debe tener traducción base "values".`);
        }
    }
}

const stringFiles = getStringFiles();
const existingResourceDirs = new Set(stringFiles.map((file) => file.resourceDir));

const items = loadUpsertFile();
validateItems(items);

console.log("\nCambios detectados:");

const pendingUpdates = [];

for (const item of items) {
    console.log(`\n${item.name}`);

    const targetResourceDirs = new Set(Object.keys(item.values));

    if (ALL_DEFAULT) {
        for (const resourceDir of existingResourceDirs) {
            targetResourceDirs.add(resourceDir);
        }
    }

    for (const resourceDir of targetResourceDirs) {
        const value = item.values[resourceDir] ?? item.values.values;

        if (value === undefined || value === null) {
            continue;
        }

        const filePath = fs.existsSync(path.join(RES_DIR, resourceDir, "strings.xml"))
            ? path.join(RES_DIR, resourceDir, "strings.xml")
            : ensureStringsFile(resourceDir);

        pendingUpdates.push({
            filePath,
            resourceDir,
            name: item.name,
            value,
        });

        const action = fs.existsSync(filePath) ? "upsert" : "crear";
        console.log(`  - ${resourceDir}: ${action}`);
    }
}

if (!APPLY) {
    console.log("\nModo revisión. No se modificó ningún archivo.");
    console.log("Para aplicar:");
    console.log("node scripts/android-strings-upsert.mjs --apply");
    console.log("\nPara aplicar el valor base a todos los idiomas existentes:");
    console.log("node scripts/android-strings-upsert.mjs --apply --all-default");
    process.exit(0);
}

const updatesByFile = new Map();

for (const update of pendingUpdates) {
    const current = updatesByFile.get(update.filePath) ?? [];
    current.push(update);
    updatesByFile.set(update.filePath, current);
}

for (const [filePath, updates] of updatesByFile.entries()) {
    let xml = fs.readFileSync(filePath, "utf8");

    for (const update of updates) {
        xml = upsertString(xml, update.name, update.value);
    }

    fs.writeFileSync(filePath, xml, "utf8");
    console.log(`${path.relative(ROOT, filePath)}: ${updates.length} cambios`);
}

console.log("\n✅ Strings aplicados.");
console.log("Ahora corre:");
console.log("node scripts/android-strings-check.mjs check");
console.log("node scripts/android-strings-check.mjs audit");
console.log(".\\gradlew.bat assembleUniversalFossDebug");