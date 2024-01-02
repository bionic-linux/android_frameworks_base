package api

import (
	//"fmt"
	//"os"
	"testing"

	"android/soong/android"
)

func registerTestBuildComponents(ctx android.RegistrationContext) {
	ctx.RegisterModuleType("combined_apis", combinedApisModuleFactory1)
	ctx.RegisterModuleType("combined_apis_defaults", CombinedApisModuleDefaultsFactory)
}

func combinedApisModuleFactory1() android.Module {
	module := &CombinedApis{}
	module.AddProperties(&module.properties)
	android.InitAndroidModule(module)
	android.InitDefaultableModule(module)
	return module
}

var PrepareForTestWithCombinedApis = android.FixtureRegisterWithContext(
	func(ctx android.RegistrationContext) { registerTestBuildComponents(ctx) })

func TestFilegroupDefaults(t *testing.T) {
	//stub, _ := os.ReadFile("StubLibraries.bp")

	bp := android.FixtureAddTextFile("p/Android.bp", `
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
	`)
	result := android.GroupFixturePreparers(
		PrepareForTestWithCombinedApis,
		android.PrepareForTestWithDefaults,
		android.PrepareForTestWithVisibility,
		android.PrepareForTestWithSoongConfigModuleBuildComponents,
		bp).RunTest(t)
	android.AssertDeepEquals(t, "bootclasspath",
		[]string{"framework-test", "framework-existing"}, result.Config)
}
