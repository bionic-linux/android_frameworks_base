package api

import (
	"fmt"
	"testing"

	"android/soong/android"
	"android/soong/dexpreopt"
	"android/soong/java"
)

var PrepareForTestWithCombinedApis = android.FixtureRegisterWithContext(
	func(ctx android.RegistrationContext) {
		registerBuildComponents(ctx)
	})

// gatherRequiredDepsForTest gathers the module definitions used by
// PrepareForTestWithJavaDefaultModules.
//
// As functionality is moved out of here into separate FixturePreparer instances they should also
// be moved into GatherRequiredDepsForTest for use by tests that have not yet switched to use test
// fixtures.
func gatherRequiredDepsForTest() string {
	var bp string

	extraModules := []string{
		"core-lambda-stubs",
		"ext",
		"core.current.stubs",
		"legacy.core.platform.api.stubs",
		"stable.core.platform.api.stubs",

		"kotlin-stdlib",
		"kotlin-stdlib-jdk7",
		"kotlin-stdlib-jdk8",
		"kotlin-annotations",
		"stub-annotations",

		"aconfig-annotations-lib",
		"unsupportedappusage",
	}

	for _, extra := range extraModules {
		bp += fmt.Sprintf(`
			java_library {
				name: "%s",
				srcs: ["a.java"],
				sdk_version: "none",
				system_modules: "stable-core-platform-api-stubs-system-modules",
				compile_dex: true,
			}
		`, extra)
	}

	type droidstubsStruct struct {
		name        string
		apiSurface  string
		apiFile     string
		removedFile string
	}

	var publicDroidstubs = droidstubsStruct{
		name:        "api-stubs-docs-non-updatable",
		apiSurface:  "public",
		apiFile:     "api/current.txt",
		removedFile: "api/removed.txt",
	}
	var systemDroidstubs = droidstubsStruct{
		name:        "system-api-stubs-docs-non-updatable",
		apiSurface:  "system",
		apiFile:     "api/system-current.txt",
		removedFile: "api/system-removed.txt",
	}
	var testDroidstubs = droidstubsStruct{
		name:        "test-api-stubs-docs-non-updatable",
		apiSurface:  "test",
		apiFile:     "api/test-current.txt",
		removedFile: "api/test-removed.txt",
	}
	var moduleLibDroidstubs = droidstubsStruct{
		name:        "module-lib-api-stubs-docs-non-updatable",
		apiSurface:  "module-lib",
		apiFile:     "api/module-lib-current.txt",
		removedFile: "api/module-lib-removed.txt",
	}
	var systemServerDroidstubs = droidstubsStruct{
		// This module does not exist but is named this way for consistency
		name:        "system-server-api-stubs-docs-non-updatable",
		apiSurface:  "system-server",
		apiFile:     "api/system-server-current.txt",
		removedFile: "api/system-server-removed.txt",
	}
	var droidstubsStructs = []droidstubsStruct{
		publicDroidstubs,
		systemDroidstubs,
		testDroidstubs,
		moduleLibDroidstubs,
		systemServerDroidstubs,
	}

	extraApiLibraryModules := map[string]droidstubsStruct{
		"android_stubs_current.from-text":                  publicDroidstubs,
		"android_system_stubs_current.from-text":           systemDroidstubs,
		"android_test_stubs_current.from-text":             testDroidstubs,
		"android_module_lib_stubs_current.from-text":       moduleLibDroidstubs,
		"android_module_lib_stubs_current_full.from-text":  moduleLibDroidstubs,
		"android_system_server_stubs_current.from-text":    systemServerDroidstubs,
		"core.current.stubs.from-text":                     publicDroidstubs,
		"legacy.core.platform.api.stubs.from-text":         publicDroidstubs,
		"stable.core.platform.api.stubs.from-text":         publicDroidstubs,
		"core-lambda-stubs.from-text":                      publicDroidstubs,
		"android-non-updatable.stubs.from-text":            publicDroidstubs,
		"android-non-updatable.stubs.system.from-text":     systemDroidstubs,
		"android-non-updatable.stubs.test.from-text":       testDroidstubs,
		"android-non-updatable.stubs.module_lib.from-text": moduleLibDroidstubs,
		"android-non-updatable.stubs.test_module_lib":      moduleLibDroidstubs,
	}

	for _, droidstubs := range droidstubsStructs {
		bp += fmt.Sprintf(`
			droidstubs {
				name: "%s",
				api_surface: "%s",
				check_api: {
					current: {
						api_file: "%s",
						removed_api_file: "%s",
					}
				}
			}
		`,
			droidstubs.name,
			droidstubs.apiSurface,
			droidstubs.apiFile,
			droidstubs.removedFile,
		)
	}

	for libName, droidstubs := range extraApiLibraryModules {
		bp += fmt.Sprintf(`
            java_api_library {
                name: "%s",
                api_contributions: ["%s"],
            }
        `, libName, droidstubs.name+".api.contribution")
	}

	bp += `
		java_library {
			name: "framework",
			srcs: ["a.java"],
			sdk_version: "none",
			system_modules: "stable-core-platform-api-stubs-system-modules",
			aidl: {
				export_include_dirs: ["framework/aidl"],
			},
			compile_dex: true,
		}

		android_app {
			name: "framework-res",
			sdk_version: "core_platform",
		}`

	systemModules := []string{
		"core-public-stubs-system-modules",
		"core-module-lib-stubs-system-modules",
		"legacy-core-platform-api-stubs-system-modules",
		"stable-core-platform-api-stubs-system-modules",
		"core-public-stubs-system-modules.from-text",
		"core-module-lib-stubs-system-modules.from-text",
		"legacy-core-platform-api-stubs-system-modules.from-text",
		"stable-core-platform-api-stubs-system-modules.from-text",
	}

	for _, extra := range systemModules {
		bp += fmt.Sprintf(`
			java_system_modules {
				name: "%[1]s",
				libs: ["%[1]s-lib"],
			}
			java_library {
				name: "%[1]s-lib",
				sdk_version: "none",
				system_modules: "none",
			}
		`, extra)
	}

	// Make sure that the dex_bootjars singleton module is instantiated for the tests.
	bp += `
		dex_bootjars {
			name: "dex_bootjars",
		}
`

	bp += `
		all_apex_contributions {
			name: "all_apex_contributions",
		}
`
	return bp
}

const defaultJavaDir = "default/java"

var prepareForTestWithFrameworkDeps = android.GroupFixturePreparers(
	// The java default module definitions.
	android.FixtureAddTextFile(defaultJavaDir+"/Android.bp", gatherRequiredDepsForTest()),
	// Additional files needed when test disallows non-existent source.
	android.MockFS{
		// Needed for framework-res
		defaultJavaDir + "/AndroidManifest.xml": nil,
		// Needed for framework
		defaultJavaDir + "/framework/aidl": nil,
		// Needed for various deps defined in GatherRequiredDepsForTest()
		defaultJavaDir + "/a.java":                        nil,
		defaultJavaDir + "/api/current.txt":               nil,
		defaultJavaDir + "/api/removed.txt":               nil,
		defaultJavaDir + "/api/system-current.txt":        nil,
		defaultJavaDir + "/api/system-removed.txt":        nil,
		defaultJavaDir + "/api/test-current.txt":          nil,
		defaultJavaDir + "/api/test-removed.txt":          nil,
		defaultJavaDir + "/api/module-lib-current.txt":    nil,
		defaultJavaDir + "/api/module-lib-removed.txt":    nil,
		defaultJavaDir + "/api/system-server-current.txt": nil,
		defaultJavaDir + "/api/system-server-removed.txt": nil,

		// Needed for R8 rules on apps
		"build/make/core/proguard.flags":             nil,
		"build/make/core/proguard_basic_keeps.flags": nil,
		"prebuilts/cmdline-tools/shrinker.xml":       nil,
	}.AddToFixture(),
)

var prepareForTestWithCombinedApisDefaultModules = android.GroupFixturePreparers(
	java.PrepareForTestWithJavaBuildComponents,
	prepareForTestWithFrameworkDeps,
	dexpreopt.FixtureDisableDexpreoptBootImages(true),
	dexpreopt.FixtureDisableDexpreopt(true),
	// Add dexpreopt compat libs (android.test.base, etc.) and a fake dex2oatd module.
	dexpreopt.PrepareForTestWithDexpreoptCompatLibs,
)

func TestFilegroupDefaults(t *testing.T) {

	bp := `
		soong_config_module_type {
			name: "test_module",
			module_type: "combined_apis_defaults",
			config_namespace: "ANDROID",
			bool_variables: ["test_var"],
			properties: [
					"bootclasspath",
					"system_server_classpath",
			],
		}
		
		soong_config_bool_variable {
			name: "test_var",
		}
		
		test_module {
			name: "test_module_defaults",
			soong_config_variables: {
				test_var: {
					bootclasspath: [
							"framework-test",
					],
					system_server_classpath: [
							"service-test",
					],
				},
			},
		}

		combined_apis {
			name: "foo",
			defaults: ["test_module_defaults"],
			bootclasspath: [
				"framework-existing",
			],
			system_server_classpath: [
				"service-existing",
			],
		}
	`
	for _, test := range []struct {
		testVar               bool
		expectedBootclasspath []string
	}{
		{
			testVar:               true,
			expectedBootclasspath: []string{"framework-existing", "framework-test"},
		},
		{
			testVar:               false,
			expectedBootclasspath: []string{"framework-existing"},
		},
	} {
		t.Run(fmt.Sprintf("testVar:%t", test.testVar), func(t *testing.T) {
			result := android.GroupFixturePreparers(
				PrepareForTestWithCombinedApis,
				prepareForTestWithCombinedApisDefaultModules,
				android.PrepareForTestWithSoongConfigModuleBuildComponents,
				android.FixtureWithRootAndroidBp(bp),
				android.FixtureModifyProductVariables(func(variables android.FixtureProductVariables) {
					variables.VendorVars = map[string]map[string]string{
						"ANDROID": {
							"test_var": fmt.Sprintf("%t", test.testVar),
						},
					}
				}),
			).RunTest(t)
			module := result.ModuleForTests("foo", "").Module().(*CombinedApis)
			android.AssertDeepEquals(t, "bootclasspath", test.expectedBootclasspath, module.properties.Bootclasspath)
		})
	}
}
