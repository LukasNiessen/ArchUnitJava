import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch
import json

spec = importlib.util.spec_from_file_location(
    "publish_central", Path(__file__).resolve().parents[1] / "publish-central.py")
central = importlib.util.module_from_spec(spec)
spec.loader.exec_module(central)
DEPLOYMENT = "12dfc7c7-4015-4fbf-887e-bc3103ff6560"


class PublicationGuardTest(unittest.TestCase):
    def status(self):
        return {"deploymentId": DEPLOYMENT, "deploymentState": "VALIDATED",
                "purls": ["pkg:maven/io.github.tristankruse/archunitjava@1.0.0"]}

    def test_exact_validated_or_published_component_is_accepted(self):
        for state in ("VALIDATED", "PUBLISHING", "PUBLISHED"):
            self.assertEqual(state, central.validate_status(
                {**self.status(), "deploymentState": state}, DEPLOYMENT, "1.0.0"))

    def test_wrong_id_version_package_and_extra_components_are_rejected(self):
        for key, value in (
            ("deploymentId", "00000000-0000-0000-0000-000000000000"),
            ("purls", ["pkg:maven/io.github.tristankruse/archunitjava@0.1.0"]),
            ("purls", ["pkg:maven/other/package@1.0.0"]),
            ("purls", self.status()["purls"] * 2),
            ("purls", []),
        ):
            with self.subTest(key=key, value=value), self.assertRaises(ValueError):
                central.validate_status({**self.status(), key: value}, DEPLOYMENT, "1.0.0")

    def test_unvalidated_missing_and_failed_states_are_rejected(self):
        for state in (None, "FAILED", "PENDING", "VALIDATING", "DELETED"):
            with self.subTest(state=state), self.assertRaises(ValueError):
                central.validate_status({**self.status(), "deploymentState": state}, DEPLOYMENT, "1.0.0")

    def test_nonfinal_versions_and_noncanonical_ids_are_rejected(self):
        for version in ("1.0.0-SNAPSHOT", "1.0.0-rc.1", "1.0.0\n", "../1.0.0"):
            with self.subTest(version=version), self.assertRaises(ValueError):
                central.validate_status(self.status(), DEPLOYMENT, version)
        for deployment in ("../release", DEPLOYMENT.upper(), "not-a-uuid"):
            with self.subTest(deployment=deployment), self.assertRaises(ValueError):
                central.validate_status(self.status(), deployment, "1.0.0")

    def test_published_empty_purls_require_an_exact_committed_review_record(self):
        status = {**self.status(), "deploymentState": "PUBLISHED", "purls": []}
        review = {"schema": "archunitjava.release.v1", "deploymentId": DEPLOYMENT,
                  "coordinate": self.status()["purls"][0], "bundleSha256": "a" * 64}
        self.assertEqual("PUBLISHED", central.validate_status(status, DEPLOYMENT, "1.0.0", review))
        for bad in (None, {}, {**review, "deploymentId": "wrong"},
                    {**review, "coordinate": "pkg:maven/other/package@1.0.0"},
                    {**review, "bundleSha256": "invalid"}):
            with self.subTest(review=bad), self.assertRaises(ValueError):
                central.validate_status(status, DEPLOYMENT, "1.0.0", bad)
        with self.assertRaises(ValueError):
            central.validate_status({**status, "deploymentState": "VALIDATED"}, DEPLOYMENT, "1.0.0", review)

    def run_main(self, mode, status):
        with patch("sys.argv", ["publish-central.py", "--deployment-id", DEPLOYMENT,
                                "--version", "1.0.0", "--mode", mode]), \
             patch.dict("os.environ", {"MAVEN_CENTRAL_USERNAME": "test", "MAVEN_CENTRAL_PASSWORD": "test"}), \
             patch.object(central, "request", return_value=json.dumps(status).encode()) as request, \
             patch("builtins.print"):
            central.main()
            return [call.args[0] for call in request.call_args_list]

    def test_inspect_does_not_publish_and_publish_is_idempotent(self):
        self.assertEqual(1, len(self.run_main("inspect", self.status())))
        self.assertEqual("/deployment/" + DEPLOYMENT, self.run_main("publish", self.status())[-1])
        self.assertEqual(1, len(self.run_main("publish", {**self.status(), "deploymentState": "PUBLISHED"})))

    def test_mismatched_deployment_is_never_published(self):
        with self.assertRaises(ValueError):
            self.run_main("publish", {**self.status(), "purls": []})


if __name__ == "__main__":
    unittest.main()
