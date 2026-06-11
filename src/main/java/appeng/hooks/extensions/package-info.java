/**
 * Loader-neutral shims for NeoForge "extension methods" that AE2 overrides on vanilla types.
 * <p>
 * NeoForge injects extra virtual methods into vanilla classes (via {@code IItemExtension}, {@code IBlockExtension},
 * ...). Shared AE2 classes override several of them; on NeoForge those overrides are picked up by NeoForge's own
 * dispatch. To keep the same overrides compiling against vanilla (Fabric), each overridden extension method gets a shim
 * interface here that declares the method with the exact NeoForge signature and a default body replicating the NeoForge
 * default.
 * <ul>
 * <li>On NeoForge, a class implementing a shim interface must (and does) override the method, which simultaneously
 * overrides NeoForge's injected method, so NeoForge dispatch is unchanged.</li>
 * <li>On Fabric, the interfaces are the dispatch surface: loader code must invoke the hooks explicitly (events or
 * mixins, see PORTING_NOTES "Phase 2a" for the per-hook wiring status).</li>
 * </ul>
 */
package appeng.hooks.extensions;
