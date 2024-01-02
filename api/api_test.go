package api

import (
	"fmt"
	"testing"

	"android/soong/android"
	"android/soong/java"
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
				java.PrepareForTestWithJavaDefaultModules,
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
			android.AssertDeepEquals(t, "bootclasspath", test.expectedBootclasspath, result.Config)
		})
	}
}
