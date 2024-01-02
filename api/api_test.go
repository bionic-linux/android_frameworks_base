package api

import (
	"testing"

	"android/soong/android"
)

func TestFilegroupDefaults(t *testing.T) {
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
		
		enable_crashrecovery_module {
				name: "test_module_defaults",
				soong_config_variables: {
						test_module: {
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
		bp).RunTest(t)
	android.AssertDeepEquals(t, "bootclasspath", []string{"framework-test", "framework-existing"}, result)
}
