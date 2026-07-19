import fs from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";

const [webPath, wasmPath, hostPath] = process.argv.slice(2);
if (!webPath || !wasmPath || !hostPath) throw new Error("missing conformance paths");

const web = await import(pathToFileURL(path.resolve(webPath)));
if (web.kotobaArtifact.requiredCapabilities.length !== 0)
  throw new Error("Time Machine Web graph requested a capability");
for (const [power, difficulty, expected] of [
  [14n, 10n, ":recruited"], [13n, 10n, ":cleared"], [10n, 10n, ":cleared"],
  [9n, 10n, ":retreat"], [-9223372036854775808n, 9223372036854775807n, ":retreat"],
]) {
  const instance = web.instantiateKotoba();
  if (instance["boundary-check"](power, difficulty, expected) !== 42n)
    throw new Error(`Web encounter boundary mismatch: ${power}/${difficulty}`);
}
if (web.instantiateKotoba().main() !== 42n || web.instantiateKotoba()["summary-check"]() !== 42n)
  throw new Error("Time Machine Web summary mismatch");

const host = await import(pathToFileURL(path.resolve(hostPath)));
const wasmBytes = fs.readFileSync(path.resolve(wasmPath));
for (const [power, difficulty, expected] of [
  [14n, 10n, ":recruited"], [13n, 10n, ":cleared"], [10n, 10n, ":cleared"],
  [9n, 10n, ":retreat"], [-9223372036854775808n, 9223372036854775807n, ":retreat"],
]) {
  const instance = await host.instantiateKotoba(wasmBytes);
  if (instance.instance.exports["boundary-check"](power, difficulty, expected) !== 42n)
    throw new Error(`Wasm encounter boundary mismatch: ${power}/${difficulty}`);
}
const wasm = await host.instantiateKotoba(wasmBytes);
if (wasm.instance.exports.main() !== 42n || wasm.instance.exports["summary-check"]() !== 42n)
  throw new Error("Time Machine Wasm summary mismatch");

console.log("ghosthacker-timemachine: bounded inherited-server Web/Wasm conformance passed");
