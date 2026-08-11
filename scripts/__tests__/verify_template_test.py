import importlib.util
import re
import tomllib
import unittest
from pathlib import Path
from unittest.mock import patch


REPO_ROOT = Path(__file__).resolve().parents[2]
VERIFY_TEMPLATE = REPO_ROOT / "scripts" / "verify_template.py"

spec = importlib.util.spec_from_file_location("verify_template", VERIFY_TEMPLATE)
verify_template = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verify_template)


LYNX_SDK_COMPANION_MODULES = (
    "lynx-jssdk",
    "lynx-trace",
    "lynx-service-image",
    "lynx-service-log",
    "lynx-service-http",
    "lynx-service-devtool",
    "lynx-devtool",
)


def maven_module_metadata(*dependencies):
    return {
        "variants": [
            {
                "name": "releaseVariantReleaseRuntimePublication",
                "dependencies": [
                    {
                        "group": group,
                        "module": module,
                        "version": {"requires": version},
                    }
                    for group, module, version in dependencies
                ],
            },
        ],
    }


class AndroidLynxAlignmentTest(unittest.TestCase):
    def test_template_lynx_plugin_matches_version_catalog(self):
        expected_lynx_version, _ = verify_template.template_android_lynx_versions()

        ready, reason = verify_template.validate_template_android_lynx_versions()

        self.assertTrue(ready, reason)
        self.assertIn(expected_lynx_version, reason)

    def test_template_host_keeps_strict_lynx_dependency_contract(self):
        catalog = tomllib.loads(verify_template.TEMPLATE_ANDROID_VERSION_CATALOG.read_text())
        libraries_by_module = {}
        for alias, library in catalog["libraries"].items():
            coordinate = library.get("module")
            if coordinate:
                group, module = coordinate.split(":", 1)
            else:
                group = library.get("group")
                module = library.get("name")
            if group == verify_template.LYNX_GROUP:
                libraries_by_module[module] = (alias, library["version"]["ref"])

        lynx_alias, lynx_version_ref = libraries_by_module["lynx"]
        primjs_alias, primjs_version_ref = libraries_by_module["primjs"]
        companion_aliases = []
        for module in LYNX_SDK_COMPANION_MODULES:
            alias, version_ref = libraries_by_module[module]
            self.assertEqual(version_ref, lynx_version_ref, module)
            companion_aliases.append(alias)

        source = (verify_template.TEMPLATE_ANDROID_DIR / "app" / "build.gradle.kts").read_text()
        source = re.sub(r"/\*.*?\*/|//[^\n]*", "", source, flags=re.DOTALL)
        source = re.sub(r"\s+", "", source)
        direct_pin = (
            f"implementation(libs.{lynx_alias})"
            f"{{version{{strictly(libs.versions.{lynx_version_ref}.get())}}"
        )
        constraints_index = source.index("constraints{")
        self.assertEqual(source[:constraints_index].count(direct_pin), 1)

        grouped_constraints = re.search(
            r"listOf\((?P<aliases>libs(?:\.[A-Za-z0-9_]+)+(?:,libs(?:\.[A-Za-z0-9_]+)+)*,?)\)"
            r"\.forEach\{(?P<variable>[A-Za-z_][A-Za-z0-9_]*)->"
            r"implementation\((?P=variable)\)\{version\{strictly\(libs\.versions\."
            r"(?P<version_ref>[A-Za-z_][A-Za-z0-9_]*)\.get\(\)\)",
            source[constraints_index:],
        )
        self.assertIsNotNone(grouped_constraints)
        self.assertEqual(
            set(grouped_constraints.group("aliases").strip(",").split(",")),
            {"libs." + re.sub(r"[-_.]+", ".", alias) for alias in companion_aliases},
        )
        self.assertEqual(grouped_constraints.group("version_ref"), lynx_version_ref)

        primjs_constraint = (
            f"implementation(libs.{primjs_alias})"
            f"{{version{{strictly(libs.versions.{primjs_version_ref}.get())}}"
        )
        self.assertEqual(source[constraints_index:].count(primjs_constraint), 1)

    def test_maven_metadata_accepts_coherent_lynx_versions(self):
        metadata = {
            "com/tiktok/sparkling/sparkling": maven_module_metadata(
                ("org.lynxsdk.lynx", "lynx", "3.9.0"),
                ("org.lynxsdk.lynx", "lynx-jssdk", "3.9.0"),
                ("org.lynxsdk.lynx", "lynx-trace", "3.9.0"),
                ("org.lynxsdk.lynx", "primjs", "3.8.0-alpha.6"),
                ("org.lynxsdk.lynx", "lynx-service-image", "3.9.0"),
                ("org.lynxsdk.lynx", "lynx-service-http", "3.9.0"),
            ),
            "com/tiktok/sparkling/sparkling-method": maven_module_metadata(
                ("org.lynxsdk.lynx", "lynx", "3.9.0"),
            ),
            "com/tiktok/sparkling/sparkling-debug-tool": maven_module_metadata(
                ("org.lynxsdk.lynx", "lynx", "3.9.0"),
                ("org.lynxsdk.lynx", "lynx-service-log", "3.9.0"),
                ("org.lynxsdk.lynx", "lynx-service-devtool", "3.9.0"),
                ("org.lynxsdk.lynx", "lynx-devtool", "3.9.0"),
                ("org.lynxsdk.lynx", "debug-router", "0.0.18"),
            ),
        }

        ready, reason = verify_template.validate_maven_lynx_versions(
            metadata,
            "3.9.0",
            "3.8.0-alpha.6",
        )

        self.assertTrue(ready, reason)

    def test_maven_metadata_rejects_stale_lynx_versions(self):
        metadata = {
            "com/tiktok/sparkling/sparkling": maven_module_metadata(
                ("org.lynxsdk.lynx", "lynx", "3.6.0"),
                ("org.lynxsdk.lynx", "primjs", "3.6.1"),
            ),
        }

        ready, reason = verify_template.validate_maven_lynx_versions(
            metadata,
            "3.9.0",
            "3.8.0-alpha.6",
        )

        self.assertFalse(ready)
        self.assertIn("org.lynxsdk.lynx:lynx:3.6.0", reason)
        self.assertIn("org.lynxsdk.lynx:primjs:3.6.1", reason)


class CocoaPodsCdnTest(unittest.TestCase):
    def test_ios_verify_invokes_cli_with_pod_repo_update_flag(self):
        self.assertEqual(
            verify_template.IOS_VERIFY_RUN_COMMAND,
            ["pnpm", "exec", "sparkling-app-cli", "run:ios", "--copy", "--pod-repo-update"],
        )
        self.assertNotIn("--", verify_template.IOS_VERIFY_RUN_COMMAND)

    def test_read_json_url_uses_cocoapods_user_agent(self):
        captured = {}

        class Response:
            def __enter__(self):
                return self

            def __exit__(self, _exc_type, _exc_value, _traceback):
                return False

            def read(self):
                return b'{"ok": true}'

        def fake_urlopen(req, timeout):
            captured["user_agent"] = req.get_header("User-agent")
            captured["timeout"] = timeout
            return Response()

        with patch.object(verify_template.urllib.request, "urlopen", fake_urlopen):
            self.assertEqual(verify_template.read_json_url("https://example.test/spec.json"), {"ok": True})

        self.assertEqual(captured["user_agent"], "CocoaPods/1.16.2")
        self.assertEqual(captured["timeout"], 30)

    def test_cocoapods_cdn_spec_url_uses_md5_sharded_path(self):
        self.assertEqual(
            verify_template.cocoapods_cdn_spec_url("SparklingMethod", "2.1.0-rc.26"),
            "https://cdn.cocoapods.org/Specs/0/e/8/SparklingMethod/2.1.0-rc.26/SparklingMethod.podspec.json",
        )

    def test_cocoapods_cdn_versions_url_uses_md5_sharded_path(self):
        self.assertEqual(
            verify_template.cocoapods_cdn_versions_url("SparklingMethod"),
            "https://cdn.cocoapods.org/all_pods_versions_0_e_8.txt",
        )

    def test_cdn_spec_ready_requires_template_sparkling_method_subspecs(self):
        spec_json = {
            "name": "SparklingMethod",
            "version": "2.1.0-rc.26",
            "subspecs": [
                {"name": "Core"},
                {"name": "Lynx"},
                {"name": "DIProvider"},
                {"name": "Debug"},
            ],
        }

        ready, reason = verify_template.cocoapods_cdn_spec_ready(
            "SparklingMethod",
            "2.1.0-rc.26",
            fetch_json=lambda _url: spec_json,
            fetch_text=lambda _url: "SparklingMethod/2.1.0-rc.25/2.1.0-rc.26\n",
        )

        self.assertTrue(ready, reason)

    def test_cdn_spec_ready_rejects_missing_version_index(self):
        spec_json = {
            "name": "SparklingMethod",
            "version": "2.1.0-rc.29",
            "subspecs": [
                {"name": "Core"},
                {"name": "Lynx"},
                {"name": "DIProvider"},
                {"name": "Debug"},
            ],
        }

        ready, reason = verify_template.cocoapods_cdn_spec_ready(
            "SparklingMethod",
            "2.1.0-rc.29",
            fetch_json=lambda _url: spec_json,
            fetch_text=lambda _url: "SparklingMethod/2.1.0-rc.27/2.1.0-rc.28\n",
        )

        self.assertFalse(ready)
        self.assertIn("versions index missing 2.1.0-rc.29", reason)

    def test_cdn_spec_ready_rejects_stale_sparkling_method_subspecs(self):
        spec_json = {
            "name": "SparklingMethod",
            "version": "2.1.0-rc.26",
            "subspecs": [
                {"name": "Core"},
                {"name": "Lynx"},
            ],
        }

        ready, reason = verify_template.cocoapods_cdn_spec_ready(
            "SparklingMethod",
            "2.1.0-rc.26",
            fetch_json=lambda _url: spec_json,
            fetch_text=lambda _url: "SparklingMethod/2.1.0-rc.26\n",
        )

        self.assertFalse(ready)
        self.assertIn("DIProvider", reason)

    def test_wait_for_cocoapods_cdn_polls_pending_pods_together(self):
        calls = []
        call_counts = {}

        class PodsConfig:
            def read_text(self):
                return '[{"pod_name":"SparklingMacro"},{"pod_name":"SparklingMethod"}]'

        def fake_spec_ready(pod, version):
            calls.append(pod)
            call_counts[pod] = call_counts.get(pod, 0) + 1
            if pod == "SparklingMacro" and call_counts[pod] == 1:
                return False, f"{pod} {version} pending"
            return True, f"{pod} {version} ready"

        with (
            patch.object(verify_template, "IOS_PODS_CONFIG", PodsConfig()),
            patch.object(verify_template, "cocoapods_cdn_spec_ready", fake_spec_ready),
            patch.object(verify_template.time, "sleep") as sleep,
        ):
            verify_template.wait_for_cocoapods_cdn("2.1.0-rc.30", attempts=2, delay=0)

        self.assertEqual(calls, ["SparklingMacro", "SparklingMethod", "SparklingMacro"])
        sleep.assert_called_once_with(0)


if __name__ == "__main__":
    unittest.main()
