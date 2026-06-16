import fs from "node:fs";
import path from "node:path";

const MODE = process.argv[2] ?? "check";
const STRICT_LOCALE = process.argv.includes("--strict-locale");

const VALID_MODES = ["check", "unused", "audit"];

if (!VALID_MODES.includes(MODE)) {
    console.error(`Modo inválido: ${MODE}`);
    console.error(`Usa uno de estos: ${VALID_MODES.join(", ")}`);
    process.exit(1);
}

const ROOT = process.cwd();
const APP_DIR = path.join(ROOT, "app", "src", "main");
const RES_DIR = path.join(APP_DIR, "res");
const BASE_RESOURCE_DIR = "values";

const IGNORED_UNUSED_KEYS = new Set([
    "app_name",
]);

const IGNORED_UNUSED_PREFIXES = [
    "abc_",
    "mtrl_",
    "material_",
    "androidx_",
];

function readFilesRecursive(dir, extensions) {
    const result = [];

    if (!fs.existsSync(dir)) {
        return result;
    }

    for (const item of fs.readdirSync(dir)) {
        const fullPath = path.join(dir, item);
        const stat = fs.statSync(fullPath);

        if (stat.isDirectory()) {
            if (
                fullPath.includes(`${path.sep}build${path.sep}`) ||
                fullPath.includes(`${path.sep}.gradle${path.sep}`)
            ) {
                continue;
            }

            result.push(...readFilesRecursive(fullPath, extensions));
            continue;
        }

        if (extensions.includes(path.extname(fullPath))) {
            result.push(fullPath);
        }
    }

    return result;
}

function getStringFiles() {
    return fs
        .readdirSync(RES_DIR, { withFileTypes: true })
        .filter((entry) => entry.isDirectory())
        .filter((entry) => entry.name === "values" || entry.name.startsWith("values-"))
        .map((entry) => ({
            resourceDir: entry.name,
            filePath: path.join(RES_DIR, entry.name, "strings.xml"),
        }))
        .filter((entry) => fs.existsSync(entry.filePath));
}

function parseStringNames(xml) {
    const names = [];
    const pattern = /<string\b([^>]*)>[\s\S]*?<\/string>/g;

    let match;

    while ((match = pattern.exec(xml)) !== null) {
        const attrs = match[1];
        const nameMatch = attrs.match(/\bname="([^"]+)"/);

        if (nameMatch) {
            names.push(nameMatch[1]);
        }
    }

    return names;
}

function loadStrings() {
    const result = {};

    for (const file of getStringFiles()) {
        const xml = fs.readFileSync(file.filePath, "utf8");
        const names = parseStringNames(xml);
        const duplicates = names.filter((name, index) => names.indexOf(name) !== index);

        result[file.resourceDir] = {
            filePath: file.filePath,
            keys: new Set(names),
            duplicates: [...new Set(duplicates)].sort(),
        };
    }

    if (!result[BASE_RESOURCE_DIR]) {
        throw new Error(`No existe ${path.join(RES_DIR, BASE_RESOURCE_DIR, "strings.xml")}`);
    }

    return result;
}

function extractUsedStringKeysFromSource(source) {
    const keys = new Set();

    const patterns = [
        /\bR\.string\.([a-zA-Z0-9_]+)/g,
        /@string\/([a-zA-Z0-9_]+)/g,
    ];

    for (const pattern of patterns) {
        let match;

        while ((match = pattern.exec(source)) !== null) {
            keys.add(match[1]);
        }
    }

    return keys;
}

function extractUsedStringKeys() {
    const files = [
        ...readFilesRecursive(path.join(APP_DIR, "kotlin"), [".kt"]),
        ...readFilesRecursive(path.join(APP_DIR, "java"), [".java"]),
        ...readFilesRecursive(RES_DIR, [".xml"]),
    ];

    const used = new Set();

    for (const file of files) {
        const normalizedFile = file.split(path.sep).join("/");

        if (
            normalizedFile.includes("/res/values/") ||
            /\/res\/values-[^/]+\//.test(normalizedFile)
        ) {
            continue;
        }

        const source = fs.readFileSync(file, "utf8");

        for (const key of extractUsedStringKeysFromSource(source)) {
            used.add(key);
        }
    }

    return used;
}

function shouldIgnoreUnused(key) {
    if (IGNORED_UNUSED_KEYS.has(key)) {
        return true;
    }

    return IGNORED_UNUSED_PREFIXES.some((prefix) => key.startsWith(prefix));
}

function printList(title, items) {
    console.log(`\n${title}`);

    if (items.length === 0) {
        console.log("  ✅ Ninguno");
        return;
    }

    for (const item of items) {
        console.log(`  - ${item}`);
    }
}

function runCheck(strings, usedKeys) {
    const baseKeys = strings[BASE_RESOURCE_DIR].keys;
    let hasError = false;

    for (const [resourceDir, data] of Object.entries(strings)) {
        printList(
            `Duplicados en ${resourceDir}/strings.xml`,
            data.duplicates,
        );

        if (data.duplicates.length > 0) {
            hasError = true;
        }
    }

    const missingUsedInBase = [...usedKeys]
        .filter((key) => !baseKeys.has(key))
        .sort();

    printList(
        "Strings usados en código/XML pero faltantes en values/strings.xml",
        missingUsedInBase,
    );

    if (missingUsedInBase.length > 0) {
        hasError = true;
    }

    for (const [resourceDir, data] of Object.entries(strings)) {
        if (resourceDir === BASE_RESOURCE_DIR) {
            continue;
        }

        const missingComparedToBase = [...baseKeys]
            .filter((key) => !data.keys.has(key))
            .sort();

        const extraComparedToBase = [...data.keys]
            .filter((key) => !baseKeys.has(key))
            .sort();

        printList(
            `Faltan en ${resourceDir}/strings.xml comparado con values/strings.xml`,
            missingComparedToBase,
        );

        printList(
            `Sobran en ${resourceDir}/strings.xml comparado con values/strings.xml`,
            extraComparedToBase,
        );

        if (STRICT_LOCALE && (missingComparedToBase.length > 0 || extraComparedToBase.length > 0)) {
            hasError = true;
        }
    }

    if (hasError) {
        console.log("\n❌ Hay problemas en strings.xml.");
        process.exit(1);
    }

    console.log("\n✅ Revisión de strings terminada.");
}

function runUnused(strings, usedKeys) {
    const baseKeys = strings[BASE_RESOURCE_DIR].keys;

    const unused = [...baseKeys]
        .filter((key) => !usedKeys.has(key))
        .filter((key) => !shouldIgnoreUnused(key))
        .sort();

    printList("Strings posiblemente no usados en values/strings.xml", unused);

    console.log("\n✅ Revisión de unused terminada. No borres automáticamente sin revisar.");
}

function extractHardcodedTextsFromKotlin(source) {
    const results = [];

    const patterns = [
        /\bText\(\s*"([^"]{3,})"\s*\)/g,
        /\btitle\s*=\s*"([^"]{3,})"/g,
        /\bdescription\s*=\s*"([^"]{3,})"/g,
        /Toast\.makeText\([^,]+,\s*"([^"]{3,})"/g,
        /android\.widget\.Toast\.makeText\([^,]+,\s*"([^"]{3,})"/g,
    ];

    for (const pattern of patterns) {
        let match;

        while ((match = pattern.exec(source)) !== null) {
            const value = match[1];

            if (
                value.includes("$") ||
                value.startsWith("http") ||
                /^[A-Z0-9_]+$/.test(value) ||
                /^[0-9.,:;!?()\-\s]+$/.test(value)
            ) {
                continue;
            }

            results.push(value);
        }
    }

    return results;
}

function runAudit() {
    const kotlinFiles = readFilesRecursive(path.join(APP_DIR, "kotlin"), [".kt"]);
    const hardcoded = [];

    for (const file of kotlinFiles) {
        const source = fs.readFileSync(file, "utf8");
        const values = extractHardcodedTextsFromKotlin(source);

        for (const value of values) {
            hardcoded.push(`${path.relative(ROOT, file)} -> "${value}"`);
        }
    }

    printList("Textos hardcodeados sospechosos en Kotlin", hardcoded);

    if (hardcoded.length > 0) {
        console.log("\n⚠️ Revisa estos textos y muévelos a strings.xml si son visibles para el usuario.");
        process.exit(1);
    }

    console.log("\n✅ Auditoría básica terminada.");
}

const strings = loadStrings();
const usedKeys = extractUsedStringKeys();

if (MODE === "check") {
    runCheck(strings, usedKeys);
}

if (MODE === "unused") {
    runUnused(strings, usedKeys);
}

if (MODE === "audit") {
    runAudit();
}